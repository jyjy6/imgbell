package ImgBell.Image;

import ImgBell.Member.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/image")
@RequiredArgsConstructor
public class ImageController {

    private final RecentViewService recentViewService;
    private final RankingService rankingService;
    private final ImageService imageService;
    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    @ResponseBody
    @GetMapping("/presigned-url")
    public PresignedUrlResponse getPermanentImgUrl(@RequestParam String filename, @RequestParam String filetype) {
        String decodedFilename = URLDecoder.decode(filename, StandardCharsets.UTF_8);
        var decodedRandomFilename = UUID.randomUUID().toString() + "_" + decodedFilename;

        // 타입에 따른 폴더에 저장
        var fullPath = filetype + "/" + decodedRandomFilename;
        var presignedUrl = imageService.createPresignedUrl(fullPath);

        // 베이스 URL 생성 (쿼리 파라미터 없는 실제 이미지 URL)
        String imageUrl = "https://" + bucket + ".s3.amazonaws.com/" + fullPath;

        // 프론트엔드로 둘 다 전송
        return new PresignedUrlResponse(presignedUrl, imageUrl);
    }

    @PostMapping("/upload")
    public ResponseEntity<?> registerFiles(@RequestBody List<ImageDto> images) {
        try {
            // DB에 파일 정보 저장
            System.out.println("이미지 업로드");
            imageService.saveFileInfoToDb(images);
            return new ResponseEntity<>("Added", HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }


    @GetMapping("/list")
    public ResponseEntity<Page<ImageDto>> getImageList(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication auth,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String imageName,
            @RequestParam(required = false) String uploaderName,
            @RequestParam(required = false) String artist,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String searchType,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) Boolean myImageList,
            @RequestParam(required = false) Boolean likeImageList
    ) {
        // 마이페이지 이미지리스트는 로그인 유저의 업로드만 필터링
        if (Boolean.TRUE.equals(myImageList) && auth != null && auth.isAuthenticated()) {
            uploaderName = ((CustomUserDetails)auth.getPrincipal()).getUsername();  // ✅ 현재 로그인된 유저 이름으로 덮어쓰기
        }
        return ResponseEntity.ok(imageService.getImageList(
                pageable, tag, imageName, uploaderName, artist, keyword, searchType, grade, myImageList, likeImageList, auth
        ));
    }



//     * 단일 이미지 상세 정보를 조회합니다.
    @GetMapping("/{id}")
    public ResponseEntity<ImageDto> getImageDetail(@PathVariable Long id, @RequestParam Boolean increaseView, Authentication auth) {
        return ResponseEntity.ok(imageService.getImageDetail(id, increaseView, auth));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteImage(@PathVariable Long id, Authentication auth) {


        return imageService.deleteImage(id,auth);
    }

    @PutMapping("/edit/{id}")
    public ResponseEntity<?> editImage(@RequestBody ImageDto dto, Authentication auth) {

        return imageService.editImage(dto, auth);
    }


    @PutMapping("/ispublic/{id}")
    public ResponseEntity<?> toggleImagePublic(@PathVariable Long id, Authentication auth){
        try {

            boolean isPublic = imageService.toggleImagePublic(id, auth);
            return ResponseEntity.ok(Map.of("success", true, "isPublic", isPublic));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }



    @GetMapping("/popular")
    public ResponseEntity<Page<ImageDto>> getPopularImages(
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(imageService.getPopularImages(pageable));
    }


    @GetMapping("/recent")
    public ResponseEntity<List<Long>> getRecentViews(Authentication auth) {
        Long userId = ((CustomUserDetails)auth.getPrincipal()).getId();
        List<Long> recentImages = recentViewService.getRecentViews(userId);
        return ResponseEntity.ok(recentImages);
    }

    @GetMapping("/ranking")
    public ResponseEntity<List<Long>> getRanking(
            @RequestParam(defaultValue = "daily") String period,
            @RequestParam(defaultValue = "10") int limit) {

        List<Long> topImages = rankingService.getTopImages(period, limit);
        return ResponseEntity.ok(topImages);
    }
    


    @Getter
    @Setter
    @AllArgsConstructor
    class PresignedUrlResponse {
        private String presignedUrl;
        private String imageUrl;

        // 생성자, getter, setter
    }
}

//페이지네이션할때 뭐 권장되는 방식이라곤 하는데 필요없음 딱히
@AllArgsConstructor
@NoArgsConstructor
class PageResponse<T> {
    private List<T> content;
    private int totalPages;
    private long totalElements;
    private int size;
    private int number;
}
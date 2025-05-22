package ImgBell.Image;

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
import java.util.UUID;

@RestController
@RequestMapping("/api/image")
@RequiredArgsConstructor
public class ImageController {

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
            uploaderName = auth.getName();  // ✅ 현재 로그인된 유저 이름으로 덮어쓰기
        }
        return ResponseEntity.ok(imageService.getImageList(
                pageable, tag, imageName, uploaderName, artist, keyword, searchType, grade, myImageList, likeImageList, auth
        ));
    }





//     * 단일 이미지 상세 정보를 조회합니다.
    @GetMapping("/{id}")
    public ResponseEntity<ImageDto> getImageDetail(@PathVariable Long id) {
        return ResponseEntity.ok(imageService.getImageDetail(id));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteImage(@PathVariable Long id, Authentication auth) {

        return imageService.deleteImage(id,auth);
    }
    

    @GetMapping("/popular")
    public ResponseEntity<Page<ImageDto>> getPopularImages(
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(imageService.getPopularImages(pageable));
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
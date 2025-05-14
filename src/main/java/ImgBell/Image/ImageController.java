package ImgBell.Image;

import lombok.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
            @PageableDefault(size = 20, sort = "id", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String grade
    ) {
        return ResponseEntity.ok(imageService.getImageList(pageable, tag, grade));
    }

    /**
     * 단일 이미지 상세 정보를 조회합니다.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ImageDto> getImageDetail(@PathVariable Long id) {
        return ResponseEntity.ok(imageService.getImageDetail(id));
    }

    /**
     * 인기 이미지 목록을 조회합니다.
     */
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


@AllArgsConstructor
@NoArgsConstructor
class PageResponse<T> {
    private List<T> content;
    private int totalPages;
    private long totalElements;
    private int size;
    private int number;
}
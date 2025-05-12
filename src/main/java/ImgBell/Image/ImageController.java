package ImgBell.Image;

import ImgBell.Member.MemberDto;
import lombok.*;
import org.springframework.beans.factory.annotation.Value;
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
    public ResponseEntity<?> registerFiles(@RequestBody List<Image> images) {
        // DB에 파일 정보 저장
        imageService.saveFileInfoToDb(images);
        return ResponseEntity.ok().build();
    }
}



@Getter
@Setter
@AllArgsConstructor
class PresignedUrlResponse {
    private String presignedUrl;
    private String imageUrl;

    // 생성자, getter, setter
}
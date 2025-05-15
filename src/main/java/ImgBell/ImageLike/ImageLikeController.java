package ImgBell.ImageLike;


import ImgBell.Image.ImageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/imagelike")
@RequiredArgsConstructor
public class ImageLikeController {

    private final ImageLikeService imageLikeService;

    @PostMapping
    public ResponseEntity<?> likeImage(@RequestBody ImageLikeDto dto) {
        imageLikeService.likeProduct(dto.getMemberId(), dto.getImageId());
        return ResponseEntity.ok("좋아요 성공");
    }

    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<ImageDto>> getLikedProducts(@PathVariable Long memberId) {
        List<ImageDto> likedProducts = imageLikeService.getLikedProducts(memberId);
        return ResponseEntity.ok(likedProducts);
    }
}



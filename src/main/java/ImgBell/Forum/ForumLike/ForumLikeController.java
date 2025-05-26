package ImgBell.Forum.ForumLike;


import ImgBell.Forum.ForumDto;
import ImgBell.Forum.ForumResponse;
import ImgBell.Image.ImageDto;
import ImgBell.ImageLike.ImageLikeDto;
import ImgBell.ImageLike.ImageLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RequiredArgsConstructor
@RequestMapping("/api/forumlike")
@RestController
public class ForumLikeController {

    private final ForumLikeService forumLikeService;

    @PostMapping
    public ResponseEntity<?> forumLike(@RequestBody ForumLikeDto dto) {
        forumLikeService.likeForum(dto.getMemberId(), dto.getForumId());
        return ResponseEntity.ok("좋아요 성공");
    }

    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<ForumResponse>> getLikedForums(@PathVariable Long memberId) {
        List<ForumResponse> likedForums = forumLikeService.getLikedForum(memberId);
        return ResponseEntity.ok(likedForums);
    }
}

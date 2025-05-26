package ImgBell.Forum.ForumComment;

import ImgBell.Forum.Forum;
import ImgBell.Forum.ForumDto;
import ImgBell.Forum.ForumRepository;
import ImgBell.Forum.ForumService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RequiredArgsConstructor
@RequestMapping("/api/forum/comment")
@RestController
public class ForumCommentController {
    private final ForumCommentService forumCommentService;
    private final ForumCommentRepository forumCommentRepository;

    @PostMapping
    public ResponseEntity<String> postComment(@RequestBody ForumCommentDto dto, Authentication auth) {

        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }
        forumCommentService.saveComment(dto, auth);
        return ResponseEntity.ok("comment Added");
    }


    @GetMapping("/{id}")
    public List<ForumCommentDto> returnCommentList(@PathVariable Long id) {
        return forumCommentService.getCommentsByForumId(id);
    }




}

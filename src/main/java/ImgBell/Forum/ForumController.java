package ImgBell.Forum;


import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/api/forum")
@RestController
public class ForumController {
    private final ForumService forumService;

    @PostMapping("/post")
    public ResponseEntity<String> postNews(@RequestBody Forum forum, Authentication auth) {

        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }
        forumService.postForum(forum, auth);
        return ResponseEntity.ok("Post created");
    }
}

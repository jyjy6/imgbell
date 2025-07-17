package ImgBell.WebSocket;

import ImgBell.Config.MyWebSocketHandler;
import ImgBell.Member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/notify")
@RequiredArgsConstructor
public class CommentNotificationController {

    private final MyWebSocketHandler socketHandler;
    private final CommentNotificationService commentNotificationService;

    @PostMapping
    public ResponseEntity<Void> sendNotification(@RequestBody Map<String, String> body) {
        String message = body.get("message");
        socketHandler.sendToAll("📢 [공지] " + message);
        return ResponseEntity.ok().build();
    }



    @PostMapping("/comment")
    public ResponseEntity<?> sendCommentNotification(@RequestBody Map<String, Object> request) {

        try {
            String postUsername = (String) request.get("postUsername");
            String commentAuthorUsername = (String) request.get("commentAuthorUsername");
            String postTitle = (String) request.get("postTitle");
            Integer postId = (Integer) request.get("postId");
            String commentContent = (String) request.get("commentContent");

            // 서비스 계층에서 알림 처리
            commentNotificationService.sendCommentNotification(
                    postUsername, commentAuthorUsername, postTitle, postId, commentContent
            );

            return ResponseEntity.ok().body(Map.of(
                    "success", true,
                    "message", "알림이 전송되었습니다",
                    "postUsername", postUsername,
                    "commentAuthorUsername", commentAuthorUsername
            ));

        } catch (Exception e) {
            System.err.println("❌ 댓글 알림 전송 실패: " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "알림 전송에 실패했습니다: " + e.getMessage()
            ));
        }
    }
}

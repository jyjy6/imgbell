package ImgBell.WebSocket;

import ImgBell.Config.MyWebSocketHandler;
import ImgBell.Member.Member;
import ImgBell.Member.MemberRepository;
import ImgBell.Member.MemberService;
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
public class NotificationController {

    private final MyWebSocketHandler socketHandler;
    private final MemberRepository memberRepository;

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

            // 알림 메시지 생성
            String notificationMessage = String.format(
                    "📝 '%s'님이 회원님의 게시글 '%s'에 댓글을 남겼습니다: %s",
                    commentAuthorUsername,
                    postTitle.length() > 20 ? postTitle.substring(0, 20) + "..." : postTitle,
                    commentContent
            );

            // JSON 형태로 메시지 구성
            String jsonMessage = String.format(
                    "{\"type\":\"comment_notification\",\"message\":\"%s\",\"postId\":%d,\"postTitle\":\"%s\",\"commenterUsername\":\"%s\"}",
                    notificationMessage.replace("\"", "\\\""),
                    postId,
                    postTitle.replace("\"", "\\\""),
                    commentAuthorUsername
            );

            // 게시글 작성자에게 알림 전송
            socketHandler.sendToUsername(postUsername, jsonMessage);

            System.out.println("🔔 댓글 알림 전송됨: " + postUsername + " <- " + commentAuthorUsername);

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

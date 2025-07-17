package ImgBell.WebSocket;

import ImgBell.Config.MyWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;

@Service
@RequiredArgsConstructor
public class CommentNotificationService {

    private final MyWebSocketHandler socketHandler;

    public void sendCommentNotification(String postUsername, String commentAuthorUsername,
                                        String postTitle, Integer postId, String commentContent) {
        try {
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

        } catch (Exception e) {
            System.err.println("❌ 댓글 알림 전송 실패: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("알림 전송에 실패했습니다: " + e.getMessage(), e);
        }
    }
}

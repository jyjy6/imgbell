package ImgBell.WebSocket;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Data
public class CommentNotificationDto {
    private Long postId;
    private String postTitle;
    private String postUsername;
    private String commentAuthorUsername;
    private String commentContent;
}
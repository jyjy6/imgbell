package ImgBell.Forum.ForumComment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ForumCommentDto {
    private Long id;
    private String content;
    private String authorDisplayName;
    private String authorUsername;
    private Integer likeCount;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Long forumId; // 게시글 ID만
    private Long parentId; // 부모 댓글 ID (null이면 최상위 댓글)
    private List<ForumCommentDto> comments; // 자식 댓글들


}

package ImgBell.Forum;

import ImgBell.Forum.ForumComment.ForumComment;
import ImgBell.Forum.ForumLike.ForumLike;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@AllArgsConstructor
@Builder
public class ForumDto {
    private Long id;
    private String title;
    private String content;
    private String authorDisplayName;
    private String authorUsername;
    private Forum.PostType type;
    private Integer viewCount;
    private Integer likeCount;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ForumComment> comments;
    private Set<ForumLike> forumLikes;
}

@Setter
@Getter
class ForumFormDto{
    private String title;
    private String content;
    private Forum.PostType type = Forum.PostType.NORMAL;
}


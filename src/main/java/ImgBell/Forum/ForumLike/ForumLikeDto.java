package ImgBell.Forum.ForumLike;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class ForumLikeDto {
    private Long memberId;
    private Long forumId;
}

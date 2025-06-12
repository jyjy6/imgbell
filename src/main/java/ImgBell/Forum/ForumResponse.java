package ImgBell.Forum;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ForumResponse {
    private Long id;
    private String title;
    private String content;
    private String authorDisplayName;
    private String authorUsername;
    private String createdAt;
    private Integer viewCount;
    private Integer likeCount;
    private Forum.PostType type;
    private Integer commentsCount;

    // 전체 정보를 포함한 응답용 (상세 조회시 사용)
    public static ForumResponse from(Forum forum) {
        return ForumResponse.builder()
                .id(forum.getId())
                .title(forum.getTitle())
                .content(forum.getContent())  // 전체 내용 포함
                .authorDisplayName(forum.getAuthorDisplayName())
                .authorUsername(forum.getAuthorUsername())
                .createdAt(forum.getCreatedAt().toString())
                .viewCount(forum.getViewCount())
                .likeCount(forum.getLikeCount())
                .type(forum.getType())
                .commentsCount(forum.getComments().size())
                .build();
    }

    // 목록용 응답 (content 제외한 간략 정보)
    public static ForumResponse forList(Forum forum) {
        return ForumResponse.builder()
                .id(forum.getId())
                .title(forum.getTitle())
                .content(null)  // 목록에서는 내용 제외
                .authorDisplayName(forum.getAuthorDisplayName())
                .createdAt(forum.getCreatedAt().toString())
                .viewCount(forum.getViewCount())
                .likeCount(forum.getLikeCount())
                .type(forum.getType())
                .commentsCount(forum.getComments().size())
                .build();
    }
}
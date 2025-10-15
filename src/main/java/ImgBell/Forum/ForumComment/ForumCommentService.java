package ImgBell.Forum.ForumComment;

import ImgBell.Forum.Forum;
import ImgBell.Forum.ForumDto;
import ImgBell.Forum.ForumRepository;
import ImgBell.Member.CustomUserDetails;
import ImgBell.GlobalErrorHandler.GlobalException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ForumCommentService {
    private final ForumCommentRepository forumCommentRepository;
    private final ForumRepository forumRepository;

    public void saveComment(ForumCommentDto dto, Authentication auth){

        String authorUsername = ((CustomUserDetails)auth.getPrincipal()).getUsername();
        String authorDisplayName = ((CustomUserDetails)auth.getPrincipal()).getDisplayName();

        ForumComment comment = new ForumComment();
        comment.setContent(dto.getContent());
        comment.setAuthorDisplayName(authorDisplayName);
        comment.setAuthorUsername(authorUsername);
        Forum forum = forumRepository.findById(dto.getForumId())
                .orElseThrow(() -> new GlobalException("게시글을 찾을 수 없습니다", "FORUM_NOT_FOUND", HttpStatus.NOT_FOUND));
        comment.setForum(forum);
        
        if(dto.getParentId() != null){
            ForumComment parentsComment = forumCommentRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new GlobalException("부모 댓글을 찾을 수 없습니다", "PARENT_COMMENT_NOT_FOUND", HttpStatus.NOT_FOUND));
            comment.setParent(parentsComment);
        }

        forumCommentRepository.save(comment);
    }

    public List<ForumCommentDto> getCommentsByForumId(Long id) {
        List<ForumComment> comments = forumCommentRepository.findByForumId(id);

        return comments.stream()
                .map(this::toDto)
                .toList();
    }

    private ForumCommentDto toDto(ForumComment comment) {
        return ForumCommentDto.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .authorDisplayName(comment.getAuthorDisplayName())
                .authorUsername(comment.getAuthorUsername())
                .likeCount(comment.getLikeCount())
                .isDeleted(comment.getIsDeleted())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .forumId(comment.getForum().getId())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .comments(comment.getComments() != null ?
                        comment.getComments().stream()
                                .map(this::toDto) // 재귀 변환
                                .toList()
                        : List.of())
                .build();
    }
}

package ImgBell.Forum;


import ImgBell.Member.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ForumService {
    private final ForumRepository forumRepository;

    public void postForum(ForumFormDto forumDto, Authentication auth){

        String username = ((CustomUserDetails)auth.getPrincipal()).getUsername();
        String displayName = ((CustomUserDetails)auth.getPrincipal()).getDisplayName();

        Forum forum = new Forum();
        forum.setTitle(forumDto.getTitle());
        forum.setContent(forumDto.getContent());
        Forum.PostType postType = forumDto.getType() != null ?
                forumDto.getType() : Forum.PostType.NORMAL;
        forum.setType(postType);
        forum.setAuthorDisplayName(displayName);
        forum.setAuthorUsername(username);
        forumRepository.save(forum);
    }


    public Page<ForumResponse> getForumList(Forum.PostType forumType, Pageable pageable) {
        return forumRepository.findByTypeAndIsDeletedFalse(forumType, pageable)
                .map(ForumResponse::forList);
    }

    // 또는 상세 조회시에는
    public ForumResponse getForumDetail(Long id) {
        Forum forum = forumRepository.findById(id).orElseThrow();
        return ForumResponse.from(forum);  // from 메서드 사용 (전체 정보 포함)
    }





}

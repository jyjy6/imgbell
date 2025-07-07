package ImgBell.Forum;

import ImgBell.Member.CustomUserDetails;
import ImgBell.GlobalErrorHandler.GlobalException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.http.HttpStatus;
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

    public void editForum(Long id, ForumFormDto forumDto, Authentication auth){
        try {
            String username = ((CustomUserDetails) auth.getPrincipal()).getUsername();
            String displayName = ((CustomUserDetails) auth.getPrincipal()).getDisplayName();

            Forum forum = forumRepository.findById(id)
                    .orElseThrow(() -> new GlobalException("게시글을 찾을 수 없습니다", "FORUM_NOT_FOUND", HttpStatus.NOT_FOUND));
            
            if (!username.equals(forum.getAuthorUsername())) {
                throw new GlobalException("글쓴이만 수정할 수 있습니다", "UNAUTHORIZED_EDIT", HttpStatus.FORBIDDEN);
            }
            
            forum.setTitle(forumDto.getTitle());
            forum.setContent(forumDto.getContent());
            Forum.PostType postType = forumDto.getType() != null ?
                    forumDto.getType() : Forum.PostType.NORMAL;
            forum.setType(postType);
            forum.setAuthorDisplayName(displayName);
            forum.setAuthorUsername(username);
            forumRepository.save(forum);
        } catch (Exception e){
            System.out.println("에러남");
            System.out.println(e.getMessage());
        }
    }

    public Page<ForumResponse> getForumList(Forum.PostType forumType, Pageable pageable) {
        return forumRepository.findByTypeAndIsDeletedFalse(forumType, pageable)
                .map(ForumResponse::forList);
    }

    // 또는 상세 조회시에는
    public ForumResponse getForumDetail(Long id) {
        Forum forum = forumRepository.findById(id).orElseThrow(()->new GlobalException("그런 게시물 없습니다", "FORUM_NOT_FOUND"));
        return ForumResponse.from(forum);  // from 메서드 사용 (전체 정보 포함)
    }

    //포럼 수정용 Dto반환
    public ForumFormDto getForumDetail(Long id, Authentication auth) {

        String username = ((CustomUserDetails)auth.getPrincipal()).getUsername();
        Forum forum = forumRepository.findById(id).orElseThrow(()->new GlobalException("그런 게시물 없습니다", "FORUM_NOT_FOUND"));

        if(!username.equals(forum.getAuthorUsername())){
            throw new GlobalException("글쓴이만 수정할 수 있습니다", "UNAUTHORIZED_EDIT", HttpStatus.FORBIDDEN);
        }
        ForumFormDto editForm = new ForumFormDto();
        editForm.setTitle(forum.getTitle());
        editForm.setContent(forum.getContent());
        editForm.setType(forum.getType());

        return editForm;
    }

    // 검색 기능 수정 - 키워드에 % 기호 추가
    public Page<ForumResponse> searchForums(String keyword, Pageable pageable) {
        // 키워드 앞뒤에 % 기호 추가
        String searchKeyword = "%" + keyword + "%";
        return forumRepository.findByKeywordAndIsDeletedFalse(searchKeyword, pageable)
                .map(ForumResponse::forList);
    }
}

package ImgBell.Forum;


import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ForumService {
    private final ForumRepository forumRepository;

    public void postForum(Forum forum, Authentication auth){

        // 2. 현재 로그인한 사용자 정보 가져오기
        String username = auth.getName(); // 기본적으로 username(email 등)이 들어감

        // 4. News 객체에 설정
        forum.setAuthorUsername(username);

        forumRepository.save(forum);
    }
}

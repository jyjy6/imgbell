package ImgBell.Auth.OAuth;

import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@RestController
@RequestMapping("/api/oauth")
public class OAuthController {

    // 구글 로그인 URL로 리다이렉트
    @GetMapping("/google")
    public String googleLogin() {
        return "<a href=\"/oauth2/authorization/google\">구글로 로그인</a>";
    }

    // 구글 콜백 처리
    @GetMapping("/google/callback")
    public Map<String, Object> googleCallback(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient,
            OAuth2User oauth2User
    ) {
        // oauth2User.getAttributes()에 구글 프로필 정보가 담겨있음
        return oauth2User.getAttributes();
    }
} 
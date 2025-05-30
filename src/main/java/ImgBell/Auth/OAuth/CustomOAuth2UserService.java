package ImgBell.Auth.OAuth;

import ImgBell.Member.CustomUserDetails;
import ImgBell.Member.Member;
import ImgBell.Member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.HashSet;

@RequiredArgsConstructor
@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final MemberRepository memberRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = new DefaultOAuth2UserService().loadUser(userRequest);

        // 구글에서 받아온 정보 추출
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");

        System.out.println("구글에서받아온정보 : ");
        System.out.println(email);
        System.out.println(name);

        // DB에 있는지 확인, 없으면 새로 생성
        Member member = memberRepository.findByUsername(email)
                .orElseGet(() -> {
                    Member newMember = Member.builder()
                            .email(email)
                            .username(email)
                            .name(name != null ? name : email) // name 필드에 값 설정
                            .displayName(name != null ? name : email) // displayName도 같이 설정
                            .password("oauth2user") // 실제로는 사용 안 함
                            .roles(new HashSet<>(Set.of("ROLE_USER", "ROLE_OAUTH"))) // roles 초기화
                            .termsAccepted(true) // OAuth2 사용자는 기본적으로 약관 동의로 처리
                            .privacyAccepted(true) // OAuth2 사용자는 기본적으로 개인정보 동의로 처리
                            .marketingAccepted(false) // 마케팅은 기본 false
                            .build();
                    return memberRepository.save(newMember);
                });

        member.setLastLogin(LocalDateTime.now());
        member.setLoginAttempts(0);
        Set<SimpleGrantedAuthority> authorities = member.getRoleSet().stream()
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toSet());

        return new CustomUserDetails(member, authorities, attributes);
    }

}

package ImgBell.Security;

import ImgBell.Member.CustomUserDetails;
import ImgBell.Member.Member;
import ImgBell.Member.MemberRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {


    private final MemberRepository memberRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        // CustomUserDetails에서 사용자 정보 가져오기
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Member member = userDetails.getMember();

        // 로그인 성공 시 처리 (마지막 로그인 시간 업데이트 등)
        member.setLastLogin(LocalDateTime.now());
        member.setLoginAttempts(0); // 로그인 시도 횟수 초기화
        memberRepository.save(member);

        // 사용자 역할에 따라 리다이렉트
        String redirectUrl = "/dashboard"; // 기본 리다이렉트

        // 관리자인 경우 관리자 페이지로
        if (userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") ||
                        a.getAuthority().equals("ROLE_SUPERADMIN"))) {
            redirectUrl = "/admin/dashboard";
        }

        response.sendRedirect(redirectUrl);
    }
}


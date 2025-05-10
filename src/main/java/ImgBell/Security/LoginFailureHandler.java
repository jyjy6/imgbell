package ImgBell.Security;

import ImgBell.Member.Member;
import ImgBell.Member.MemberRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

    @Autowired
    private MemberRepository memberRepository;

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOCK_TIME_MINUTES = 30;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        String username = request.getParameter("username");
        String errorMessage = "아이디 또는 비밀번호가 올바르지 않습니다.";

        // 이미 잠긴 계정인 경우
        if (exception instanceof LockedException) {
            errorMessage = "계정이 잠겼습니다. 나중에 다시 시도해주세요.";
        } else {
            // 로그인 시도 횟수 증가
            Optional<Member> memberOpt = memberRepository.findByUsername(username);
            if (memberOpt.isPresent()) {
                Member member = memberOpt.get();

                // 로그인 시도 횟수 증가
                int attempts = member.getLoginAttempts() != null ? member.getLoginAttempts() + 1 : 1;
                member.setLoginAttempts(attempts);

                // 로그인 시도가 MAX_LOGIN_ATTEMPTS 이상이면 계정 잠금
                if (attempts >= MAX_LOGIN_ATTEMPTS) {
                    member.setLoginSuspendedTime(LocalDateTime.now().plusMinutes(LOCK_TIME_MINUTES));
                    errorMessage = "로그인 시도 횟수가 너무 많습니다. " + LOCK_TIME_MINUTES + "분 동안 계정이 잠깁니다.";
                }

                memberRepository.save(member);
            }
        }

        // 로그인 페이지로 리다이렉트하면서 에러 메시지 전달
        response.sendRedirect("/login?error=true&message=" + errorMessage);
    }
}
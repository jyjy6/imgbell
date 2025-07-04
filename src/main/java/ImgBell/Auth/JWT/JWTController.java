package ImgBell.Auth.JWT;


import ImgBell.Member.CustomUserDetailsService;
import ImgBell.Member.Member;
import ImgBell.Member.MemberRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@RestController

@RequiredArgsConstructor // Lombok을 사용하여 생성자 주입
public class JWTController {

    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;
    private final AuthenticationManager authenticationManager;
    private final JWTUtil jwtUtil;
    @Value("${app.production}")
    private String appEnv;

    boolean isProduction = "production".equalsIgnoreCase(appEnv);
    @Value("${app.cookie.domain}")
    private String cookieDomain;



    @PostMapping("/api/login/jwt")
    public ResponseEntity<Map<String, String>> loginJWT(@RequestBody Map<String, String> data, HttpServletResponse response) {
        try {


            var authToken = new UsernamePasswordAuthenticationToken(
                    data.get("username"), data.get("password")
            );

            // AuthenticationManager를 사용하여 인증 수행
            Authentication auth = authenticationManager.authenticate(authToken);
            SecurityContextHolder.getContext().setAuthentication(auth);

            // 인증된 사용자 정보 가져오기
            var auth2 = SecurityContextHolder.getContext().getAuthentication();

            // JWT 생성
            String accessToken = jwtUtil.createAccessToken(auth2);
            String refreshToken = jwtUtil.createRefreshToken(auth2.getName());

            Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
            refreshCookie.setMaxAge(60 * 60 * 24 * 30); // 30일
            refreshCookie.setHttpOnly(true);
            refreshCookie.setSecure(isProduction); // HTTPS인 경우 true
            refreshCookie.setPath("/");
            refreshCookie.setDomain(isProduction ? cookieDomain : null); // 도메인 설정
            response.addCookie(refreshCookie);


            // 응답 바디 구성
            Map<String, String> responseBody = new HashMap<>();
            responseBody.put("accessToken", accessToken);

            return ResponseEntity.ok(responseBody);
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인 실패: " + e.getMessage()));
        }
    }

    @PostMapping("/api/login/guest")
    public ResponseEntity<Map<String, String>> guestLoginJWT(HttpServletResponse response) {
        try {
            String guestMemberCode = "GUEST" + UUID.randomUUID().toString().substring(0, 8);
            String guestPassword = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            Member guestMember = new Member();
            guestMember.addRole("ROLE_GUEST");
            guestMember.addRole("ROLE_USER");
            guestMember.setPassword(passwordEncoder.encode(guestPassword));
            guestMember.setEmail("guest@guest.guest");
            guestMember.setName(guestMemberCode);
            guestMember.setUsername(guestMemberCode);
            guestMember.setDisplayName(guestMemberCode);
            memberRepository.save(guestMember);

            var authToken = new UsernamePasswordAuthenticationToken(
                    guestMemberCode, guestPassword
            );

            // AuthenticationManager를 사용하여 인증 수행
            Authentication auth = authenticationManager.authenticate(authToken);
            SecurityContextHolder.getContext().setAuthentication(auth);

            // 인증된 사용자 정보 가져오기
            var auth2 = SecurityContextHolder.getContext().getAuthentication();

            // JWT 생성
            String accessToken = jwtUtil.createAccessToken(auth2);
            String refreshToken = jwtUtil.createRefreshToken(auth2.getName());

            Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
            refreshCookie.setMaxAge(60 * 60 * 24 * 7); // 30일
            refreshCookie.setHttpOnly(true);
            refreshCookie.setSecure(isProduction); // HTTPS인 경우 true
            refreshCookie.setPath("/");
            refreshCookie.setDomain(isProduction ? cookieDomain : null); // 도메인 설정
            response.addCookie(refreshCookie);

            // 응답 바디 구성
            Map<String, String> responseBody = new HashMap<>();
            responseBody.put("accessToken", accessToken);

            return ResponseEntity.ok(responseBody);
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인 실패: " + e.getMessage()));
        }
    }

    @GetMapping("/api/refresh-token")
    public ResponseEntity<Map<String, String>> refresh(@CookieValue(value = "refreshToken", required = false) String refreshToken) {
        System.out.println("새 액세스 토큰 요청됨");

        try {
            // 리프레시 토큰이 없는 경우
            if (refreshToken == null || refreshToken.isEmpty()) {
                System.out.println("리프레시 토큰이 없음");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "리프레시 토큰이 존재하지 않습니다."));
            }
            // 리프레시 토큰 만료 확인
            if (jwtUtil.isTokenExpired(refreshToken)) {
                System.out.println("토큰 만료됨");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "리프레시 토큰이 만료되었습니다."));
            }

            // 리프레시 토큰에서 사용자 정보(username 또는 userId) 추출
            String username = jwtUtil.extractUsername(refreshToken);
            System.out.println("필터:유저네임"+username);
            // 추출한 사용자 정보로 새 액세스 토큰 생성
            String newAccessToken = jwtUtil.refreshAccessToken(username);
            System.out.println("새 액세스 토큰 발급됨: " + newAccessToken);

            return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
        } catch (Exception e) {
            System.out.println("토큰 갱신 중 오류 발생: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "서버 오류로 인해 액세스 토큰을 갱신할 수 없습니다."));
        }
    }



    @PostMapping("/api/logout")
    public ResponseEntity<String> logout(HttpServletResponse response) {
        System.out.println("로그아웃요청됨");

        Cookie refreshCookie = new Cookie("refreshToken", null);
        refreshCookie.setMaxAge(0);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/");
        response.addCookie(refreshCookie);
        System.out.println("로그아웃요청됨2");

        return ResponseEntity.ok("로그아웃 성공");
    }
}

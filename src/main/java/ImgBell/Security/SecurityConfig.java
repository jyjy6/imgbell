package ImgBell.Security;


import ImgBell.Auth.JWT.JWTFilter;
import ImgBell.Auth.JWT.JWTUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// GEMINI: 리소스 서버(ImgBell)의 역할에 맞게 SecurityConfig를 수정합니다.
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JWTUtil jwtUtil;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // GEMINI: CSRF 보호 비활성화 (JWT 사용 시 일반적으로 불필요)
                .csrf(csrf -> csrf.disable())
                // GEMINI: 세션을 사용하지 않으므로 STATELESS로 설정
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // GEMINI: OPTIONS 메서드는 인증 없이 허용
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        // GEMINI: 모니터링 엔드포인트는 인증 없이 허용
                        .requestMatchers("/actuator/**").permitAll()
                        // GEMINI: /api/userinfo 엔드포인트는 인증된 사용자만 접근 가능하도록 설정
                        .requestMatchers("/api/userinfo").authenticated()
                        // GEMINI: 그 외 모든 요청은 일단 허용 (필요에 따라 변경)
                        .anyRequest().permitAll()
                )
                // GEMINI: 직접 구현한 JWTFilter를 UsernamePasswordAuthenticationFilter 앞에 추가
                .addFilterBefore(new JWTFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);

        // GEMINI: OAuth2 관련 설정은 인증 서버(AuthBell)의 역할이므로 제거합니다.

        return http.build();
    }

    // GEMINI: ImgBell은 자체적으로 비밀번호를 다루지 않으므로 PasswordEncoder 빈은 제거해도 무방하나,
    // GEMINI: 다른 곳에서 의존할 가능성을 고려하여 일단 남겨둡니다.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // GEMINI: 자체 로그인을 처리하지 않으므로 AuthenticationManager, DaoAuthenticationProvider, OAuth 관련 빈은 모두 제거합니다.
    // GEMINI: 이러한 설정은 인증 서버(AuthBell)에만 필요합니다.
}

package ImgBell.Security;


import ImgBell.Member.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {


    private final CustomUserDetailsService userDetailsService;

    private final AuthenticationSuccessHandler loginSuccessHandler; // 로그인 성공 핸들러 (구현 필요)

    private final AuthenticationFailureHandler loginFailureHandler; // 로그인 실패 핸들러 (구현 필요)

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable());
        http
                .authorizeHttpRequests(auth -> auth
                        // 관리자 페이지
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // 최고 관리자 페이지
                        .requestMatchers("/superadmin/**").hasRole("SUPERADMIN")
                        // 프리미엄 회원 페이지
                        .requestMatchers("/premium/**").hasRole("PREMIUM")
                        // 그 외 인증 필요
                        .anyRequest().permitAll()
                )
                .rememberMe(remember -> remember
                        .key("uniqueAndSecretKey")
                        .tokenValiditySeconds(86400) // 1일
                        .userDetailsService(userDetailsService)
                );

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
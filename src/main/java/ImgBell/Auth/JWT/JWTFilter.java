package ImgBell.Auth.JWT;



import ImgBell.Member.Dto.MemberDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private static final String REFRESH_TOKEN_ENDPOINT = "/api/refresh-token";
    private final JWTUtil jwtUtil;


    private final String allowedOrigins; // Spring Security->SecurityConfig 생성자를 통해 주입

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        System.out.println("JWT 필터 시작 - 요청 URI: " + request.getRequestURI());
        if (pathMatcher.match("/api/oauth/google", request.getRequestURI())) {
            System.out.println("OAuth 요청이므로 JWT 필터를 건너뜁니다.");
            filterChain.doFilter(request, response);
            return;
        }
        if (pathMatcher.match("/api/login/jwt", request.getRequestURI())) {
            System.out.println("로그인 요청이므로 JWT 필터를 건너뜁니다.");
            filterChain.doFilter(request, response);
            return;
        }
        if (pathMatcher.match("/api/auth/csrf", request.getRequestURI())) {
            System.out.println("CSRF 요청이므로 JWT 필터를 건너뜁니다.");
            filterChain.doFilter(request, response);
            return;
        }
        // refresh-token 요청인 경우 필터 건너뛰기
        if (pathMatcher.match(REFRESH_TOKEN_ENDPOINT, request.getRequestURI())) {
            System.out.println("refresh-token 요청이므로 JWT 필터를 건너뜁니다.");
            filterChain.doFilter(request, response);
            return;
        }
        if (pathMatcher.match("/api/oauth/user/me", request.getRequestURI())) {
            System.out.println("oauth/user 요청이므로 JWT 필터를 건너뜁니다.");
            filterChain.doFilter(request, response);
            return;
        }


        String authHeader = request.getHeader("Authorization");
        System.out.println("Auth Header: " + authHeader);

        // 요청에서 JWT 추출

        String jwt = getJwtFromRequest(request);

        System.out.println("현재jwt"+jwt);

        if (jwt != null) {
            try {
                // JWT 유효성 검증
                System.out.println("만료됐는지 확인1");
                if (!jwtUtil.isTokenExpired(jwt)) {
                    // JWT에서 Claims 추출
                    Claims claims = jwtUtil.extractToken(jwt);
                    String userInfoJson = claims.get("userInfo", String.class);

                    ObjectMapper objectMapper = new ObjectMapper();
                    objectMapper.registerModule(new JavaTimeModule());
                    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                    MemberDto memberDto = objectMapper.readValue(userInfoJson, MemberDto.class);
                    System.out.println("멤버디티오: " + memberDto);

                    // JWT에서 권한 정보 추출 (authorities가 JWT에 포함되어 있다고 가정)
                    Collection<GrantedAuthority> authorities = extractAuthoritiesFromMemberDto(memberDto);

                    // DB 조회 없이 바로 Authentication 객체 생성
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            memberDto.getUsername(), null, authorities);

                    SecurityContextHolder.getContext().setAuthentication(authentication);

                } else {
                    response.setHeader("Access-Control-Allow-Origin", allowedOrigins);
                    response.setHeader("Access-Control-Allow-Credentials", "true");
                    // 401 응답 설정
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token expired");
                    return; // 필터 체인 종료
                }
            } catch (Exception e) {
                System.out.println("JWT 검증 실패: " + e.getMessage());
                e.printStackTrace();
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT token");
                return; // 필터 체인 종료
            }
        }

        filterChain.doFilter(request, response);
    }


    // 요청에서 JWT를 추출하는 메서드
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // "Bearer " 제거
        }
        return null;
    }

    private Collection<GrantedAuthority> extractAuthoritiesFromMemberDto(MemberDto memberDto) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        // memberDto에서 권한 정보를 추출하여 authorities에 추가
        // 예: memberDto.getRoles()가 권한 목록을 반환한다고 가정
        if (memberDto.getRoleSet() != null) {
            for (String role : memberDto.getRoleSet()) {
                authorities.add(new SimpleGrantedAuthority(role));
            }
        }
        return authorities;
    }
}
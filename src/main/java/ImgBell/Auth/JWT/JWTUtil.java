package ImgBell.Auth.JWT;

import ImgBell.Member.CustomUserDetails;
import ImgBell.Member.CustomUserDetailsService;
import ImgBell.Member.Member;
import ImgBell.Member.Dto.MemberDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JWTUtil {
    @Value("${jwt.key}")
    private String jwtKey; // application.properties에서 값 주입

    private static SecretKey key; // static 유지, final 제거

    @PostConstruct
    private void init() {
        key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtKey));
    }

    // JWT 만들어주는 함수
    public static String createAccessToken(Authentication auth) {

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        Member member = userDetails.getMember();

        Set<String> roleSet = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

// 원하는 정보만 포함한 DTO 생성
        MemberDto memberDto = MemberDto.builder()
                .id(member.getId())
                .username(member.getUsername())
                .displayName(member.getDisplayName())
                .email(member.getEmail())
                .phone(member.getPhone())
                .createdAt(member.getCreatedAt())
                .roleSet(roleSet)
                .profileImage(member.getProfileImage())
                .mainAddress(member.getMainAddress())
                .subAddress(member.getSubAddress())
                .country(member.getCountry())
                .build();


        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        String memberJson = "";
        try {
            memberJson = objectMapper.writeValueAsString(memberDto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error while converting MemberDto to JSON", e);
        }

        return Jwts.builder()
                .setSubject(member.getUsername())
                .claim("userInfo", memberJson) // JSON 형태로 저장
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(key)
                .compact();
    }

    //    jwt재발급 해주는함수 메소드 오버라이딩 -> AccessToken이 만료되면 Authentication auth는 무효가 되기때문에 username
    public static String createAccessToken(String username, Set<String> authorities, Long memberId /* 필요한 다른 정보들 */) {
        // username으로 사용자 정보를 로드

        // 이제 기존의 createAccessToken(Authentication auth) 로직을 재사용할 수 있음
        CustomUserDetails customUserDetails = (CustomUserDetails) auth.getPrincipal();
        Member member = customUserDetails.getMember();

        Set<String> roleSet = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        MemberDto memberDto = MemberDto.builder()
                .id(member.getId())
                .username(member.getUsername())
                .displayName(member.getDisplayName())
                .email(member.getEmail())
                .phone(member.getPhone())
                .createdAt(member.getCreatedAt())
                .roleSet(roleSet)
                .profileImage(member.getProfileImage())
                .mainAddress(member.getMainAddress())
                .subAddress(member.getSubAddress())
                .country(member.getCountry())
                .build();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        String userInfoJson = "";
        try {
            userInfoJson = objectMapper.writeValueAsString(memberDto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error while converting MemberDto to JSON", e);
        }

        return Jwts.builder()
                .setSubject(member.getUsername())
                .claim("userInfo", userInfoJson)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(key)
                .compact();
    }


    public static String createRefreshToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 604800000)) // 7일 유효
                .signWith(key)
                .compact();
    }

    // JWT 까주는 함수
    public static Claims extractToken(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
        return claims;
    }


    //JWT 토큰에서 클레임(Claims)을 추출하는 기능을 수행
    public static Claims extractClaims(String token) {
        return Jwts.parser()
                .setSigningKey(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    //이 메서드는 JWT 토큰이 만료되었는지 여부를 확인하는 기능을 수행
    public static boolean isTokenExpired(String token) {
        try {
            final Date expiration = extractClaims(token).getExpiration();
            return expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            System.out.println("토큰이 만료되었습니다: " + e.getMessage());
            return true;
        } catch (Exception e) {
            System.out.println("토큰 검증 중 오류 발생: " + e.getMessage());
            return true; // 오류 났으면 만료된 걸로 간주
        }
    }


    //이 메서드는 JWT 토큰에서 사용자 이름을 추출하는 기능을 수행
    public static String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }


}
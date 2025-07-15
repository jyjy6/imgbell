package ImgBell.Redis;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    
    /**
     * 시간 창 크기 (초 단위)
     */
    long windowSeconds() default 60;
    
    /**
     * 최대 요청 수
     */
    int maxRequests() default 10;
    
    /**
     * Rate Limiting 방식
     */
    RateLimitType type() default RateLimitType.SLIDING_WINDOW;
    
    /**
     * 식별자 타입
     */
    IdentifierType identifierType() default IdentifierType.IP;
    
    /**
     * 사용자 정의 키 접두사
     */
    String keyPrefix() default "";
    
    /**
     * 에러 메시지
     */
    String message() default "너무 많은 요청입니다. 잠시 후 다시 시도해주세요.";
    
    /**
     * Token Bucket 설정 (type이 TOKEN_BUCKET일 때만 사용)
     */
    int capacity() default 10;
    
    /**
     * Token Bucket 리필 속도 (초당 토큰 수)
     */
    double refillRate() default 1.0;
    
    enum RateLimitType {
        FIXED_WINDOW,
        SLIDING_WINDOW,
        TOKEN_BUCKET
    }
    
    enum IdentifierType {
        IP,              // IP 주소
        USER_ID,         // 회원 ID
        IP_AND_USER_ID   // IP + 회원 ID 조합
    }
} 
package ImgBell.Redis;

import ImgBell.GlobalErrorHandler.GlobalException;
import ImgBell.Member.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {
    
    private final RedisService redisService;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        /**
         * HandlerMethod는 "실행될 컨트롤러 메서드에 대한 모든 정보가 담긴 객체"
         * API 요청처럼 컨트롤러의 특정 메서드를 실행해야 할 때: handler는 HandlerMethod 타입의 객체입니다.
         * */

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        
        if (rateLimit == null) {
            return true;
        }
        
        String identifier = getIdentifier(request, rateLimit.identifierType());
        String key = buildKey(rateLimit.keyPrefix(), identifier, handlerMethod);
        
        boolean allowed = checkRateLimit(key, rateLimit);
        
        if (!allowed) {
            log.warn("Rate limit exceeded for key: {}, identifier: {}", key, identifier);
            
            // 현재 요청 수 정보를 헤더에 추가
            long currentCount = redisService.getCurrentRequestCount(key, rateLimit.windowSeconds());
            response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimit.maxRequests()));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, rateLimit.maxRequests() - currentCount)));
            response.setHeader("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() + rateLimit.windowSeconds() * 1000));
            
            throw new GlobalException(rateLimit.message(), "RATE_LIMIT_EXCEEDED");
        }
        
        // 성공적인 요청에도 Rate Limit 헤더 추가
        long currentCount = redisService.getCurrentRequestCount(key, rateLimit.windowSeconds());
        response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimit.maxRequests()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, rateLimit.maxRequests() - currentCount)));
        response.setHeader("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() + rateLimit.windowSeconds() * 1000));
        
        return true;
    }
    
    private String getIdentifier(HttpServletRequest request, RateLimit.IdentifierType identifierType) {
        String ip = getClientIp(request);
        String userId = getCurrentUserId();
        
        switch (identifierType) {
            case IP:
                return "ip:" + ip;
            case USER_ID:
                return userId != null ? "user:" + userId : "ip:" + ip;
            case IP_AND_USER_ID:
                return userId != null ? "user:" + userId + ":ip:" + ip : "ip:" + ip;
            default:
                return "ip:" + ip;
        }
    }
    
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && 
            authentication.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            return userDetails.getUsername();
        }
        return null;
    }
    
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    private String buildKey(String keyPrefix, String identifier, HandlerMethod handlerMethod) {
        String methodName = handlerMethod.getMethod().getName();
        String className = handlerMethod.getBeanType().getSimpleName();
        
        if (keyPrefix.isEmpty()) {
            return String.format("%s:%s:%s", className, methodName, identifier);
        } else {
            return String.format("%s:%s:%s:%s", keyPrefix, className, methodName, identifier);
        }
    }
    
    private boolean checkRateLimit(String key, RateLimit rateLimit) {
        switch (rateLimit.type()) {
            case FIXED_WINDOW:
                return redisService.isAllowedFixedWindow(key, rateLimit.windowSeconds(), rateLimit.maxRequests());
            case SLIDING_WINDOW:
                return redisService.isAllowedSlidingWindow(key, rateLimit.windowSeconds(), rateLimit.maxRequests());
            case TOKEN_BUCKET:
                return redisService.isAllowedTokenBucket(key, rateLimit.capacity(), rateLimit.refillRate());
            default:
                return redisService.isAllowedSlidingWindow(key, rateLimit.windowSeconds(), rateLimit.maxRequests());
        }
    }
} 
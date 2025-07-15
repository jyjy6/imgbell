package ImgBell.Redis;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/rate-limit")
@RequiredArgsConstructor
@Tag(name = "Rate Limiting 관리", description = "Rate Limiting 관리 및 모니터링 API")
@Slf4j
public class RateLimitController {
    
    private final RedisService redisService;
    
    @Operation(
        summary = "Rate Limit 상태 조회",
        description = "특정 키의 Rate Limit 상태를 조회합니다."
    )
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getRateLimitStatus(
        @Parameter(description = "조회할 키", required = true)
        @RequestParam String key,
        @Parameter(description = "시간 창 크기 (초)", required = true)
        @RequestParam long windowSeconds
    ) {
        Map<String, Object> status = new HashMap<>();
        
        try {
            long currentCount = redisService.getCurrentRequestCount(key, windowSeconds);
            status.put("key", key);
            status.put("windowSeconds", windowSeconds);
            status.put("currentCount", currentCount);
            status.put("status", "active");
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Rate limit 상태 조회 실패: {}", e.getMessage());
            status.put("error", e.getMessage());
            status.put("status", "error");
            return ResponseEntity.badRequest().body(status);
        }
    }
    
    @Operation(
        summary = "Rate Limit 초기화",
        description = "특정 키의 Rate Limit을 초기화합니다."
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetRateLimit(
        @Parameter(description = "초기화할 키", required = true)
        @RequestParam String key
    ) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            redisService.resetRateLimit(key);
            result.put("message", "Rate limit 초기화 완료");
            result.put("key", key);
            result.put("status", "success");
            
            log.info("Rate limit 초기화 완료: {}", key);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Rate limit 초기화 실패: {}", e.getMessage());
            result.put("error", e.getMessage());
            result.put("status", "error");
            return ResponseEntity.badRequest().body(result);
        }
    }
    
    @Operation(
        summary = "모든 Rate Limit 키 조회",
        description = "현재 활성화된 모든 Rate Limit 키를 조회합니다."
    )
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/keys")
    public ResponseEntity<Map<String, Object>> getAllRateLimitKeys() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Set<String> keys = redisService.getAllKeys();
            Set<String> rateLimitKeys = keys.stream()
                .filter(key -> key.startsWith("rate_limit:"))
                .collect(java.util.stream.Collectors.toSet());
            
            result.put("keys", rateLimitKeys);
            result.put("totalCount", rateLimitKeys.size());
            result.put("status", "success");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Rate limit 키 조회 실패: {}", e.getMessage());
            result.put("error", e.getMessage());
            result.put("status", "error");
            return ResponseEntity.badRequest().body(result);
        }
    }
    
    @Operation(
        summary = "Rate Limit 테스트",
        description = "Rate Limit 기능을 테스트합니다."
    )
    @RateLimit(
        windowSeconds = 10,
        maxRequests = 3,
        identifierType = RateLimit.IdentifierType.IP,
        type = RateLimit.RateLimitType.SLIDING_WINDOW,
        message = "테스트 Rate Limit: 10초간 3회 제한"
    )
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testRateLimit() {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Rate Limit 테스트 성공");
        result.put("timestamp", System.currentTimeMillis());
        result.put("status", "success");
        
        return ResponseEntity.ok(result);
    }
    
    @Operation(
        summary = "Token Bucket 테스트",
        description = "Token Bucket Rate Limiting을 테스트합니다."
    )
    @RateLimit(
        type = RateLimit.RateLimitType.TOKEN_BUCKET,
        capacity = 5,
        refillRate = 0.5,
        identifierType = RateLimit.IdentifierType.IP,
        message = "Token Bucket 테스트: 최대 5개 토큰, 초당 0.5개 리필"
    )
    @GetMapping("/test/token-bucket")
    public ResponseEntity<Map<String, Object>> testTokenBucket() {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Token Bucket 테스트 성공");
        result.put("timestamp", System.currentTimeMillis());
        result.put("status", "success");
        
        return ResponseEntity.ok(result);
    }
} 
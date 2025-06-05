package ImgBell.Image;

import ImgBell.Redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecentViewService {
    
    private final RedisService redisService;
    private static final String RECENT_VIEW_KEY = "user:recent:";
    private static final int MAX_RECENT_ITEMS = 10;
    private static final int RECENT_VIEW_TTL = 7; // 7일
    
    /**
     * 최근 본 항목 추가 (Redis List 직접 활용)
     */
    public void addRecentView(Long userId, Long imageId) {
        String key = RECENT_VIEW_KEY + userId;
        
        // 기존 항목 제거 (중복 방지)
        redisService.removeFromList(key, 0, imageId.toString());
        
        // 맨 앞에 추가
        redisService.leftPush(key, imageId.toString());
        
        // 최대 개수 제한 (trim 사용)
        redisService.trimList(key, 0, MAX_RECENT_ITEMS - 1);
        
        // TTL 설정
        redisService.expire(key, RECENT_VIEW_TTL, TimeUnit.DAYS);
    }
    
    /**
     * 최근 본 항목 조회 (개선된 버전)
     */
    public List<Long> getRecentViews(Long userId) {
        String key = RECENT_VIEW_KEY + userId;
        List<Object> result = redisService.getListRange(key, 0, MAX_RECENT_ITEMS - 1);
        
        if (result == null || result.isEmpty()) {
            return new ArrayList<>();
        }
        
        return result.stream()
                .map(item -> Long.valueOf(item.toString()))
                .collect(Collectors.toList());
    }
    
    /**
     * 최근 본 항목에서 특정 이미지 제거
     */
    public void removeRecentView(Long userId, Long imageId) {
        String key = RECENT_VIEW_KEY + userId;
        redisService.removeFromList(key, 1, imageId.toString());
    }
    
    /**
     * 사용자의 최근 본 항목 전체 삭제
     */
    public void clearRecentViews(Long userId) {
        String key = RECENT_VIEW_KEY + userId;
        redisService.deleteValue(key);
    }
    
    /**
     * 최근 본 항목 개수 조회
     */
    public int getRecentViewCount(Long userId) {
        return getRecentViews(userId).size();
    }
    
    /**
     * 배치로 여러 항목 추가 (성능 최적화)
     */
    public void addMultipleRecentViews(Long userId, List<Long> imageIds) {
        String key = RECENT_VIEW_KEY + userId;
        
        for (Long imageId : imageIds) {
            // 중복 제거
            redisService.removeFromList(key, 0, imageId.toString());
            // 앞에 추가
            redisService.leftPush(key, imageId.toString());
        }
        
        // 최대 개수 제한
        redisService.trimList(key, 0, MAX_RECENT_ITEMS - 1);
        
        // TTL 설정
        redisService.expire(key, RECENT_VIEW_TTL, TimeUnit.DAYS);
    }
} 
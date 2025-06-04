package ImgBell.Image;

import ImgBell.Redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RecentViewService {
    
    private final RedisService redisService;
    private static final String RECENT_VIEW_KEY = "user:recent:";
    private static final int MAX_RECENT_ITEMS = 10;
    
    public void addRecentView(Long userId, Long imageId) {
        String key = RECENT_VIEW_KEY + userId;
        
        // 기존 목록 가져오기
        List<Long> recentList = getRecentViews(userId);
        
        // 중복 제거
        recentList.remove(imageId);
        
        // 맨 앞에 추가
        recentList.add(0, imageId);


        if (recentList.size() > MAX_RECENT_ITEMS) {
            recentList = recentList.subList(0, MAX_RECENT_ITEMS);
        }


        // Redis에 저장 (7일 후 만료)
        redisService.setValue(key, recentList, 7, TimeUnit.DAYS);
    }
    
    @SuppressWarnings("unchecked")
    public List<Long> getRecentViews(Long userId) {
        String key = RECENT_VIEW_KEY + userId;
        Object result = redisService.getValue(key);
        
        if (result instanceof List) {
            return (List<Long>) result;
        }
        return new ArrayList<>();
    }
} 
package ImgBell.Image;

import ImgBell.Redis.RedisService;
import ImgBell.GlobalErrorHandler.GlobalException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecentViewService {
    
    private final RedisService redisService;
    private static final String RECENT_VIEW_KEY = "user:recent:";
    private final ObjectMapper objectMapper;
    private static final int MAX_RECENT_ITEMS = 10;
    private static final int RECENT_VIEW_TTL = 7; // 7일
    
    /**
     * 최근 본 항목 추가 (Redis List 직접 활용)
     */
    @Transactional
    public void addRecentView(Long userId, Long imageId, String imageUrl) {
        String key = RECENT_VIEW_KEY + userId;

        try {
            // RecentViewItem 객체를 JSON 문자열로 변환
            RecentViewItem item = new RecentViewItem(imageId, imageUrl);
            String jsonValue = objectMapper.writeValueAsString(item);

            // 기존 동일한 imageId 항목 제거 (중복 방지)
            removeExistingItem(key, imageId);

            // 맨 앞에 추가
            redisService.leftPush(key, jsonValue);

            // 최대 개수 제한
            redisService.trimList(key, 0, MAX_RECENT_ITEMS - 1);

            // TTL 설정
            redisService.expire(key, RECENT_VIEW_TTL, TimeUnit.DAYS);

        } catch (JsonProcessingException e) {
            throw new GlobalException("최근 본 항목 저장 중 오류가 발생했습니다", "RECENT_VIEW_SAVE_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    

    /**
     * 최근 본 항목에서 특정 이미지 제거
     */
    private void removeExistingItem(String key, Long imageId) {
        List<Object> allItems = redisService.getListRange(key, 0, -1);
        if (allItems != null) {
            for (Object item : allItems) {
                try {
                    RecentViewItem recentItem = objectMapper.readValue(item.toString(), RecentViewItem.class);
                    if (recentItem.getImageId().equals(imageId)) {
                        redisService.removeFromList(key, 0, item.toString());
                        break;
                    }
                } catch (JsonProcessingException e) {
                    // 파싱 실패한 항목은 제거
                    redisService.removeFromList(key, 0, item.toString());
                }
            }
        }
    }

    public List<RecentViewItem> getRecentViews(Long userId) {
        String key = RECENT_VIEW_KEY + userId;
        List<Object> result = redisService.getListRange(key, 0, MAX_RECENT_ITEMS - 1);

        if (result == null || result.isEmpty()) {
            return new ArrayList<>();
        }

        return result.stream()
                .map(item -> {
                    try {
                        return objectMapper.readValue(item.toString(), RecentViewItem.class);
                    } catch (JsonProcessingException e) {
                        return null; // 파싱 실패한 항목은 null로 처리
                    }
                })
                .filter(Objects::nonNull) // null 제거
                .collect(Collectors.toList());
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


@Schema(description = "최근 본 이미지 정보")
@Data
@AllArgsConstructor
@NoArgsConstructor
class RecentViewItem {
    @Schema(description = "이미지 ID", example = "1")
    private Long imageId;
    
    @Schema(description = "이미지 URL", example = "https://bucket.s3.amazonaws.com/images/uuid_image.jpg")
    private String imageUrl;
}

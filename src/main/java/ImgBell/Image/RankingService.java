package ImgBell.Image;

import ImgBell.Redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RankingService {
    
    private final RedisService redisService;
    private static final String DAILY_RANKING_KEY = "ranking:daily:";
    private static final String WEEKLY_RANKING_KEY = "ranking:weekly:";
    
    public void updateImageScore(Long imageId, int score) {
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String thisWeek = getWeekKey();
        
        // 일일 랭킹 업데이트
        updateRanking(DAILY_RANKING_KEY + today, imageId, score);
        
        // 주간 랭킹 업데이트
        updateRanking(WEEKLY_RANKING_KEY + thisWeek, imageId, score);
    }
    
    @SuppressWarnings("unchecked")
    public void updateRanking(String key, Long imageId, int score) {
        Map<String, Integer> ranking = (Map<String, Integer>) redisService.getValue(key);
        if (ranking == null) {
            ranking = new HashMap<>();
        }
        
        ranking.put(imageId.toString(), ranking.getOrDefault(imageId.toString(), 0) + score);
        redisService.setValue(key, ranking);
    }
    
    @SuppressWarnings("unchecked")
    public List<Long> getTopImages(String period, int limit) {
        String key = period.equals("daily") ? 
            DAILY_RANKING_KEY + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) :
            WEEKLY_RANKING_KEY + getWeekKey();
        System.out.println("랭킹체크");
        System.out.println(key);
            
        Map<String, Integer> ranking = (Map<String, Integer>) redisService.getValue(key);
        if (ranking == null) {
            return new ArrayList<>();
        }

        System.out.println(ranking);
        
        return ranking.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> Long.valueOf(entry.getKey()))
                .toList();
    }
    
    private String getWeekKey() {
        // 주차 계산 로직
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-'W'ww"));
    }
} 
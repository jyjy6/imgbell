package ImgBell.Image;

import ImgBell.Redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RankingService {
    
    private final RedisService redisService;
    private static final String DAILY_RANKING_KEY = "ranking:daily:";
    private static final String WEEKLY_RANKING_KEY = "ranking:weekly:";
    private static final String MONTHLY_RANKING_KEY = "ranking:monthly:";
    
    // 점수 가중치 설정
    private static final int VIEW_SCORE = 1;
    private static final int LIKE_SCORE = 3;
    private static final int DOWNLOAD_SCORE = 2;
    
    /**
     * 이미지 점수 업데이트 (Sorted Set 활용)
     */
    public void updateImageScore(Long imageId, int score) {
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String thisWeek = getWeekKey();
        String thisMonth = getMonthKey();
        
        // Sorted Set을 사용하여 점수 증가 (더 효율적)
        redisService.incrementScoreInSortedSet(DAILY_RANKING_KEY + today, imageId.toString(), score);
        redisService.incrementScoreInSortedSet(WEEKLY_RANKING_KEY + thisWeek, imageId.toString(), score);
        redisService.incrementScoreInSortedSet(MONTHLY_RANKING_KEY + thisMonth, imageId.toString(), score);
        
        // TTL 설정 (메모리 최적화)
        redisService.expire(DAILY_RANKING_KEY + today, 2, TimeUnit.DAYS);
        redisService.expire(WEEKLY_RANKING_KEY + thisWeek, 8, TimeUnit.DAYS);
        redisService.expire(MONTHLY_RANKING_KEY + thisMonth, 32, TimeUnit.DAYS);
    }
    
    /**
     * 점수 타입별 업데이트 메서드
     */
    public void updateViewScore(Long imageId) {
        updateImageScore(imageId, VIEW_SCORE);
    }
    
    public void updateLikeScore(Long imageId) {
        updateImageScore(imageId, LIKE_SCORE);
    }
    
    public void updateDownloadScore(Long imageId) {
        updateImageScore(imageId, DOWNLOAD_SCORE);
    }
    
    /**
     * 상위 랭킹 이미지 조회 (개선된 버전)
     */
    public List<Long> getTopImages(String period, int limit) {
        String key = getRankingKey(period);
        
        Set<Object> topImages = redisService.getTopRanking(key, limit);
        
        return topImages.stream()
                .map(imageId -> Long.valueOf(imageId.toString()))
                .collect(Collectors.toList());
    }
    
    /**
     * 점수와 함께 랭킹 조회
     */
    public List<RankingEntry> getTopImagesWithScores(String period, int limit) {
        String key = getRankingKey(period);
        
        Set<ZSetOperations.TypedTuple<Object>> rankingWithScores = 
            redisService.getRangeWithScores(key, 0, limit - 1);
        
        return rankingWithScores.stream()
                .map(tuple -> new RankingEntry(
                    Long.valueOf(tuple.getValue().toString()),
                    tuple.getScore().intValue()
                ))
                .collect(Collectors.toList());
    }
    
    /**
     * 특정 이미지의 랭킹 점수 조회
     */
    public Double getImageScore(Long imageId, String period) {
        String key = getRankingKey(period);
        return redisService.getScore(key, imageId.toString());
    }
    
    private String getRankingKey(String period) {
        return switch (period.toLowerCase()) {
            case "daily" -> DAILY_RANKING_KEY + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            case "weekly" -> WEEKLY_RANKING_KEY + getWeekKey();
            case "monthly" -> MONTHLY_RANKING_KEY + getMonthKey();
            default -> DAILY_RANKING_KEY + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        };
    }
    
    private String getWeekKey() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-'W'ww"));
    }
    
    private String getMonthKey() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }
    
    /**
     * 랭킹 엔트리 클래스
     */
    public static class RankingEntry {
        private final Long imageId;
        private final Integer score;
        
        public RankingEntry(Long imageId, Integer score) {
            this.imageId = imageId;
            this.score = score;
        }
        
        public Long getImageId() { return imageId; }
        public Integer getScore() { return score; }
    }
} 
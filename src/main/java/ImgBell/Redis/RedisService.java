package ImgBell.Redis;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void setValue(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void setValue(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public Object getValue(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void deleteValue(String key) {
        redisTemplate.delete(key);
    }

    public boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    public Set<String> getAllKeys() {
        Set<String> keys = redisTemplate.keys("*"); // 모든 키 가져오기

        return keys;
    }

    public void addToSortedSet(String key, Object value, double score) {
        redisTemplate.opsForZSet().add(key, value, score);
    }

    public void incrementScoreInSortedSet(String key, Object value, double score) {
        redisTemplate.opsForZSet().incrementScore(key, value, score);
    }

    public Set<ZSetOperations.TypedTuple<Object>> getRangeWithScores(String key, long start, long end) {
        return redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
    }

    public Set<Object> getTopRanking(String key, long count) {
        return redisTemplate.opsForZSet().reverseRange(key, 0, count - 1);
    }

    public Double getScore(String key, Object value) {
        return redisTemplate.opsForZSet().score(key, value);
    }

    public void expire(String key, long timeout, TimeUnit unit) {
        redisTemplate.expire(key, timeout, unit);
    }

    public void leftPush(String key, Object value) {
        redisTemplate.opsForList().leftPush(key, value);
    }

    public void removeFromList(String key, long count, Object value) {
        redisTemplate.opsForList().remove(key, count, value);
    }

    public List<Object> getListRange(String key, long start, long end) {
        return redisTemplate.opsForList().range(key, start, end);
    }

    public void trimList(String key, long start, long end) {
        redisTemplate.opsForList().trim(key, start, end);
    }

    public void setHashValue(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    public Object getHashValue(String key, String field) {
        return redisTemplate.opsForHash().get(key, field);
    }

    public void incrementHashValue(String key, String field, long delta) {
        redisTemplate.opsForHash().increment(key, field, delta);
    }
}
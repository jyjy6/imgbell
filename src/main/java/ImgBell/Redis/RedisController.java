package ImgBell.Redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/redis")
@CrossOrigin(origins = "http://localhost:5173") // Vue 개발서버 주소
public class RedisController {

    @Autowired
    private RedisService redisService;

    @PostMapping("/set")
    public ResponseEntity<String> setData(@RequestBody Map<String, Object> request) {
        String key = (String) request.get("key");
        Object value = request.get("value");
        redisService.setValue(key, value);
        return ResponseEntity.ok("Data saved successfully");
    }

    @GetMapping("/get/{key}")
    public ResponseEntity<Object> getData(@PathVariable String key) {
        Object value = redisService.getValue(key);
        return ResponseEntity.ok(value);
    }

    @DeleteMapping("/delete/{key}")
    public ResponseEntity<String> deleteData(@PathVariable String key) {
        redisService.deleteValue(key);
        return ResponseEntity.ok("Data deleted successfully");
    }

    @GetMapping("/get/allkeys")
    public ResponseEntity<?> getAllKey(){
        return ResponseEntity.ok(redisService.getAllKeys());
    }
}
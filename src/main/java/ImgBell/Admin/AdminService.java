package ImgBell.Admin;

import ImgBell.Forum.Forum;
import ImgBell.Forum.ForumRepository;
import ImgBell.Image.Image;
import ImgBell.Image.ImageRepository;
import ImgBell.Member.Member;
import ImgBell.Member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class AdminService {
    private final MemberRepository memberRepository;
    private final ImageRepository imageRepository;
    private final ForumRepository forumRepository;

    // 통계 및 최근 데이터 반환
    public Map<String, Object> getDashboardData() {
        Map<String, Object> result = new HashMap<>();

        // 통계
        Map<String, Object> stats = new HashMap<>();
        stats.put("userCount", memberRepository.count());
        stats.put("imageCount", imageRepository.count());

        // 오늘 방문자 (예시: 오늘 가입자 수로 대체, 실제 방문자 테이블 있으면 수정)
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
        long todayVisit = memberRepository.countByCreatedAtBetween(startOfDay, endOfDay);
        stats.put("todayVisit", todayVisit);

        // 신고/문의 (예시: 0으로 반환, 실제 테이블 있으면 수정)
        stats.put("reportCount", 0);

        result.put("stats", stats);

        // 최근 가입 회원 (최신 5명)
        List<Map<String, Object>> recentUsers = memberRepository.findTop5ByOrderByCreatedAtDesc()
                .stream()
                .map(user -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", user.getId());
                    map.put("username", user.getUsername());
                    map.put("displayName", user.getDisplayName());
                    map.put("email", user.getEmail());
                    return map;
                })
                .toList();
        result.put("recentUsers", recentUsers);

        // 최근 업로드 이미지 (최신 5개)
        List<Map<String, Object>> recentImages = imageRepository.findTop5ByOrderByCreatedAtDesc()
                .stream()
                .map(img -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", img.getId());
                    map.put("imageName", img.getImageName());
                    map.put("uploaderName", img.getUploaderName());
                    return map;
                })
                .toList();
        result.put("recentImages", recentImages);

        // 최근 포럼 글 (최신 5개)
        List<Map<String, Object>> recentPosts = forumRepository.findTop5ByOrderByCreatedAtDesc()
                .stream()
                .map(post -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", post.getId());
                    map.put("title", post.getTitle());
                    map.put("authorDisplayName", post.getAuthorDisplayName());
                    return map;
                })
                .toList();
        result.put("recentPosts", recentPosts);

        return result;
    }
}

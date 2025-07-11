package ImgBell.Forum;

import ImgBell.Member.CustomUserDetails;
import ImgBell.GlobalErrorHandler.GlobalException;
import ImgBell.Image.RankingService;
import ImgBell.Redis.RedisService;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@RequiredArgsConstructor
@Service
public class ForumService {
    private final ForumRepository forumRepository;
    private final RankingService rankingService;
    private final RedisService redisService;
    
    // 🔥 Prometheus 메트릭 추가
    private final Counter forumPostCounter;

    public void postForum(ForumFormDto forumDto, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()){
            throw new GlobalException("인증정보가 없습니다", "POST_FORUM_LOGIN_NEEDED", HttpStatus.UNAUTHORIZED);
        }
        String username = ((CustomUserDetails) auth.getPrincipal()).getUsername();
        String displayName = ((CustomUserDetails) auth.getPrincipal()).getDisplayName();


        Forum forum = new Forum();
        forum.setTitle(forumDto.getTitle());
        forum.setContent(forumDto.getContent());
        Forum.PostType postType = forumDto.getType() != null ?
                forumDto.getType() : Forum.PostType.NORMAL;
        forum.setType(postType);
        forum.setAuthorDisplayName(displayName);
        forum.setAuthorUsername(username);
        Forum savedForum = forumRepository.save(forum);
        
        // 🔥 Prometheus 메트릭: 포럼 포스트 카운터 증가
        forumPostCounter.increment();
        log.info("포럼 포스트 메트릭 증가: {}", savedForum.getId());
    }

    public void editForum(Long id, ForumFormDto forumDto, Authentication auth) {
        try {
            String username = ((CustomUserDetails) auth.getPrincipal()).getUsername();
            String displayName = ((CustomUserDetails) auth.getPrincipal()).getDisplayName();

            Forum forum = forumRepository.findById(id)
                    .orElseThrow(() -> new GlobalException("게시글을 찾을 수 없습니다", "FORUM_NOT_FOUND", HttpStatus.NOT_FOUND));

            if (!username.equals(forum.getAuthorUsername())) {
                throw new GlobalException("글쓴이만 수정할 수 있습니다", "UNAUTHORIZED_EDIT", HttpStatus.FORBIDDEN);
            }

            forum.setTitle(forumDto.getTitle());
            forum.setContent(forumDto.getContent());
            Forum.PostType postType = forumDto.getType() != null ?
                    forumDto.getType() : Forum.PostType.NORMAL;
            forum.setType(postType);
            forum.setAuthorDisplayName(displayName);
            forum.setAuthorUsername(username);
            forumRepository.save(forum);
        } catch (GlobalException e) {
            log.error("예상치못한에러{}", e.getMessage());
            throw e;
        }
    }

    public Page<ForumResponse> getForumList(Forum.PostType forumType, Pageable pageable) {
        return forumRepository.findByTypeAndIsDeletedFalse(forumType, pageable)
                .map(ForumResponse::forList);
    }

    // 또는 상세 조회시에는
    public ForumResponse getForumDetail(Long id) {
        Forum forum = forumRepository.findById(id).orElseThrow(() -> new GlobalException("그런 게시물 없습니다", "FORUM_NOT_FOUND"));
        return ForumResponse.from(forum);  // from 메서드 사용 (전체 정보 포함)
    }

    //포럼 수정용 Dto반환 (오버로딩)
    public ForumFormDto getForumDetail(Long id, Authentication auth) {

        String username = ((CustomUserDetails) auth.getPrincipal()).getUsername();
        Forum forum = forumRepository.findById(id).orElseThrow(() -> new GlobalException("그런 게시물 없습니다", "FORUM_NOT_FOUND"));

        if (!username.equals(forum.getAuthorUsername())) {
            throw new GlobalException("글쓴이만 수정할 수 있습니다", "UNAUTHORIZED_EDIT", HttpStatus.FORBIDDEN);
        }
        ForumFormDto editForm = new ForumFormDto();
        editForm.setTitle(forum.getTitle());
        editForm.setContent(forum.getContent());
        editForm.setType(forum.getType());

        return editForm;
    }

    // 검색 기능 수정 - 키워드에 % 기호 추가
    public Page<ForumResponse> searchForums(String keyword, Pageable pageable) {
        // 키워드 앞뒤에 % 기호 추가
        String searchKeyword = "%" + keyword + "%";
        return forumRepository.findByKeywordAndIsDeletedFalse(searchKeyword, pageable)
                .map(ForumResponse::forList);
    }
    
    // ===== 랭킹 관련 메소드들 =====
    
    /**
     * 포럼 조회수 증가
     */
    @Transactional
    public void incrementViewCount(Long forumId) {
        // DB 업데이트
        Forum forum = forumRepository.findById(forumId)
                .orElseThrow(() -> new GlobalException("게시글을 찾을 수 없습니다.", "FORUM_NOT_FOUND", HttpStatus.NOT_FOUND));
        forum.setViewCount(forum.getViewCount() + 1);
        forumRepository.save(forum);
        
        // Redis 캐시 업데이트
        redisService.incrementHashValue("forum:stats:" + forumId, "viewCount", 1);
        
        // 랭킹 점수 업데이트
        rankingService.updateViewScore("forum", forumId);
    }
    
    /**
     * 포럼 좋아요 수 증가
     */
    @Transactional
    public void incrementLikeCount(Long forumId) {
        // DB 업데이트
        Forum forum = forumRepository.findById(forumId)
                .orElseThrow(() -> new GlobalException("게시글을 찾을 수 없습니다.", "FORUM_NOT_FOUND", HttpStatus.NOT_FOUND));
        forum.setLikeCount(forum.getLikeCount() + 1);
        forumRepository.save(forum);
        
        // Redis 캐시 업데이트
        redisService.incrementHashValue("forum:stats:" + forumId, "likeCount", 3);
        
        // 랭킹 점수 업데이트
        rankingService.updateLikeScore("forum", forumId);
    }
    
    /**
     * 포럼 좋아요 수 감소
     */
    @Transactional
    public void decrementLikeCount(Long forumId) {
        // DB 업데이트
        Forum forum = forumRepository.findById(forumId)
                .orElseThrow(() -> new GlobalException("게시글을 찾을 수 없습니다.", "FORUM_NOT_FOUND", HttpStatus.NOT_FOUND));
        forum.setLikeCount(Math.max(0, forum.getLikeCount() - 1)); // 0 미만으로 내려가지 않도록
        forumRepository.save(forum);
        
        // Redis 캐시 업데이트
        redisService.incrementHashValue("forum:stats:" + forumId, "likeCount", -3);
        
        // 랭킹 점수 업데이트 (감소)
        rankingService.updateScore("forum", forumId, -3); // 좋아요 취소는 -3점
    }
    
    /**
     * Redis에서 포럼 조회수 조회
     */
    public Long getViewCount(Long forumId) {
        // Redis에서 먼저 확인
        Object cachedCount = redisService.getHashValue("forum:stats:" + forumId, "viewCount");
        if (cachedCount != null) {
            return Long.valueOf(cachedCount.toString());
        }
        
        // Redis에 없으면 DB에서 가져와서 캐시 설정
        Forum forum = forumRepository.findById(forumId).orElse(null);
        if (forum != null) {
            redisService.setHashValue("forum:stats:" + forumId, "viewCount", forum.getViewCount());
            return (long) forum.getViewCount();
        }
        
        return 0L;
    }
    
    /**
     * Redis에서 포럼 좋아요 수 조회
     */
    public Long getLikeCount(Long forumId) {
        // Redis에서 먼저 확인
        Object cachedCount = redisService.getHashValue("forum:stats:" + forumId, "likeCount");
        if (cachedCount != null) {
            return Long.valueOf(cachedCount.toString());
        }
        
        // Redis에 없으면 DB에서 가져와서 캐시 설정
        Forum forum = forumRepository.findById(forumId).orElse(null);
        if (forum != null) {
            redisService.setHashValue("forum:stats:" + forumId, "likeCount", forum.getLikeCount());
            return (long) forum.getLikeCount();
        }
        
        return 0L;
    }
}

package ImgBell.Forum;

import ImgBell.Forum.ForumComment.ForumComment;
import ImgBell.Forum.ForumComment.ForumCommentDto;
import ImgBell.GlobalErrorHandler.GlobalException;
import ImgBell.Image.RankingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RequestMapping("/api/forum")
@RestController
public class ForumController {
    private final ForumService forumService;
    private final ForumRepository forumRepository;
    private final RankingService rankingService;

    @PostMapping("/post")
    public ResponseEntity<String> postForum(@RequestBody ForumFormDto forumDto, Authentication auth) {

        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }
        forumService.postForum(forumDto, auth);
        return ResponseEntity.ok("Post created");
    }

    @GetMapping("/list")
    public Page<ForumResponse> getForumNoticeList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "NORMAL") Forum.PostType forumType) {
        return forumService.getForumList(forumType,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
    }

    //포럼 상세보기
    @GetMapping("/{id}")
    public ForumResponse getForumPost(@PathVariable("id") Long id){
        return forumService.getForumDetail(id);
    }

    //포럼 수정 시 기존데이터 불러오기
    @GetMapping("/edit/{id}")
    public ForumFormDto getForForumEdit(@PathVariable("id") Long id, Authentication auth){

        return forumService.getForumDetail(id, auth);
    }

    @PutMapping("/edit/{id}")
    public ResponseEntity<String> editForum(@PathVariable("id") Long id,
                                            @RequestBody ForumFormDto forumFormDto,
                                            Authentication auth) {
        forumService.editForum(id, forumFormDto, auth);
        return ResponseEntity.ok("수정완료");
    }

    @PutMapping("/view/{id}")
    public void countView(@PathVariable Long id){
        // ✅ ForumService의 통합 메서드 사용 (DB + Redis + 랭킹 한번에 처리) 중복view처리는 프론트에서함
        forumService.incrementViewCount(id);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> deleteForum(@PathVariable("id") Long id) {
        try {
            // 게시글 존재 여부 확인
            Forum forum = forumRepository.findById(id)
                    .orElseThrow(() -> new GlobalException("게시글을 찾을 수 없습니다", "FORUM_NOT_FOUND", HttpStatus.NOT_FOUND));

            // 이미 삭제된 게시글인지 확인
            if (forum.getIsDeleted()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "이미 삭제된 게시글입니다.", "success", false));
            }

            // 소프트 삭제 처리
            forum.markAsDeleted();
            forumRepository.save(forum);

            // 성공 응답 반환
            return ResponseEntity.ok()
                    .body(Map.of(
                            "message", "게시글이 성공적으로 삭제되었습니다.",
                            "success", true,
                            "deletedId", id
                    ));

        } catch (GlobalException e) {
            // 비즈니스 로직 관련 예외 (게시글 없음 등)
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            // 예상치 못한 서버 에러
            System.err.println("게시글 삭제 중 오류 발생: " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message", "서버 내부 오류가 발생했습니다.",
                            "success", false
                    ));
        }
    }

    // 검색 기능 추가
    @GetMapping("/search")
    public Page<ForumResponse> searchForums(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return forumService.searchForums(keyword, 
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
    }



    //    포럼 랭킹==================================================
    @Operation(
            summary = "포럼 랭킹 조회",
            description = "기간별 포럼 랭킹을 조회합니다. (일간/주간)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "랭킹 조회 성공")
    })
    @GetMapping("/ranking")
    public ResponseEntity<List<Long>> getForumRanking(
            @Parameter(description = "기간", example = "daily", schema = @Schema(allowableValues = {"daily", "weekly"}))
            @RequestParam(defaultValue = "daily") String period,
            @Parameter(description = "조회할 랭킹 수", example = "10")
            @RequestParam(defaultValue = "10") int limit) {

        List<Long> topForums = rankingService.getTop("forum", period, limit);
        return ResponseEntity.ok(topForums);
    }

    @Operation(
            summary = "포럼 랭킹 조회 (점수 포함)",
            description = "기간별 포럼 랭킹을 점수와 함께 조회합니다. (일간/주간)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "랭킹 조회 성공")
    })
    @GetMapping("/ranking/with-scores")
    public ResponseEntity<List<RankingService.RankingEntry>> getForumRankingWithScores(
            @Parameter(description = "기간", example = "daily", schema = @Schema(allowableValues = {"daily", "weekly"}))
            @RequestParam(defaultValue = "daily") String period,
            @Parameter(description = "조회할 랭킹 수", example = "10")
            @RequestParam(defaultValue = "10") int limit) {

        List<RankingService.RankingEntry> topForums = rankingService.getTopWithScores("forum", period, limit);
        return ResponseEntity.ok(topForums);
    }

    @Operation(
            summary = "특정 포럼 랭킹 점수 조회",
            description = "특정 포럼의 랭킹 점수를 조회합니다."
    )
    @GetMapping("/ranking/score/{forumId}")
    public ResponseEntity<Double> getForumRankingScore(
            @PathVariable Long forumId,
            @Parameter(description = "기간", example = "daily", schema = @Schema(allowableValues = {"daily", "weekly"}))
            @RequestParam(defaultValue = "daily") String period) {

        Double score = rankingService.getScore("forum", forumId, period);
        return ResponseEntity.ok(score != null ? score : 0.0);
    }

}

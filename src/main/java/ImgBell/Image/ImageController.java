package ImgBell.Image;

import ImgBell.Image.ElasticSearch.ImageSearchService;
import ImgBell.Member.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/image")
@RequiredArgsConstructor
@Tag(name = "이미지 관리", description = "이미지 업로드, 조회, 수정, 삭제 관련 API")
@Slf4j
public class ImageController {

    private final RecentViewService recentViewService;
    private final RankingService rankingService;
    private final ImageService imageService;
    private final ImageAIService imageAIService;
    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    @Operation(
        summary = "이미지 업로드용 Presigned URL 생성",
        description = "S3에 이미지를 업로드하기 위한 Presigned URL을 생성합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Presigned URL 생성 성공",
            content = @Content(schema = @Schema(implementation = PresignedUrlResponse.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @ResponseBody
    @GetMapping("/presigned-url")
    public PresignedUrlResponse getPermanentImgUrl(
        @Parameter(description = "업로드할 파일명", required = true, example = "image.jpg")
        @RequestParam String filename, 
        
        @Parameter(description = "파일 타입", required = true, example = "images")
        @RequestParam String filetype
    ) {
        String decodedFilename = URLDecoder.decode(filename, StandardCharsets.UTF_8);
        var decodedRandomFilename = UUID.randomUUID().toString() + "_" + decodedFilename;

        // 타입에 따른 폴더에 저장
        var fullPath = filetype + "/" + decodedRandomFilename;
        var presignedUrl = imageService.createPresignedUrl(fullPath);

        // 베이스 URL 생성 (쿼리 파라미터 없는 실제 이미지 URL)
        String imageUrl = "https://" + bucket + ".s3.amazonaws.com/" + fullPath;

        // 프론트엔드로 둘 다 전송
        return new PresignedUrlResponse(presignedUrl, imageUrl);
    }

    @Operation(
        summary = "이미지 정보 DB 저장",
        description = "업로드된 이미지들의 메타데이터를 데이터베이스에 저장합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "이미지 정보 저장 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PostMapping("/upload")
    public ResponseEntity<?> registerFiles(
        @Parameter(description = "저장할 이미지 정보 리스트", required = true)
        @RequestBody List<ImageDto> images
    ) {
        try {
            // DB에 파일 정보 저장
            System.out.println("이미지 업로드");
            imageService.saveFileInfoToDb(images);
            return new ResponseEntity<>("Added", HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(
        summary = "이미지 목록 조회",
        description = "다양한 필터 조건으로 이미지 목록을 페이지네이션으로 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "이미지 목록 조회 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/list")
    public ResponseEntity<Page<ImageDto>> getImageList(
            @Parameter(description = "페이지 정보 (기본: 20개씩, ID 내림차순)")
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication auth,
            
            @Parameter(description = "태그 필터", example = "풍경")
            @RequestParam(required = false) String tag,
            
            @Parameter(description = "이미지명 필터", example = "아름다운")
            @RequestParam(required = false) String imageName,
            
            @Parameter(description = "업로더명 필터", example = "홍길동")
            @RequestParam(required = false) String uploaderName,
            
            @Parameter(description = "아티스트명 필터", example = "김작가")
            @RequestParam(required = false) String artist,
            
            @Parameter(description = "키워드 검색", example = "자연")
            @RequestParam(required = false) String keyword,
            
            @Parameter(description = "검색 타입", example = "title")
            @RequestParam(required = false) String searchType,
            
            @Parameter(description = "등급 필터", example = "A")
            @RequestParam(required = false) String grade,
            
            @Parameter(description = "내 이미지만 조회", example = "true")
            @RequestParam(required = false) Boolean myImageList,
            
            @Parameter(description = "좋아요한 이미지만 조회", example = "true")
            @RequestParam(required = false) Boolean likeImageList
    ) {
        // 마이페이지 이미지리스트는 로그인 유저의 업로드만 필터링
        if (Boolean.TRUE.equals(myImageList) && auth != null && auth.isAuthenticated()) {
            uploaderName = ((CustomUserDetails)auth.getPrincipal()).getUsername();  // ✅ 현재 로그인된 유저 이름으로 덮어쓰기
        }
        return ResponseEntity.ok(imageService.getImageList(
                pageable, tag, imageName, uploaderName, artist, keyword, searchType, grade, myImageList, likeImageList, auth
        ));
    }

    @Operation(
        summary = "이미지 상세 조회",
        description = "특정 이미지의 상세 정보를 조회합니다. 조회수 증가 옵션 포함."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "이미지 상세 조회 성공",
            content = @Content(schema = @Schema(implementation = ImageDto.class))),
        @ApiResponse(responseCode = "404", description = "이미지를 찾을 수 없음")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}")
    public ResponseEntity<ImageDto> getImageDetail(
        @Parameter(description = "이미지 ID", required = true, example = "1")
        @PathVariable Long id, 
        
        @Parameter(description = "조회수 증가 여부", example = "true")
        @RequestParam(defaultValue = "false") Boolean increaseView, 
        
        Authentication auth
    ) {
        return ResponseEntity.ok(imageService.getImageDetail(id, increaseView, auth));
    }

    @Operation(
        summary = "이미지 삭제",
        description = "본인이 업로드한 이미지를 삭제합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "이미지 삭제 성공"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "이미지를 찾을 수 없음")
    })
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteImage(
        @Parameter(description = "삭제할 이미지 ID", required = true, example = "1")
        @PathVariable Long id, 
        Authentication auth
    ) {
        return imageService.deleteImage(id,auth);
    }

    @Operation(
        summary = "이미지 정보 수정",
        description = "본인이 업로드한 이미지의 정보를 수정합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "이미지 수정 성공"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "이미지를 찾을 수 없음")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/edit/{id}")
    public ResponseEntity<?> editImage(
        @Parameter(description = "수정할 이미지 정보", required = true)
        @RequestBody ImageDto dto, 
        Authentication auth ) {
        return imageService.editImage(dto, auth);
    }

    @Operation(
        summary = "이미지 공개/비공개 토글",
        description = "이미지의 공개/비공개 상태를 변경합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "공개 상태 변경 성공"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "이미지를 찾을 수 없음")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/ispublic/{id}")
    public ResponseEntity<?> toggleImagePublic(
        @Parameter(description = "상태를 변경할 이미지 ID", required = true, example = "1")
        @PathVariable Long id, 
        Authentication auth
    ){
        try {
            boolean isPublic = imageService.toggleImagePublic(id, auth);
            return ResponseEntity.ok(Map.of("success", true, "isPublic", isPublic));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(
        summary = "인기 이미지 조회",
        description = "조회수, 좋아요 등을 기준으로 인기 이미지 목록을 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "인기 이미지 조회 성공")
    })
    @GetMapping("/popular")
    public ResponseEntity<Page<ImageDto>> getPopularImages(
            @Parameter(description = "페이지 정보 (기본: 10개씩)")
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(imageService.getPopularImages(pageable));
    }

    @Operation(
        summary = "최근 본 이미지 조회",
        description = "로그인한 사용자의 최근 본 이미지 목록을 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "최근 본 이미지 조회 성공"),
        @ApiResponse(responseCode = "204", description = "로그인하지 않은 사용자")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/recent")
    public ResponseEntity<?> getRecentViews(Authentication auth) {
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CustomUserDetails) {
            Long userId = ((CustomUserDetails) auth.getPrincipal()).getId();
            List<RecentViewItem> recentImages = recentViewService.getRecentViews(userId);
            return ResponseEntity.ok(recentImages);
        }

        // 로그인 안 된 경우 아무 동작 안 함 (204 No Content or 401 Unauthorized 등 선택 가능)
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "이미지 랭킹 조회",
        description = "기간별 이미지 랭킹을 조회합니다. (일간/주간/월간)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "랭킹 조회 성공")
    })
    @GetMapping("/ranking")
    public ResponseEntity<List<Long>> getRanking(
            @Parameter(description = "기간", example = "daily", schema = @Schema(allowableValues = {"daily", "weekly", "monthly"}))
            @RequestParam(defaultValue = "daily") String period,
            @Parameter(description = "조회할 랭킹 수", example = "10")
            @RequestParam(defaultValue = "10") int limit) {

        List<Long> topImages = rankingService.getTopImages(period, limit);
        return ResponseEntity.ok(topImages);
    }
    
    @Operation(
        summary = "점수 포함 이미지 랭킹 조회",
        description = "기간별 이미지 랭킹을 점수와 함께 조회합니다."
    )
    @GetMapping("/ranking/with-scores")
    public ResponseEntity<List<RankingService.RankingEntry>> getRankingWithScores(
            @Parameter(description = "기간", example = "daily")
            @RequestParam(defaultValue = "daily") String period,
            
            @Parameter(description = "조회할 랭킹 수", example = "10")
            @RequestParam(defaultValue = "10") int limit) {

        List<RankingService.RankingEntry> topImages = rankingService.getTopImagesWithScores(period, limit);
        return ResponseEntity.ok(topImages);
    }
    
    @Operation(
        summary = "특정 이미지 점수 조회",
        description = "특정 이미지의 기간별 점수를 조회합니다."
    )
    @GetMapping("/ranking/score/{imageId}")
    public ResponseEntity<Double> getImageScore(
            @Parameter(description = "이미지 ID", required = true, example = "1")
            @PathVariable Long imageId,
            
            @Parameter(description = "기간", example = "daily")
            @RequestParam(defaultValue = "daily") String period) {
        
        Double score = rankingService.getImageScore(imageId, period);
        return ResponseEntity.ok(score != null ? score : 0.0);
    }
    
    @Operation(
        summary = "이미지 다운로드 기록",
        description = "이미지 다운로드 횟수를 증가시킵니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "다운로드 기록 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PostMapping("/download/{imageId}")
    public ResponseEntity<?> downloadImage(
        @Parameter(description = "다운로드할 이미지 ID", required = true, example = "1")
        @PathVariable Long imageId
    ) {
        try {
            imageService.incrementDownloadCount(imageId);
            return ResponseEntity.ok(Map.of("success", true, "message", "다운로드가 기록되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

//    여기부터 AI ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ
    @Operation(summary = "이미지 AI 분석", description = "이미지 URL을 받아서 AI로 자동 태그 생성 및 품질 분석")
    @ApiResponse(responseCode = "200", description = "분석 성공")
    @PostMapping("/ai/analyze")
    public ResponseEntity<ImageAIService.ImageAnalysisResult> analyzeImageByUrl(@RequestBody Map<String, String> request) {
        try {
            String imageUrl = request.get("imageUrl");
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            ImageAIService.ImageAnalysisResult result = imageAIService.analyzeImage(imageUrl);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("이미지 AI 분석 실패", e);
            return ResponseEntity.status(500).build();
        }
    }

    @Operation(summary = "이미지 파일 AI 분석", description = "업로드된 이미지 파일을 직접 받아서 AI로 분석")
    @ApiResponse(responseCode = "200", description = "분석 성공")
    @PostMapping("/ai/analyze/file")
    public ResponseEntity<ImageAIService.ImageAnalysisResult> analyzeImageByFile(
            @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            // 이미지 파일 검증
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().build();
            }
            ImageAIService.ImageAnalysisResult result = imageAIService.analyzeImageFile(file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("이미지 파일 AI 분석 실패", e);
            return ResponseEntity.status(500).build();
        }
    }



    @Operation(summary = "이미지 AI 캐릭터 성격으로 분석", description = "이미지 URL을 받아서 AI로 캐릭터말투로 태그 생성 및 품질 분석")
    @ApiResponse(responseCode = "200", description = "분석 성공")
    @PostMapping("/ai/char/analyze/")
    public ResponseEntity<ImageAIService.ImageCharAnalysisResult> charAnalyzeImageByUrl(@RequestBody Map<String, String> request) {
        try {
            String imageUrl = request.get("imageUrl");
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            ImageAIService.ImageCharAnalysisResult result = imageAIService.charAnalyzeImage(imageUrl);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("이미지 AI 분석 실패", e);
            return ResponseEntity.status(500).build();
        }
    }

    @Operation(summary = "이미지 파일 AI 캐릭터 성격으로 분석", description = "업로드된 이미지 파일을 직접 받아서 AI로 캐릭터 말투로 분석")
    @ApiResponse(responseCode = "200", description = "분석 성공")
    @PostMapping("/ai/char/analyze/file")
    public ResponseEntity<ImageAIService.ImageCharAnalysisResult> charAnalyzeImageByFile(
            @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            // 이미지 파일 검증
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().build();
            }
            ImageAIService.ImageCharAnalysisResult result = imageAIService.charAnalyzeImageFile(file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("이미지 파일 AI 분석 실패", e);
            return ResponseEntity.status(500).build();
        }
    }





    @Schema(description = "Presigned URL 응답")
    @Getter
    @Setter
    @AllArgsConstructor
    class PresignedUrlResponse {
        @Schema(description = "S3 업로드용 Presigned URL", example = "https://bucket.s3.amazonaws.com/...")
        private String presignedUrl;
        
        @Schema(description = "실제 이미지 접근 URL", example = "https://bucket.s3.amazonaws.com/images/uuid_image.jpg")
        private String imageUrl;

        // 생성자, getter, setter
    }



}



//페이지네이션할때 뭐 권장되는 방식이라곤 하는데 필요없음 딱히
@AllArgsConstructor
@NoArgsConstructor
class PageResponse<T> {
    private List<T> content;
    private int totalPages;
    private long totalElements;
    private int size;
    private int number;
}
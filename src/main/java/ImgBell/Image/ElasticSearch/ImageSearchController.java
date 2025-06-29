package ImgBell.Image.ElasticSearch;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/image/search")
@Tag(name = "이미지 검색", description = "ElasticSearch 기반 이미지 검색 API")
@Slf4j
public class ImageSearchController {
    
    private final ImageSearchService imageSearchService;

    @Operation(
        summary = "스마트 이미지 검색",
        description = "키워드를 통한 이미지명, 작가명, 태그명 검색. 오타 허용 및 부분 검색 지원. 페이지네이션 지원"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "검색 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @GetMapping("/smart")
    public ResponseEntity<Page<ImageDocument>> smartSearch(
        @Parameter(description = "검색 키워드", required = true, example = "자연")
        @RequestParam String keyword,
        
        @Parameter(description = "이미지 등급 필터", example = "GENERAL")
        @RequestParam(required = false) String imageGrade,
        
        @Parameter(description = "공개 이미지만 조회", example = "true")
        @RequestParam(required = false) Boolean isPublic,
        
        @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
        @RequestParam(defaultValue = "0") int page,
        
        @Parameter(description = "페이지 크기", example = "20")
        @RequestParam(defaultValue = "20") int size
    ) {
        log.info("🔍 스마트 검색 요청: keyword={}, grade={}, public={}, page={}, size={}", 
                keyword, imageGrade, isPublic, page, size);
        
        Page<ImageDocument> results = imageSearchService.smartSearch(keyword, imageGrade, isPublic, page, size);
        return ResponseEntity.ok(results);
    }

    @Operation(
        summary = "태그 기반 검색",
        description = "하나 이상의 태그명으로 이미지 검색. 페이지네이션 지원"
    )
    @PostMapping("/tags")
    public ResponseEntity<Page<ImageDocument>> searchByTags(
        @Parameter(description = "검색할 태그명 리스트", required = true)
        @RequestBody List<String> tagNames,
        
        @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
        @RequestParam(defaultValue = "0") int page,
        
        @Parameter(description = "페이지 크기", example = "20")
        @RequestParam(defaultValue = "20") int size
    ) {
        log.info("🏷️ 태그 검색 요청: tags={}, page={}, size={}", tagNames, page, size);
        
        Page<ImageDocument> results = imageSearchService.searchByTags(tagNames, page, size);
        return ResponseEntity.ok(results);
    }

    @Operation(
        summary = "인기 이미지 조회",
        description = "인기도 점수 기준으로 정렬된 이미지 목록. 페이지네이션 지원"
    )
    @GetMapping("/popular")
    public ResponseEntity<Page<ImageDocument>> getPopularImages(
        @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
        @RequestParam(defaultValue = "0") int page,
        
        @Parameter(description = "페이지 크기", example = "10")
        @RequestParam(defaultValue = "10") int size
    ) {
        log.info("🔥 인기 이미지 요청: page={}, size={}", page, size);
        
        Page<ImageDocument> results = imageSearchService.getPopularImages(page, size);
        return ResponseEntity.ok(results);
    }

    @Operation(
        summary = "최신 이미지 조회",
        description = "최근 업로드된 이미지 목록. 페이지네이션 지원"
    )
    @GetMapping("/recent")
    public ResponseEntity<Page<ImageDocument>> getRecentImages(
        @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
        @RequestParam(defaultValue = "0") int page,
        
        @Parameter(description = "페이지 크기", example = "10")
        @RequestParam(defaultValue = "10") int size
    ) {
        log.info("🆕 최신 이미지 요청: page={}, size={}", page, size);
        
        Page<ImageDocument> results = imageSearchService.getRecentImages(page, size);
        return ResponseEntity.ok(results);
    }

    @Operation(
        summary = "업로더별 이미지 검색",
        description = "특정 업로더가 업로드한 이미지 목록. 페이지네이션 지원"
    )
    @GetMapping("/uploader/{uploaderId}")
    public ResponseEntity<Page<ImageDocument>> searchByUploader(
        @Parameter(description = "업로더 ID", required = true, example = "1")
        @PathVariable Long uploaderId,
        
        @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
        @RequestParam(defaultValue = "0") int page,
        
        @Parameter(description = "페이지 크기", example = "20")
        @RequestParam(defaultValue = "20") int size
    ) {
        log.info("👤 업로더별 검색 요청: uploaderId={}, page={}, size={}", uploaderId, page, size);
        
        Page<ImageDocument> results = imageSearchService.searchByUploader(uploaderId, page, size);
        return ResponseEntity.ok(results);
    }

    @Operation(
        summary = "자동완성",
        description = "이미지명 기반 자동완성 제안"
    )
    @GetMapping("/autocomplete")
    public ResponseEntity<List<String>> autoComplete(
        @Parameter(description = "입력 중인 텍스트", required = true, example = "자연")
        @RequestParam String prefix,
        
        @Parameter(description = "제안 개수", example = "5")
        @RequestParam(defaultValue = "5") int size
    ) {
        log.info("🔍 자동완성 요청: prefix={}, size={}", prefix, size);
        
        List<String> results = imageSearchService.autoComplete(prefix, size);
        return ResponseEntity.ok(results);
    }
}

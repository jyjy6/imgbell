package ImgBell.Image.ElasticSearch;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/image/sync")
@Tag(name = "이미지 동기화", description = "Image 테이블과 ElasticSearch 동기화 관리 API")
@Slf4j
public class ImageSyncController {
    
    private final ImageSyncService imageSyncService;

    @Operation(
        summary = "전체 이미지 동기화",
        description = "Image 테이블의 모든 데이터를 ElasticSearch에 동기화합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "동기화 성공"),
        @ApiResponse(responseCode = "500", description = "동기화 실패")
    })
    @PostMapping("/all")
    public ResponseEntity<String> syncAllImages() {
        try {
            log.info("🚀 전체 이미지 동기화 요청");
            imageSyncService.syncAllImages();
            return ResponseEntity.ok("전체 이미지 동기화가 완료되었습니다.");
        } catch (Exception e) {
            log.error("❌ 전체 동기화 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body("동기화 실패: " + e.getMessage());
        }
    }

    @Operation(
        summary = "단일 이미지 동기화",
        description = "특정 이미지를 ElasticSearch에 동기화합니다."
    )
    @PostMapping("/image/{imageId}")
    public ResponseEntity<String> syncSingleImage(
        @Parameter(description = "동기화할 이미지 ID", required = true, example = "1")
        @PathVariable Long imageId
    ) {
        try {
            log.info("🔄 단일 이미지 동기화 요청: imageId={}", imageId);
            imageSyncService.syncSingleImage(imageId);
            return ResponseEntity.ok("이미지 동기화가 완료되었습니다.");
        } catch (Exception e) {
            log.error("❌ 단일 동기화 실패: imageId={}, error={}", imageId, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body("동기화 실패: " + e.getMessage());
        }
    }

    @Operation(
        summary = "태그별 이미지 동기화",
        description = "특정 태그와 관련된 모든 이미지를 재동기화합니다."
    )
    @PostMapping("/tag/{tagId}")
    public ResponseEntity<String> syncImagesByTag(
        @Parameter(description = "태그 ID", required = true, example = "1")
        @PathVariable Long tagId
    ) {
        try {
            log.info("🏷️ 태그별 이미지 동기화 요청: tagId={}", tagId);
            imageSyncService.syncImagesByTag(tagId);
            return ResponseEntity.ok("태그별 이미지 동기화가 완료되었습니다.");
        } catch (Exception e) {
            log.error("❌ 태그별 동기화 실패: tagId={}, error={}", tagId, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body("동기화 실패: " + e.getMessage());
        }
    }

    @Operation(
        summary = "이미지 인덱스에서 삭제",
        description = "ElasticSearch 인덱스에서 특정 이미지를 삭제합니다."
    )
    @DeleteMapping("/image/{imageId}")
    public ResponseEntity<String> deleteFromIndex(
        @Parameter(description = "삭제할 이미지 ID", required = true, example = "1")
        @PathVariable Long imageId
    ) {
        try {
            log.info("🗑️ 인덱스에서 이미지 삭제 요청: imageId={}", imageId);
            imageSyncService.deleteFromIndex(imageId);
            return ResponseEntity.ok("인덱스에서 이미지가 삭제되었습니다.");
        } catch (Exception e) {
            log.error("❌ 인덱스 삭제 실패: imageId={}, error={}", imageId, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body("삭제 실패: " + e.getMessage());
        }
    }

    @Operation(
        summary = "동기화 상태 확인",
        description = "Database와 ElasticSearch의 동기화 상태를 확인합니다."
    )
    @GetMapping("/status")
    public ResponseEntity<ImageSyncService.SyncStatus> getSyncStatus() {
        try {
            log.info("📊 동기화 상태 확인 요청");
            ImageSyncService.SyncStatus status = imageSyncService.getSyncStatus();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("❌ 상태 확인 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
} 
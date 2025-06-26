package ImgBell.Image.ElasticSearch;

import ImgBell.Image.Image;
import ImgBell.Image.ImageRepository;
import ImgBell.Image.Tag.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageSyncService {

    private final ImageRepository imageRepository;
    private final ImageSearchRepository imageSearchRepository;

    /**
     * 🔄 Image 엔티티를 ImageDocument로 변환
     */
    public ImageDocument convertToDocument(Image image) {
        ImageDocument document = new ImageDocument();
        
        // 기본 정보
        document.setId(image.getId().toString());
        document.setImageName(image.getImageName());
        document.setImageUrl(image.getImageUrl());
        document.setFileType(image.getFileType());
        
        // 업로더 정보
        if (image.getUploader() != null) {
            document.setUploaderId(image.getUploader().getId());
        }
        document.setUploaderName(image.getUploaderName());
        
        // 출처 정보
        document.setSource(image.getSource());
        document.setArtist(image.getArtist());
        
        // 통계 정보
        document.setViewCount(image.getViewCount());
        document.setLikeCount(image.getLikeCount());
        
        // 등급 및 공개 설정
        document.setImageGrade(image.getImageGrade() != null ? image.getImageGrade().name() : "GENERAL");
        document.setIsPublic(image.getIsPublic());
        
        // 타임스탬프
        document.setCreatedAt(image.getCreatedAt());
        document.setUpdatedAt(image.getUpdatedAt());
        
        // 태그 정보 변환
        if (image.getTags() != null && !image.getTags().isEmpty()) {
            List<ImageDocument.TagDocument> tagDocuments = image.getTags().stream()
                    .map(tag -> new ImageDocument.TagDocument(
                            tag.getId(),
                            tag.getName(),
                            tag.getCategory() != null ? tag.getCategory() : "GENERAL"
                    ))
                    .collect(Collectors.toList());
            document.setTags(tagDocuments);
            
            // 태그명만 별도로 저장
            List<String> tagNames = image.getTags().stream()
                    .map(Tag::getName)
                    .collect(Collectors.toList());
            document.setTagNames(tagNames);
        }
        
        // 검색 텍스트와 인기도 점수 생성
        document.generateSearchText();
        document.calculatePopularityScore();
        
        return document;
    }

    /**
     * 🚀 단일 이미지 동기화
     */
    @Transactional
    public void syncSingleImage(Long imageId) {
        try {
            Image image = imageRepository.findById(imageId)
                    .orElseThrow(() -> new RuntimeException("이미지를 찾을 수 없습니다: " + imageId));
            
            ImageDocument document = convertToDocument(image);
            imageSearchRepository.save(document);
            
            log.info("✅ 이미지 동기화 완료: id={}, name={}", imageId, image.getImageName());
        } catch (Exception e) {
            log.error("❌ 이미지 동기화 실패: id={}, error={}", imageId, e.getMessage());
        }
    }

    /**
     * 🔄 전체 이미지 일괄 동기화
     */
    @Transactional
    public void syncAllImages() {
        log.info("🚀 전체 이미지 동기화 시작");
        
        int pageSize = 100;
        int pageNumber = 0;
        int totalSynced = 0;
        
        try {
            // 기존 인덱스 클리어
            imageSearchRepository.deleteAll();
            log.info("🗑️ 기존 ElasticSearch 인덱스 클리어 완료");
            
            Page<Image> imagePage;
            do {
                imagePage = imageRepository.findAll(PageRequest.of(pageNumber, pageSize));
                
                List<ImageDocument> documents = imagePage.getContent().stream()
                        .map(this::convertToDocument)
                        .collect(Collectors.toList());
                
                if (!documents.isEmpty()) {
                    imageSearchRepository.saveAll(documents);
                    totalSynced += documents.size();
                    log.info("📦 배치 동기화 완료: {} ~ {} (총 {}개)", 
                            pageNumber * pageSize + 1, 
                            pageNumber * pageSize + documents.size(),
                            totalSynced);
                }
                
                pageNumber++;
            } while (imagePage.hasNext());
            
            log.info("🎉 전체 이미지 동기화 완료! 총 {}개 이미지 처리", totalSynced);
            
        } catch (Exception e) {
            log.error("❌ 전체 이미지 동기화 실패: {}", e.getMessage());
            throw new RuntimeException("동기화 실패", e);
        }
    }

    /**
     * 🏷️ 태그 업데이트 시 관련 이미지들 재동기화
     */
    @Transactional
    public void syncImagesByTag(Long tagId) {
        try {
            List<Image> images = imageRepository.findByTagsId(tagId);
            
            for (Image image : images) {
                ImageDocument document = convertToDocument(image);
                imageSearchRepository.save(document);
            }
            
            log.info("🏷️ 태그 관련 이미지 동기화 완료: tagId={}, 이미지 {}개", tagId, images.size());
        } catch (Exception e) {
            log.error("❌ 태그 관련 이미지 동기화 실패: tagId={}, error={}", tagId, e.getMessage());
        }
    }

    /**
     * 🗑️ 이미지 삭제 시 ElasticSearch에서도 제거
     */
    @Transactional
    public void deleteFromIndex(Long imageId) {
        try {
            imageSearchRepository.deleteById(imageId.toString());
            log.info("🗑️ ElasticSearch에서 이미지 삭제 완료: id={}", imageId);
        } catch (Exception e) {
            log.error("❌ ElasticSearch 이미지 삭제 실패: id={}, error={}", imageId, e.getMessage());
        }
    }

    /**
     * 📊 동기화 상태 확인
     */
    @Transactional(readOnly = true)
    public SyncStatus getSyncStatus() {
        try {
            long dbCount = imageRepository.count();
            long esCount = imageSearchRepository.count();
            
            return new SyncStatus(dbCount, esCount, dbCount == esCount);
        } catch (Exception e) {
            log.error("❌ 동기화 상태 확인 실패: {}", e.getMessage());
            return new SyncStatus(0, 0, false);
        }
    }

    /**
     * 동기화 상태 정보 클래스
     */
    public static class SyncStatus {
        public final long databaseCount;
        public final long elasticsearchCount;
        public final boolean inSync;
        
        public SyncStatus(long databaseCount, long elasticsearchCount, boolean inSync) {
            this.databaseCount = databaseCount;
            this.elasticsearchCount = elasticsearchCount;
            this.inSync = inSync;
        }
    }
} 
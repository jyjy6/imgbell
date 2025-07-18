package ImgBell.Image;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Arrays;
import java.util.List;

public interface ImageRepository extends JpaRepository<Image, Long>, JpaSpecificationExecutor<Image> {

    /**
     * 공개 및 승인된 이미지만 조회
     */
    Page<Image> findAllByIsPublicTrue(Pageable pageable);

    /**
     * 태그로 이미지 필터링
     */
    @Query("SELECT i FROM Image i JOIN i.tags t WHERE t.name = :tagName AND i.isPublic = true")
    Page<Image> findByTagName(@Param("tagName") String tagName, Pageable pageable);

    /**
     * 태그 ID로 이미지 조회 (ElasticSearch 동기화용)
     */
    @Query("SELECT i FROM Image i JOIN i.tags t WHERE t.id = :tagId")
    List<Image> findByTagsId(@Param("tagId") Long tagId);

    /**
     * 등급으로 이미지 필터링
     */
    Page<Image> findByImageGrade(Image.ImageGrade imageGrade, Pageable pageable);

    /**
     * 인기순(조회수) 이미지 조회
     */
    Page<Image> findAllByOrderByViewCountDesc(Pageable pageable);

    List<Image> findTop5ByOrderByCreatedAtDesc();
}

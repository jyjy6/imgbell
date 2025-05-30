package ImgBell.Forum;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ForumRepository extends JpaRepository<Forum, Long> {

    Page<Forum> findByTypeAndIsDeletedFalse(Forum.PostType type, Pageable pageable);

    // 네이티브 SQL 쿼리 사용
    @Query(value = "SELECT * FROM forum f WHERE f.is_deleted = false AND " +
           "(LOWER(f.title) LIKE LOWER(:keyword) OR " +
           "LOWER(f.content) LIKE LOWER(:keyword)) " +
           "ORDER BY f.id DESC", 
           countQuery = "SELECT COUNT(*) FROM forum f WHERE f.is_deleted = false AND " +
           "(LOWER(f.title) LIKE LOWER(:keyword) OR " +
           "LOWER(f.content) LIKE LOWER(:keyword))",
           nativeQuery = true)
    Page<Forum> findByKeywordAndIsDeletedFalse(@Param("keyword") String keyword, Pageable pageable);

    List<Forum> findTop5ByOrderByCreatedAtDesc();
}

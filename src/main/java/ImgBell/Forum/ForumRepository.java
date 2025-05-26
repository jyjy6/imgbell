package ImgBell.Forum;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ForumRepository extends JpaRepository<Forum, Long> {

    Page<Forum> findByTypeAndIsDeletedFalse(Forum.PostType type, Pageable pageable);


}

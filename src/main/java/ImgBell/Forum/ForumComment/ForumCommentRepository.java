package ImgBell.Forum.ForumComment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ForumCommentRepository extends JpaRepository<ForumComment, Long> {

    List<ForumComment> findByForumId(Long forumId);

}

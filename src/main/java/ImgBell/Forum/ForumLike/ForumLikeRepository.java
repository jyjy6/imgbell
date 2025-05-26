package ImgBell.Forum.ForumLike;

import ImgBell.Forum.Forum;
import ImgBell.Member.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ForumLikeRepository extends JpaRepository<ForumLike, Long> {

    Optional<ForumLike> findByMemberAndForum(Member member, Forum forum);

    List<ForumLike> findAllByMember(Member member);

}

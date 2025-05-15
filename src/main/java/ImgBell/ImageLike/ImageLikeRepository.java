package ImgBell.ImageLike;

import ImgBell.Image.Image;
import ImgBell.Member.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ImageLikeRepository extends JpaRepository<ImageLike, Long> {
    boolean existsByMemberAndImage(Member member, Image image);

    Optional<ImageLike> findByMemberAndImage(Member member, Image image);

    List<ImageLike> findAllByMember(Member member);
}

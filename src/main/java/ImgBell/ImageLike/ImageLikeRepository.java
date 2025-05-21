package ImgBell.ImageLike;

import ImgBell.Image.Image;
import ImgBell.Member.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ImageLikeRepository extends JpaRepository<ImageLike, Long> {
    boolean existsByMemberAndImage(Member member, Image image);

    Optional<ImageLike> findByMemberAndImage(Member member, Image image);

    List<ImageLike> findAllByMember(Member member);

    @Query("SELECT il.image.id FROM ImageLike il WHERE il.member.id = :memberId")
    List<Long> findLikedImageIdsByMemberId(@Param("memberId") Long memberId);
}

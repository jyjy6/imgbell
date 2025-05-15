package ImgBell.ImageLike;

import ImgBell.Image.Image;
import ImgBell.Member.Member;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"member_id", "image_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 좋아요 누른 회원
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 좋아요 누른 상품
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id", nullable = false)
    private Image image;

}
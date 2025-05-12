package ImgBell.Image.Comment;

import ImgBell.Image.Image;
import ImgBell.Member.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    // 작성자 이름을 별도로 저장 (회원 삭제 후에도 표시 가능)
    private String authorName;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id")
    private Image image;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}

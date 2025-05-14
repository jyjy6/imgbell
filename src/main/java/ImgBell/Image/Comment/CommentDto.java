package ImgBell.Image.Comment;

import ImgBell.Image.Image;
import ImgBell.Member.Member;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommentDto {
    private Long id;
    private Member member;
    private String authorName;
    private Image image;
    private LocalDateTime createdAt;
}

package ImgBell.Image;

import ImgBell.Image.Comment.Comment;
import ImgBell.Image.Tag.Tag;
import ImgBell.Image.Tag.TagDto;
import ImgBell.Member.Member;
import lombok.*;

import java.util.List;
import java.util.Set;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ImageRequestDto {
    private String imageUrl;
    private String imageName;
    private String uploaderName;
    private Member uploader;
    private String fileType;
    private Long fileSize;
    private List<TagDto> tags;
    private String source;
    private String artist;
    private Integer viewCount;
    private Integer likeCount;
    private Integer downloadCount;
    private Image.ImageGrade imageGrade;
    private Boolean isPublic = true;
    private Boolean isApproved;
    private List<Comment> comments;


}



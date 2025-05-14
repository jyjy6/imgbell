package ImgBell.Image;

import ImgBell.Image.Comment.Comment;
import ImgBell.Image.Comment.CommentDto;
import ImgBell.Image.Tag.TagDto;
import ImgBell.Member.Member;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

//null필드 응답하지않게
@JsonInclude(JsonInclude.Include.NON_NULL)
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageDto {
    private Long id;
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
    private List<CommentDto> comments;


}



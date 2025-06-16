package ImgBell.Image;

import ImgBell.Image.Comment.Comment;
import ImgBell.Image.Comment.CommentDto;
import ImgBell.Image.Tag.TagDto;
import ImgBell.Member.Member;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

//null필드 응답하지않게
@JsonInclude(JsonInclude.Include.NON_NULL)
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "이미지 정보 DTO")
public class ImageDto {
    
    @Schema(description = "이미지 ID", example = "1")
    private Long id;
    
    @Schema(description = "이미지 URL", example = "https://bucket.s3.amazonaws.com/images/uuid_image.jpg")
    private String imageUrl;
    
    @Schema(description = "이미지 이름", example = "아름다운 풍경")
    private String imageName;
    
    @Schema(description = "업로더 이름", example = "홍길동")
    private String uploaderName;
    
    @Schema(description = "업로더 정보")
    private Member uploader;
    
    @Schema(description = "파일 타입", example = "image/jpeg")
    private String fileType;
    
    @Schema(description = "파일 크기 (바이트)", example = "1048576")
    private Long fileSize;
    
    @Schema(description = "이미지 태그 리스트")
    private List<TagDto> tags;
    
    @Schema(description = "이미지 출처", example = "https://example.com")
    private String source;
    
    @Schema(description = "아티스트/작가명", example = "김작가")
    private String artist;
    
    @Schema(description = "조회수", example = "100")
    private Integer viewCount;
    
    @Schema(description = "좋아요 수", example = "25")
    private Integer likeCount;
    
    @Schema(description = "다운로드 수", example = "10")
    private Integer downloadCount;
    
    @Schema(description = "이미지 등급", example = "A", allowableValues = {"S", "A", "B", "C", "D"})
    private Image.ImageGrade imageGrade;
    
    @Schema(description = "공개 여부", example = "true")
    private Boolean isPublic = true;
    
    @Schema(description = "승인 여부", example = "true")
    private Boolean isApproved;
    
    @Schema(description = "댓글 리스트")
    private List<CommentDto> comments;
}



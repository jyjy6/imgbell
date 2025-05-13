package ImgBell.Image;

import ImgBell.Image.Tag.Tag;
import ImgBell.Image.Tag.TagDto;
import ImgBell.Image.Tag.TagRepository;
import ImgBell.Image.Tag.TagService;
import ImgBell.Member.Member;
import ImgBell.Member.MemberRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImageService {
    private final ImageRepository imageRepository;
    private final MemberRepository memberRepository;
    private final TagRepository tagRepository;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;
    private final S3Presigner s3Presigner;
    String createPresignedUrl(String path) {

        var putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket) //올릴 버킷명
                .key(path) //경로
                .build();
        var preSignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(60)) //URL 유효기간
                .putObjectRequest(putObjectRequest)
                .build();

        String presignedUrl = s3Presigner.presignPutObject(preSignRequest).url().toString();

        return presignedUrl;
    }


    @Transactional
    public void saveFileInfoToDb(List<ImageRequestDto> imageDtos) {
        for (ImageRequestDto dto : imageDtos) {
            try {
                Image image = Image.builder()
                        .imageUrl(dto.getImageUrl())
                        .imageName(dto.getImageName())
                        .source(dto.getSource())
                        .artist(dto.getArtist())
                        .fileSize(dto.getFileSize())
                        .fileType(dto.getFileType())
                        .imageGrade(dto.getImageGrade())
                        .isPublic(dto.getIsPublic())
                        .uploaderName(dto.getUploaderName())
                        .build();

                // uploader 설정
                if (!"GUEST".equals(dto.getUploaderName())) {
                    Member uploader = memberRepository.findByUsername(dto.getUploaderName())
                            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + dto.getUploaderName()));
                    image.setUploader(uploader);
                }


                Set<Tag> tagEntities = new HashSet<>();
                for (TagDto tagDto : dto.getTags()) {
                    Tag tag = tagRepository.findByName(tagDto.getName())
                            .orElseGet(() -> {
                                Tag newTag = new Tag(tagDto.getName());
                                newTag.setDescription(tagDto.getDescription());
                                newTag.setCategory(tagDto.getCategory());
                                return tagRepository.save(newTag);
                            });
                    tag.setUsageCount(tag.getUsageCount() + 1);
                    tagEntities.add(tag);
                }
                image.setTags(tagEntities);

                imageRepository.save(image);
            } catch (Exception e) {
                System.out.println("이미지 저장 오류남: " + e.getMessage());
            }
        }
    }


    /**
     * URL에서 파일 타입 추출
     */
    private String extractFileTypeFromUrl(String url) {
        // URL 형태: https://bucket-name.s3.amazonaws.com/image/filename
        String prefix = "https://" + bucket + ".s3.amazonaws.com/";

        if (url.startsWith(prefix)) {
            String path = url.substring(prefix.length());
            // 첫 번째 '/' 이전의 문자열이 폴더명(=파일 타입)
            int slashIndex = path.indexOf('/');
            if (slashIndex > 0) {
                return path.substring(0, slashIndex);
            }
        }
        // 기본값
        return "other";
    }

}

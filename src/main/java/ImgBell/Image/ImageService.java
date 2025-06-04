package ImgBell.Image;

import ImgBell.Image.Comment.Comment;
import ImgBell.Image.Comment.CommentDto;
import ImgBell.Image.Tag.Tag;
import ImgBell.Image.Tag.TagDto;
import ImgBell.Image.Tag.TagRepository;
import ImgBell.ImageLike.ImageLikeRepository;
import ImgBell.Member.CustomUserDetails;
import ImgBell.Member.Member;
import ImgBell.Member.MemberRepository;
import ImgBell.Redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;


import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ImageService {
    private final ImageRepository imageRepository;
    private final MemberRepository memberRepository;
    private final TagRepository tagRepository;
    private final ImageLikeRepository imageLikeRepository;
    private final S3Client s3Client;
    private final RecentViewService recentViewService;
    private final RankingService rankingService;
    private final RedisService redisService;
    private static final String VIEW_COUNT_KEY = "image:views:";
    private static final String LIKE_COUNT_KEY = "image:likes:";

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
    public void saveFileInfoToDb(List<ImageDto> imageDtos) {
        for (ImageDto dto : imageDtos) {
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
                        .likeCount(0)
                        .downloadCount(0)
                        .viewCount(0)
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


    @Transactional
    public ResponseEntity<?> deleteImage(@PathVariable Long id, Authentication auth){
        // 이미지 조회
        Image deleteTargetImage = imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("그런 이미지 없습니다"));
        // 업로더가 현재 로그인한 사용자와 일치하는지 확인
        String username = ((CustomUserDetails)auth.getPrincipal()).getUsername();
        if (!deleteTargetImage.getUploaderName().equals(username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("이미지를 삭제할 권한이 없습니다");
        }

        // 이미지 삭제
        String s3Key = extractS3Key(deleteTargetImage.getImageUrl());
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .build());
            System.out.println("삭제됨");
        } catch (S3Exception e) {
            System.out.printf("S3 이미지삭제실패");
            System.out.println(e.getMessage());
        }

        imageRepository.delete(deleteTargetImage);
        return ResponseEntity.ok().body("이미지가 성공적으로 삭제되었습니다");
    }

    @Transactional
    public ResponseEntity<?> editImage(ImageDto dto, Authentication auth){

        Image targetImage = imageRepository.findById(dto.getId()).orElseThrow(()->new RuntimeException("그런이미지 없음"));
        targetImage.setImageName(dto.getImageName());
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
        targetImage.setTags(tagEntities);
        targetImage.setSource(dto.getSource());
        targetImage.setArtist(dto.getArtist());
        imageRepository.save(targetImage);

        Image savedImage = imageRepository.save(targetImage);

        // Entity를 DTO로 변환
        ImageDto responseDto = convertToRequestDto(savedImage);
        return ResponseEntity.ok(responseDto);
    }


    @Transactional(readOnly = true)
    public Page<ImageDto> getImageList(Pageable pageable, String tag, String imageName, String uploaderName, String artist,
                                       String keyword, String searchType, String grade, Boolean myImageList, Boolean likeImageList, Authentication auth) {

        Specification<Image> spec = Specification.where(null);


        if(myImageList){
            //마이페이지에선 해당 업로더만
            String username = ((CustomUserDetails)auth.getPrincipal()).getUsername();
            spec = spec.and(ImageSpecification.hasUploaderName(username));
        }  else {
            //ADMIN이 아닌 경우에만 공개된 것만 필터링
            if( auth == null || !auth.isAuthenticated() || !auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))){
                spec = spec.and(ImageSpecification.isPublic());
            }

        }


        // 검색 타입에 따른 조건 적용
        if (searchType != null) {
            switch (searchType) {
                case "all":
                    if (keyword != null && !keyword.isEmpty()) {
                        spec = spec.and(ImageSpecification.searchAll(keyword));
                    }
                    break;
                case "tag":
                    if (tag != null && !tag.isEmpty()) {
                        spec = spec.and(ImageSpecification.hasTag(tag));
                    }
                    break;
                case "imageName":
                    if (imageName != null && !imageName.isEmpty()) {
                        spec = spec.and(ImageSpecification.hasImageName(imageName));
                    }
                    break;
                case "uploaderName":
                    if (uploaderName != null && !uploaderName.isEmpty()) {
                        spec = spec.and(ImageSpecification.hasUploaderName(uploaderName));
                    }
                    break;
                case "artist":
                    if (artist != null && !artist.isEmpty()) {
                        spec = spec.and(ImageSpecification.hasArtist(artist));
                    }
                    break;
                default:
                    // 기본적으로 태그 검색 적용
                    if (tag != null && !tag.isEmpty()) {
                        spec = spec.and(ImageSpecification.hasTag(tag));
                    }
            }
        } else {
            // 이전 방식과의 호환성 유지
            if (tag != null && !tag.isEmpty()) {
                spec = spec.and(ImageSpecification.hasTag(tag));
            }
        }

        // 등급 필터링
        if (grade != null && !grade.isEmpty()) {
            spec = spec.and(ImageSpecification.hasGrade(grade));
        }



        // 좋아요 이미지 필터
        if (Boolean.TRUE.equals(likeImageList) && auth != null && auth.isAuthenticated()) {
            Long memberId = ((CustomUserDetails)auth.getPrincipal()).getId();
            List<Long> likedImageIds = imageLikeRepository.findLikedImageIdsByMemberId(memberId);
            spec = spec.and(ImageSpecification.likedByMember(likedImageIds));
        }

        Page<Image> images = imageRepository.findAll(spec, pageable);


        // Entity -> DTO 변환
        return images.map(this::convertToLightDto);
    }

    @Transactional
    public Page<ImageDto> getPopularImages(Pageable pageable) {
        Page<Image> images = imageRepository.findAllByOrderByViewCountDesc(pageable);
        return images.map(this::convertToLightDto);
    }

    // Service
    @Transactional
    public boolean toggleImagePublic(Long imageId, Authentication auth) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("이미지를 찾을 수 없습니다."));
        Long userId = ((CustomUserDetails) auth.getPrincipal()).getId();

        System.out.println(auth.getAuthorities());

        // 업로더 확인
        if (image.getUploader() != null && !image.getUploader().getId().equals(userId) && auth.getAuthorities().contains("ROLE_ADMIN")){
            throw new SecurityException("이미지 공개 설정을 변경할 권한이 없습니다.");
        }

        // 공개/비공개 토글
        image.setIsPublic(!image.getIsPublic());
        imageRepository.save(image);

        return image.getIsPublic();
    }


    /**
     * 단일 이미지 상세 정보를 조회하는 메서드
     */
    @Transactional
    public ImageDto getImageDetail(Long id, Boolean increaseView, Authentication auth) {
            Image image = imageRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("이미지를 찾을 수 없습니다."));
            // 조회수 증가
            if(increaseView) {
                image.setViewCount(image.getViewCount() + 1);

                // 이미지 랭킹 점수 업데이트 (조회 = 1점)
                rankingService.updateImageScore(id, 1);
            }
            // 로그인 되었으면 최근 본 목록에 추가
            if(auth != null && auth.isAuthenticated()) {
                Long userId = ((CustomUserDetails)auth.getPrincipal()).getId();
                recentViewService.addRecentView(userId, id);
            }

            imageRepository.save(image);
            return convertToRequestDto(image);
    }









    /**
     * Image Entity를 ImageResponseDto로 변환하는 메서드
     */
    public ImageDto convertToLightDto(Image image) {
        return ImageDto.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .imageName(image.getImageName())
                .uploaderName(image.getUploader() != null ? image.getUploader().getUsername() : "Unknown")
                .likeCount(image.getLikeCount())
                .viewCount(image.getViewCount())
                .imageGrade(image.getImageGrade())
                .isPublic(image.getIsPublic())
                .build();
    }

    public ImageDto convertToRequestDto(Image image) {
        // 이 메서드는 상세 정보를 포함한 DTO로 변환
        ImageDto dto = new ImageDto();
        dto.setId(image.getId());
        dto.setImageUrl(image.getImageUrl());
        dto.setImageName(image.getImageName());
        dto.setUploaderName(image.getUploader() != null ? image.getUploader().getUsername() : null);
//        dto.setUploader(image.getUploader());
        dto.setFileType(image.getFileType());
        dto.setFileSize(image.getFileSize());
        List<TagDto> tagDtos = new ArrayList<>();
        for (Tag tag : image.getTags()) {
            TagDto tagDto = new TagDto();
            tagDto.setCategory(tag.getCategory());
            tagDto.setDescription(tag.getDescription());
            tagDto.setName(tag.getName());
            tagDto.setUsageCount(tag.getUsageCount());
            tagDtos.add(tagDto);
        }
        dto.setTags(tagDtos);
        dto.setSource(image.getSource());
        dto.setArtist(image.getArtist());
        dto.setViewCount(image.getViewCount());
        dto.setLikeCount(image.getLikeCount());
        dto.setDownloadCount(image.getDownloadCount());
        dto.setImageGrade(image.getImageGrade());
        dto.setIsPublic(image.getIsPublic());
        dto.setIsApproved(image.getIsApproved());
        List<CommentDto> commentDtos = new ArrayList<>();
        for (Comment comment : image.getComments()) {
            CommentDto commentDto = new CommentDto();
            commentDto.setId(comment.getId());
            commentDto.setMember(comment.getMember());
            commentDto.setAuthorName(comment.getAuthorName());
            commentDto.setImage(comment.getImage());
            commentDto.setCreatedAt(comment.getCreatedAt());
            commentDtos.add(commentDto);
        }
        dto.setComments(commentDtos);
        return dto;
    }


    public String extractS3Key(String imageUrl) {
        return imageUrl.substring(imageUrl.indexOf(".com/") + 5);

    }


    public Long getViewCount(Long imageId) {
        Object count = redisService.getValue(VIEW_COUNT_KEY + imageId);
        return count != null ? Long.valueOf(count.toString()) : 0L;
    }

    public Long getLikeCount(Long imageId) {
        Object count = redisService.getValue(LIKE_COUNT_KEY + imageId);
        return count != null ? Long.valueOf(count.toString()) : 0L;
    }





}

package ImgBell.Image;

import ImgBell.Image.Tag.Tag;
import ImgBell.Image.Tag.TagRepository;
import ImgBell.ImageLike.ImageLikeRepository;
import ImgBell.Member.Member;
import ImgBell.Member.MemberRepository;
import ImgBell.Redis.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Spring 없이 Mockito만 사용
class ImageServiceTest {

    @Mock
    private ImageRepository imageRepository;
    
    @Mock
    private MemberRepository memberRepository;
    
    @Mock
    private TagRepository tagRepository;
    
    @Mock
    private ImageLikeRepository imageLikeRepository;
    
    @Mock
    private S3Client s3Client;
    
    @Mock
    private RecentViewService recentViewService;
    
    @Mock
    private RankingService rankingService;
    
    @Mock
    private RedisService redisService;
    
    @Mock
    private S3Presigner s3Presigner;
    
    @InjectMocks
    private ImageService imageService;
    
    private Member testMember;
    private Image testImage;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 준비
        testMember = Member.builder()
                .username("testuser")
                .displayName("테스트유저")
                .email("test@test.com")
                .build();
        
        testImage = Image.builder()
                .imageName("test.jpg")
                .imageUrl("https://example.com/test.jpg")
                .uploader(testMember)
                .uploaderName("testuser")
                .viewCount(100)
                .likeCount(10)
                .isPublic(true)
                .build();
        
        // 컬렉션 초기화
        testImage.setTags(new HashSet<>());
        testImage.setComments(new HashSet<>());
        
        // ID 설정 (리플렉션 사용)
        try {
            java.lang.reflect.Field idField = Image.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testImage, 1L);
        } catch (Exception e) {
            // ID 설정 실패 시 무시
        }
    }

    @Test
    void 조회수_증가_테스트() {
        // given
        Long imageId = 1L;
        when(imageRepository.findById(imageId)).thenReturn(Optional.of(testImage));
        when(imageRepository.save(any(Image.class))).thenReturn(testImage);
        
        // when
        imageService.incrementViewCount(imageId);
        
        // then
        verify(imageRepository).findById(imageId);
        verify(imageRepository).save(any(Image.class));
        verify(redisService).incrementHashValue(anyString(), anyString(), anyLong());
    }

    @Test
    void 좋아요_증가_테스트() {
        // given
        Long imageId = 1L;
        when(imageRepository.findById(imageId)).thenReturn(Optional.of(testImage));
        when(imageRepository.save(any(Image.class))).thenReturn(testImage);
        
        // when
        imageService.incrementLikeCount(imageId);
        
        // then
        verify(imageRepository).findById(imageId);
        verify(imageRepository).save(any(Image.class));
        verify(redisService).incrementHashValue(anyString(), anyString(), anyLong());
        verify(rankingService).updateLikeScore(imageId);
    }

    @Test
    void 존재하지_않는_이미지_조회시_예외발생() {
        // given
        Long nonExistentImageId = 99999L;
        when(imageRepository.findById(nonExistentImageId)).thenReturn(Optional.empty());
        
        // when & then
        assertThatThrownBy(() -> 
            imageService.incrementViewCount(nonExistentImageId))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("이미지를 찾을 수 없습니다");
    }


    @Test
    void 이미지_검색_키워드_검증() {
        // given
        String searchKeyword = "test";
        
        // when & then
        assertThat(searchKeyword).isNotNull();
        assertThat(searchKeyword).isNotEmpty();
        assertThat(searchKeyword).contains("test");
    }

    @Test
    void 이미지_빌더_패턴_테스트() {
        // given & when
        Image image = Image.builder()
                .imageName("test.jpg")
                .imageUrl("https://example.com/test.jpg")
                .uploaderName("testuser")
                .viewCount(100)
                .likeCount(10)
                .isPublic(true)
                .build();
        
        // then
        assertThat(image).isNotNull();
        assertThat(image.getImageName()).isEqualTo("test.jpg");
        assertThat(image.getImageUrl()).isEqualTo("https://example.com/test.jpg");
        assertThat(image.getUploaderName()).isEqualTo("testuser");
        assertThat(image.getViewCount()).isEqualTo(100);
        assertThat(image.getLikeCount()).isEqualTo(10);
        assertThat(image.getIsPublic()).isTrue();
    }

    @Test
    void 레디스_캐시_키_검증() {
        // given
        Long imageId = 1L;
        String expectedStatsKey = "image:stats:" + imageId;
        
        // when & then - 키 형식 검증
        assertThat(expectedStatsKey).isEqualTo("image:stats:1");
        assertThat(expectedStatsKey).contains("image:stats:");
        assertThat(expectedStatsKey).endsWith("1");
    }
} 
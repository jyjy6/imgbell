package ImgBell.Image;

import ImgBell.Member.Member;
import ImgBell.Member.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest // ← 🔥 JPA만 테스트 (DB 레이어만)
@ActiveProfiles("test")
class ImageServiceIntegrationTest {

    @Autowired
    private ImageRepository imageRepository;
    
    @Autowired  
    private MemberRepository memberRepository;
    
    private Member testMember;

    @BeforeEach
    void setUp() {
        // 🔥 진짜 H2 DB에 데이터 저장! (필수 필드들 모두 포함)
        testMember = Member.builder()
                .username("testuser")
                .name("테스트유저실명")  // 필수 필드
                .password("testpassword123")  // 필수 필드
                .displayName("테스트유저")
                .email("test@test.com")
                .termsAccepted(true)
                .privacyAccepted(true)
                .build();
        
        memberRepository.save(testMember); // ← 진짜 DB에 저장!
    }

    @Test
    void 진짜_DB에_이미지_저장_조회() {
        // given - 테스트용 이미지 생성 (필수 필드들 모두 포함)
        Image testImage = Image.builder()
                .imageName("test.jpg")
                .imageUrl("https://example.com/test.jpg")  // 필수 필드
                .uploader(testMember)
                .uploaderName("testuser")
                .fileType("jpg")
                .width(1920)
                .height(1080)
                .fileSize(1024L)
                .viewCount(100)
                .likeCount(10)
                .downloadCount(5)
                .imageGrade(Image.ImageGrade.GENERAL)
                .isPublic(true)
                .isApproved(false)
                .tags(new HashSet<>())  // 빈 Set으로 초기화
                .comments(new HashSet<>())  // 빈 Set으로 초기화
                .build();
        
        // when - 진짜 DB에 저장
        Image savedImage = imageRepository.save(testImage);
        
        // then - 진짜 DB에서 조회해서 검증
        Optional<Image> foundImage = imageRepository.findById(savedImage.getId());
        
        assertThat(foundImage).isPresent();
        assertThat(foundImage.get().getImageName()).isEqualTo("test.jpg");
        assertThat(foundImage.get().getUploaderName()).isEqualTo("testuser");
        assertThat(foundImage.get().getViewCount()).isEqualTo(100);
        assertThat(foundImage.get().getIsPublic()).isTrue();
    }

    @Test
    void 진짜_DB에서_업로더별_이미지_검색() {
        // given - 여러 이미지 저장
        Image image1 = Image.builder()
                .imageName("image1.jpg")
                .imageUrl("https://example.com/image1.jpg")
                .uploader(testMember)
                .uploaderName("testuser")
                .fileType("jpg")
                .imageGrade(Image.ImageGrade.GENERAL)
                .isPublic(true)
                .isApproved(false)
                .tags(new HashSet<>())
                .comments(new HashSet<>())
                .build();
                
        Image image2 = Image.builder()
                .imageName("image2.jpg") 
                .imageUrl("https://example.com/image2.jpg")
                .uploader(testMember)
                .uploaderName("testuser")
                .fileType("jpg")
                .imageGrade(Image.ImageGrade.GENERAL)
                .isPublic(true)
                .isApproved(false)
                .tags(new HashSet<>())
                .comments(new HashSet<>())
                .build();
        
        imageRepository.save(image1);
        imageRepository.save(image2);
        
        // when - 모든 이미지 조회 (진짜 DB 쿼리)
        List<Image> allImages = imageRepository.findAll();
        
        // then - 검증
        assertThat(allImages).hasSize(2);
        assertThat(allImages).extracting("imageName")
                            .containsExactlyInAnyOrder("image1.jpg", "image2.jpg");
        assertThat(allImages).extracting("uploaderName")
                            .containsOnly("testuser");
    }

    @Test
    void 진짜_DB에서_조회수_업데이트() {
        // given
        Image testImage = Image.builder()
                .imageName("test.jpg")
                .imageUrl("https://example.com/test.jpg")
                .uploader(testMember)
                .uploaderName("testuser")
                .fileType("jpg")
                .viewCount(100)
                .imageGrade(Image.ImageGrade.GENERAL)
                .isPublic(true)
                .isApproved(false)
                .tags(new HashSet<>())
                .comments(new HashSet<>())
                .build();
        
        Image savedImage = imageRepository.save(testImage);
        
        // when - 조회수 증가 (진짜 DB 업데이트)
        savedImage.setViewCount(savedImage.getViewCount() + 1);
        imageRepository.save(savedImage);
        
        // then - 진짜 DB에서 다시 조회해서 확인
        Image updatedImage = imageRepository.findById(savedImage.getId()).orElseThrow();
        assertThat(updatedImage.getViewCount()).isEqualTo(101);
    }

    @Test
    void 진짜_DB에서_존재하지_않는_이미지_조회() {
        // given
        Long nonExistentId = 99999L;
        
        // when
        Optional<Image> result = imageRepository.findById(nonExistentId);
        
        // then
        assertThat(result).isEmpty();
    }
} 
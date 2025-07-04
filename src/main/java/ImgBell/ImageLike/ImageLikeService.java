package ImgBell.ImageLike;

import ImgBell.Image.*;
import ImgBell.Member.Member;
import ImgBell.Member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImageLikeService {

    private final MemberRepository memberRepository;
    private final ImageRepository imageRepository;
    private final ImageLikeRepository imageLikeRepository;
    private final ImageService imageService;

    public void likeProduct(Long memberId, Long imageId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원 없음"));
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("이미지 없음"));

        // 중복 좋아요 체크
        Optional<ImageLike> existingLike = imageLikeRepository.findByMemberAndImage(member, image);

        if (existingLike.isPresent()) {
            // 이미 좋아요 누름 → 취소
            System.out.println("좋아요취소");
            imageLikeRepository.delete(existingLike.get());
            
            // ✅ ImageService의 통합 메서드 사용 (DB + Redis + 랭킹 한번에 처리)
            imageService.decrementLikeCount(imageId);

        } else {
            // 좋아요 등록
            System.out.println("좋아용");
            ImageLike like = ImageLike.builder()
                    .member(member)
                    .image(image)
                    .build();
            imageLikeRepository.save(like);
            
            // ✅ ImageService의 통합 메서드 사용 (DB + Redis + 랭킹 한번에 처리)
            imageService.incrementLikeCount(imageId);
        }
        
        // ❌ 불필요한 save 제거 (increment/decrementLikeCount에서 이미 처리함)
        // imageRepository.save(targetImage);
    }

    public List<ImageDto> getLikedProducts(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원 없음"));

        List<ImageLike> likes = imageLikeRepository.findAllByMember(member);

        return likes.stream()
                .map(ImageLike::getImage)
                .map(imageService::convertToLightDto)
                .collect(Collectors.toList());
    }
}

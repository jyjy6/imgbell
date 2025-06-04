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
    private final RankingService rankingService;


    public void likeProduct(Long memberId, Long imageId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원 없음"));
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("이미지 없음"));

        // 중복 좋아요 체크
        Optional<ImageLike> existingLike = imageLikeRepository.findByMemberAndImage(member, image);
        Image targetImage = imageRepository.findById(imageId).orElseThrow(()->new RuntimeException("그런이미지 없음"));

        if (existingLike.isPresent()) {
            // 이미 좋아요 누름 → 취소
            System.out.println("좋아요취소");
            imageLikeRepository.delete(existingLike.get());
            // 좋아요 개수 감소
            targetImage.setLikeCount(targetImage.getLikeCount()-1);

            // 이미지 랭킹 점수 업데이트 (좋아요취소 = -5점)
            rankingService.updateImageScore(imageId, -5);

        } else {
            // 좋아요 등록
            System.out.println("좋아용");
            ImageLike like = ImageLike.builder()
                    .member(member)
                    .image(image)
                    .build();
            imageLikeRepository.save(like);
            //좋아요 개수 증가
            targetImage.setLikeCount(targetImage.getLikeCount()+1);

            // 이미지 랭킹 점수 업데이트 (좋아요 = 5점)
            rankingService.updateImageScore(imageId, 5);

        }
        imageRepository.save(targetImage);
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

package ImgBell.Forum.ForumLike;

import ImgBell.Forum.*;
import ImgBell.Image.Image;
import ImgBell.Image.ImageDto;
import ImgBell.ImageLike.ImageLike;
import ImgBell.Member.Member;
import ImgBell.Member.MemberRepository;
import ImgBell.GlobalErrorHandler.GlobalException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ForumLikeService {
    private final MemberRepository memberRepository;
    private final ForumRepository forumRepository;
    private final ForumLikeRepository forumLikeRepository;
    private final ForumService forumService;

    public void likeForum(Long memberId, Long forumId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GlobalException("회원을 찾을 수 없습니다", "MEMBER_NOT_FOUND", HttpStatus.NOT_FOUND));
        Forum forum = forumRepository.findById(forumId)
                .orElseThrow(() -> new GlobalException("게시글을 찾을 수 없습니다", "FORUM_NOT_FOUND", HttpStatus.NOT_FOUND));

        // 중복 좋아요 체크
        Optional<ForumLike> existingLike = forumLikeRepository.findByMemberAndForum(member, forum);

        if (existingLike.isPresent()) {
            // 이미 좋아요 누름 → 취소
            System.out.println("좋아요 취소");
            forumLikeRepository.delete(existingLike.get());
            
            // ✅ ForumService의 통합 메서드 사용 (DB + Redis + 랭킹 한번에 처리)
            forumService.decrementLikeCount(forumId);

        } else {
            // 좋아요 등록
            System.out.println("좋아용");
            ForumLike like = ForumLike.builder()
                    .member(member)
                    .forum(forum)
                    .build();
            forumLikeRepository.save(like);
            
            // ✅ ForumService의 통합 메서드 사용 (DB + Redis + 랭킹 한번에 처리)
            forumService.incrementLikeCount(forumId);
        }
        
        // ❌ 불필요한 save 제거 (increment/decrementLikeCount에서 이미 처리함)
        // forumRepository.save(targetForum);
    }

    public List<ForumResponse> getLikedForum(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GlobalException("회원을 찾을 수 없습니다", "MEMBER_NOT_FOUND", HttpStatus.NOT_FOUND));

        List<ForumLike> likes = forumLikeRepository.findAllByMember(member);

        return likes.stream()
                .map(ForumLike::getForum)
                .map(ForumResponse::forList)
                .collect(Collectors.toList());
    }
}

package ImgBell.Admin;

import ImgBell.Forum.Forum;
import ImgBell.Forum.ForumRepository;
import ImgBell.Image.Image;
import ImgBell.Image.ImageRepository;
import ImgBell.Member.CustomUserDetails;
import ImgBell.Member.Dto.MemberDto;
import ImgBell.Member.Dto.MemberFormDto;
import ImgBell.Member.Member;
import ImgBell.Member.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Service
public class AdminService {
    private final MemberRepository memberRepository;
    private final ImageRepository imageRepository;
    private final ForumRepository forumRepository;

    // 통계 및 최근 데이터 반환
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardData() {
        try {
            log.info("관리자 대시보드 데이터 조회 시작");
            
            Map<String, Object> result = new HashMap<>();

            // 🎯 통계 데이터 - 간단하게 처리
            Map<String, Object> stats = new HashMap<>();
            stats.put("userCount", memberRepository.count());
            stats.put("imageCount", imageRepository.count());
            
            // 오늘 방문자 (오늘 가입자 수로 대체)
            LocalDate today = LocalDate.now();
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
            stats.put("todayVisit", memberRepository.countByCreatedAtBetween(startOfDay, endOfDay));
            
            // 신고/문의 (실제 테이블 있으면 수정)
            stats.put("reportCount", 0);
            
            result.put("stats", stats);

            // 🎯 최근 데이터들 - 간단하게 처리
            result.put("recentUsers", getRecentUsersData());
            result.put("recentImages", getRecentImagesData());
            result.put("recentPosts", getRecentPostsData());

            log.info("관리자 대시보드 데이터 조회 완료");
            return result;
            
        } catch (Exception e) {
            log.error("대시보드 데이터 조회 중 예상치 못한 오류 발생", e);
            throw new AdminException("대시보드 데이터를 조회하는 중 오류가 발생했습니다", e);
        }
    }
    
    // 🔧 최근 사용자 데이터 조회
    private List<Map<String, Object>> getRecentUsersData() {
        return memberRepository.findTop5ByOrderByCreatedAtDesc()
                .stream()
                .map(user -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", user.getId());
                    map.put("username", user.getUsername());
                    map.put("displayName", user.getDisplayName());
                    map.put("email", user.getEmail());
                    return map;
                })
                .toList();
    }
    
    // 🔧 최근 이미지 데이터 조회
    private List<Map<String, Object>> getRecentImagesData() {
        return imageRepository.findTop5ByOrderByCreatedAtDesc()
                .stream()
                .map(img -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", img.getId());
                    map.put("imageName", img.getImageName());
                    map.put("uploaderName", img.getUploaderName());
                    return map;
                })
                .toList();
    }
    
    // 🔧 최근 게시글 데이터 조회
    private List<Map<String, Object>> getRecentPostsData() {
        return forumRepository.findTop5ByOrderByCreatedAtDesc()
                .stream()
                .map(post -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", post.getId());
                    map.put("title", post.getTitle());
                    map.put("authorDisplayName", post.getAuthorDisplayName());
                    return map;
                })
                .toList();
    }

    @Transactional
    public MemberDto adminEditUser(MemberFormDto memberDto) {
        try {
            log.info("관리자 회원 정보 수정 시작: {}", memberDto.getUsername());
            
            // 입력값 검증
            validateMemberFormDto(memberDto);
            
            String username = memberDto.getUsername();

            // 회원 조회 - 커스텀 예외 발생
            Member editTargetMember = memberRepository.findByUsername(username)
                    .orElseThrow(() -> new MemberNotFoundException(username));
            
            log.info("수정 대상 회원 조회 완료: {}", editTargetMember.getUsername());

            // 회원 정보 수정
            memberDto.adminUpdateMember(editTargetMember);
            
            // 사용자 저장
            Member savedMember = memberRepository.save(editTargetMember);
            
            log.info("회원 정보 수정 완료: {}", savedMember.getUsername());

            // 갱신된 사용자 정보 return
            MemberDto memberdto = new MemberDto();
            return memberdto.convertToDetailMemberDto(savedMember);
            
        } catch (MemberNotFoundException e) {
            // 이미 커스텀 예외이므로 그대로 재발생
            throw e;
        } catch (InvalidMemberDataException e) {
            // 이미 커스텀 예외이므로 그대로 재발생
            throw e;
        } catch (Exception e) {
            log.error("회원 정보 수정 중 예상치 못한 오류 발생: {}", memberDto.getUsername(), e);
            throw new AdminException("회원 정보 수정 중 오류가 발생했습니다", e);
        }
    }
    
    // 입력값 검증 메서드
    private void validateMemberFormDto(MemberFormDto memberDto) {
        if (memberDto == null) {
            throw new InvalidMemberDataException("회원 정보가 비어있습니다");
        }
        
        if (memberDto.getUsername() == null || memberDto.getUsername().trim().isEmpty()) {
            throw new InvalidMemberDataException("사용자명이 비어있습니다");
        }
        
        // 추가 검증 로직들...
        if (memberDto.getDisplayName() != null && memberDto.getDisplayName().length() > 50) {
            throw new InvalidMemberDataException("표시명은 50자를 초과할 수 없습니다");
        }
        
        if (memberDto.getEmail() != null && !isValidEmail(memberDto.getEmail())) {
            throw new InvalidMemberDataException("유효하지 않은 이메일 형식입니다");
        }
    }
    
    // 간단한 이메일 검증
    private boolean isValidEmail(String email) {
        return email.contains("@") && email.contains(".");
    }
}

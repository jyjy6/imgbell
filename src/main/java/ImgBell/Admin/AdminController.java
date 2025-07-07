package ImgBell.Admin;

import ImgBell.Member.Dto.MemberDto;
import ImgBell.Member.Dto.MemberFormDto;
import ImgBell.Member.Member;
import ImgBell.Member.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@RestController
public class AdminController {
    
    private final AdminService adminService;
    private final MemberService memberService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        log.info("관리자 대시보드 요청");
        
        Map<String, Object> dashboardData = adminService.getDashboardData();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", dashboardData);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/members")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getMembers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int itemsPerPage,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder,
            Authentication authentication
    ) {
        log.info("관리자 회원 목록 요청 - 페이지: {}, 검색어: {}", page, search);
        
        try {
            // 입력값 검증
            validatePaginationParams(page, itemsPerPage);
            
            // 페이지는 0부터 시작하므로 -1
            int pageNumber = Math.max(0, page - 1);

            // 정렬 방향 설정
            Sort.Direction direction = sortOrder.equalsIgnoreCase("desc")
                    ? Sort.Direction.DESC
                    : Sort.Direction.ASC;

            // Pageable 객체 생성
            Pageable pageable = PageRequest.of(pageNumber, itemsPerPage, Sort.by(direction, sortBy));

            // 서비스에서 페이지네이션된 데이터 가져오기
            Page<Member> memberPage = memberService.getMembers(search, pageable);

            // DTO로 변환
            List<MemberDto> memberDTOs = memberPage.getContent().stream()
                    .map(MemberDto::convertToDetailMemberDto)
                    .collect(Collectors.toList());

            // Vue.js에서 기대하는 응답 형식
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", memberDTOs);
            response.put("total", memberPage.getTotalElements());
            response.put("totalPages", memberPage.getTotalPages());
            response.put("currentPage", page);

            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("회원 목록 조회 중 오류 발생", e);
            throw new AdminException("회원 목록을 조회하는 중 오류가 발생했습니다", e);
        }
    }

    @PutMapping("/members/userupdate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateMember(
            @RequestBody MemberFormDto memberDto, 
            Authentication authentication
    ) {
        log.info("관리자 회원 정보 수정 요청 - 대상: {}", memberDto.getUsername());
        
        // 관리자 권한 재검증 (이중 보안)
        validateAdminPermission(authentication);
        
        MemberDto updatedMember = adminService.adminEditUser(memberDto);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "회원정보가 성공적으로 수정되었습니다.");
        response.put("user", updatedMember);

        return ResponseEntity.ok(response);
    }
    
    // 페이지네이션 파라미터 검증
    private void validatePaginationParams(int page, int itemsPerPage) {
        if (page < 1) {
            throw new InvalidMemberDataException("페이지 번호는 1 이상이어야 합니다");
        }
        
        if (itemsPerPage < 1 || itemsPerPage > 100) {
            throw new InvalidMemberDataException("페이지당 항목 수는 1-100 사이여야 합니다");
        }
    }
    
    // 관리자 권한 검증
    private void validateAdminPermission(Authentication authentication) {
        if (authentication == null) {
            throw new UnauthorizedAccessException();
        }
        
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        
        if (!isAdmin) {
            log.warn("관리자 권한 없는 사용자의 접근 시도: {}", authentication.getName());
            throw new UnauthorizedAccessException();
        }
    }
}

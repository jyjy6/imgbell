package ImgBell.Admin;

import ImgBell.Member.Dto.MemberDto;
import ImgBell.Member.Dto.MemberFormDto;
import ImgBell.Member.Member;
import ImgBell.Member.MemberRepository;
import ImgBell.Member.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RequiredArgsConstructor
@RequestMapping("/api/admin")
@RestController
public class AdminController {
    
    private final AdminService adminService;


    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard() {
        return ResponseEntity.ok(adminService.getDashboardData());
    }

    private final MemberService memberService;

    @GetMapping("/members")
    public ResponseEntity<Map<String, Object>> getMembers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int itemsPerPage,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder
    ) {
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
        response.put("data", memberDTOs);
        response.put("total", memberPage.getTotalElements());
        response.put("totalPages", memberPage.getTotalPages());
        response.put("currentPage", page);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/members/userupdate")
    public ResponseEntity<?> updateMember(@RequestBody MemberFormDto memberDto, Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    Map.of("message", "접근 권한이 없습니다.")
            );
        }

        MemberDto updatedMember = adminService.adminEditUser(memberDto);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "회원정보가 성공적으로 수정되었습니다.");
        response.put("user", updatedMember);

        return ResponseEntity.ok(response);
    }

}

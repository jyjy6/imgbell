package ImgBell.Member;


import ImgBell.Member.Dto.MemberDto;
import ImgBell.Member.Dto.MemberFormDto;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/members")
public class MemberController {
    private final MemberService memberService;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;



    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody MemberFormDto memberFormDto) {
        try {
            memberService.registerUser(memberFormDto);
            return new ResponseEntity<>("User registered successfully", HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/modify")
    public ResponseEntity<MemberDto> editUser(@RequestBody MemberFormDto memberDto, Authentication auth) {
        try {
            System.out.println("인증정보" + auth);
            MemberDto updatedUser = memberService.editUser(memberDto, auth);
            return new ResponseEntity<>(updatedUser, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }



    @PostMapping("/validate")
    public ResponseEntity<String> validatePassword(@RequestBody Map<String, String> requestBody) {
        return memberService.validatePw(requestBody);
    }


    @GetMapping("/userinfo")
    public ResponseEntity<?> getUserInfo(Authentication auth) {

        if (auth.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");
        }

        var result = memberRepository.findByUsername(auth.getName());
        if (result.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("사용자를 찾을 수 없습니다");
        }
        Member member = result.get();
        MemberDto memberdto = new MemberDto();
        MemberDto userInfo = memberdto.convertToDetailMemberDto(member);

        return ResponseEntity.ok(userInfo);
    }

    @GetMapping("/status")
    public ResponseEntity<String> getSessionStatus(HttpSession session, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return ResponseEntity.ok("세션 유지 중 (인증됨)");
        } else if (session != null && session.getAttribute("user") != null) {
            return ResponseEntity.ok("세션 유지 중 (세션에 사용자 정보 있음)");
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");
    }

    @PostMapping("/check-username")
    public ResponseEntity<Map<String, Object>> checkUsername(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        Map<String, Object> response = new HashMap<>();
        // 아이디가 제공되지 않은 경우
        if (username == null || username.trim().isEmpty()) {
            response.put("available", false);
            response.put("message", "아이디를 입력하세요.");
            return ResponseEntity.ok(response);
        }

        try {
            // 아이디 중복 검사
            boolean exists = memberRepository.existsByUsername(username);

            if (exists) {
                response.put("available", false);
                response.put("message", "이미 사용 중인 아이디입니다.");
            } else {
                response.put("available", true);
                response.put("message", "사용 가능한 아이디입니다.");
            }

        } catch (Exception e) {
            // 서버 오류 처리
            response.put("available", false);
            response.put("message", "서버 오류가 발생했습니다.");
        }

        return ResponseEntity.ok(response);

    }
    @PostMapping("/check-displayname")
    public ResponseEntity<Map<String, Object>> checkDisplayName(@RequestBody Map<String, String> body, Authentication auth) {
        String displayName = body.get("displayName");

        System.out.println(displayName);
        Map<String, Object> response = new HashMap<>();
        // 닉네임이 제공되지 않은 경우
        if (displayName == null || displayName.trim().isEmpty()) {
            response.put("available", false);
            response.put("message", "닉네임을 입력하세요.");
            return ResponseEntity.ok(response);
        }

        try {
            // 닉네임 중복 검사
            boolean exists = memberRepository.existsByDisplayName(displayName);
            if (auth != null && auth.isAuthenticated()){

                Member member = memberRepository.findByUsername(auth.getName()).orElseThrow(() -> new RuntimeException("Member not found"));
                String presentDisplayName = member.getDisplayName();
                if(presentDisplayName.equals(displayName)){
                    response.put("available", true);
                    response.put("message", "지금 니 아이디입니다");
                    return ResponseEntity.ok(response);
                }
            }

            if (exists) {
                response.put("available", false);
                response.put("message", "이미 사용 중인 아이디입니다.");
            } else {
                response.put("available", true);
                response.put("message", "사용 가능한 아이디입니다.");
            }

        } catch (Exception e) {
            // 서버 오류 처리
            System.out.println(e.getMessage());
            response.put("available", false);
            response.put("message", "서버 오류가 발생했습니다.");
        }

        return ResponseEntity.ok(response);
    }




}





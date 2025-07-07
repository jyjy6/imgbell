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

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody MemberFormDto memberFormDto) {
        memberService.registerUser(memberFormDto);
        return new ResponseEntity<>("회원가입이 완료되었습니다", HttpStatus.CREATED);
    }

    @PutMapping("/modify")
    public ResponseEntity<MemberDto> editUser(@RequestBody MemberFormDto memberDto, Authentication auth) {
        MemberDto updatedUser = memberService.editUser(memberDto, auth);
        return new ResponseEntity<>(updatedUser, HttpStatus.OK);
    }

    @PostMapping("/validate")
    public String validatePassword(@RequestBody Map<String, String> requestBody) {
        return memberService.validatePw(requestBody);
    }

    @GetMapping("/userinfo")
    public MemberDto getUserInfo(Authentication auth) {
        return memberService.getUserInfo(auth);
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
    public Map<String, Object> checkUsername(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        return memberService.checkUsername(username);
    }

    @PostMapping("/check-displayname")
    public Map<String, Object> checkDisplayName(@RequestBody Map<String, String> body, Authentication auth) {
        String displayName = body.get("displayName");
        return memberService.checkDisplayName(displayName, auth);
    }
}





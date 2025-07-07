package ImgBell.Member;

import ImgBell.GlobalErrorHandler.GlobalException;
import ImgBell.Member.Dto.MemberDto;
import ImgBell.Member.Dto.MemberFormDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;

@RequiredArgsConstructor
@Service
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;




    public Member registerUser(MemberFormDto memberFormDto) {
        // 사용자 이름, 닉네임이 이미 존재하는지 확인
        if (memberRepository.existsByUsername(memberFormDto.getUsername())) {
            throw new GlobalException("이미 사용 중인 아이디입니다", "USERNAME_ALREADY_EXISTS");
        } else if (memberRepository.existsByDisplayName(memberFormDto.getDisplayName())) {
            throw new GlobalException("이미 사용 중인 닉네임입니다", "DISPLAYNAME_ALREADY_EXISTS");
        }
        // 비밀번호가 비어 있지 않으면 암호화
        if (memberFormDto.getPassword() == null || memberFormDto.getPassword().isEmpty()) {
            throw new GlobalException("비밀번호는 필수 입력 항목입니다", "PASSWORD_REQUIRED");
        }
        // 비밀번호 암호화
        memberFormDto.setPassword(passwordEncoder.encode(memberFormDto.getPassword()));

        Member newMember = memberFormDto.convertToMember();
        if(newMember.getUsername().equals("admin")){
            newMember.addRole("ROLE_SUPERADMIN");
            newMember.addRole("ROLE_ADMIN");
        }
        // 사용자 저장
        return memberRepository.save(newMember);
    }


    @Transactional
    public MemberDto editUser(MemberFormDto memberDto, Authentication auth) {
        String username = ((CustomUserDetails)auth.getPrincipal()).getUsername();
        if(!username.equals(memberDto.getUsername())){
            throw new GlobalException("아이디는 수정할 수 없습니다.", "ID_CANNOT_BE_MODIFIED");
        }

        Member editTargetMember = memberRepository.findByUsername(username)
                .orElseThrow(() -> new GlobalException("Member not found", "MEMBER_NOT_FOUND", HttpStatus.NOT_FOUND));
        

        // 비밀번호도 바꿨으면 설정
        if (memberDto.getPassword() != null && !memberDto.getPassword().isEmpty()) {
            editTargetMember.setPassword(passwordEncoder.encode(memberDto.getPassword()));
        } else {
            editTargetMember.setPassword(editTargetMember.getPassword());
        }

        memberDto.updateMember(editTargetMember);
        // 사용자 저장
        memberRepository.save(editTargetMember);

        // 갱신된 사용자 정보 return
        MemberDto memberdto = new MemberDto();
        return memberdto.convertToDetailMemberDto(editTargetMember);
    }

    public String validatePw(@RequestBody Map<String, String> requestBody){
        String id = requestBody.get("id"); // id 추출
        String pw = requestBody.get("pw"); // pw 추출

        var targetMember = memberRepository.findByUsername(id)
                .orElseThrow(() -> new GlobalException("그런 멤버 없습니다", "MEMBER_NOT_FOUND", HttpStatus.NOT_FOUND));
        
        if (!passwordEncoder.matches(pw, targetMember.getPassword())) {
            throw new GlobalException("비밀번호가 일치하지 않습니다", "INVALID_PASSWORD", HttpStatus.UNAUTHORIZED);
        }
        
        return "Valid";
    }

    public Map<String, Object> checkUsername(String username) {
        Map<String, Object> response = new HashMap<>();
        
        // 아이디가 제공되지 않은 경우
        if (username == null || username.trim().isEmpty()) {
            response.put("available", false);
            response.put("message", "아이디를 입력하세요.");
            return response;
        }

        // 아이디 중복 검사
        boolean exists = memberRepository.existsByUsername(username);
        
        if (exists) {
            response.put("available", false);
            response.put("message", "이미 사용 중인 아이디입니다.");
        } else {
            response.put("available", true);
            response.put("message", "사용 가능한 아이디입니다.");
        }
        
        return response;
    }

    public Map<String, Object> checkDisplayName(String displayName, Authentication auth) {
        Map<String, Object> response = new HashMap<>();
        
        // 닉네임이 제공되지 않은 경우
        if (displayName == null || displayName.trim().isEmpty()) {
            response.put("available", false);
            response.put("message", "닉네임을 입력하세요.");
            return response;
        }

        // 로그인된 사용자인 경우 현재 닉네임과 비교
        if (auth != null && auth.isAuthenticated()) {
            String username = ((CustomUserDetails)auth.getPrincipal()).getUsername();
            Member member = memberRepository.findByUsername(username)
                    .orElseThrow(() -> new GlobalException("사용자를 찾을 수 없습니다", "MEMBER_NOT_FOUND", HttpStatus.NOT_FOUND));
            
            String presentDisplayName = member.getDisplayName();
            if (presentDisplayName.equals(displayName)) {
                response.put("available", true);
                response.put("message", "현재 사용 중인 닉네임입니다.");
                return response;
            }
        }

        // 닉네임 중복 검사
        boolean exists = memberRepository.existsByDisplayName(displayName);
        
        if (exists) {
            response.put("available", false);
            response.put("message", "이미 사용 중인 닉네임입니다.");
        } else {
            response.put("available", true);
            response.put("message", "사용 가능한 닉네임입니다.");
        }
        
        return response;
    }

    public MemberDto getUserInfo(Authentication auth) {
        // 인증 확인
        if (auth == null || auth.getPrincipal() == null) {
            throw new GlobalException("로그인이 필요합니다", "LOGIN_REQUIRED", HttpStatus.UNAUTHORIZED);
        }
        String username = ((CustomUserDetails)auth.getPrincipal()).getUsername();
        // 사용자 조회
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new GlobalException("사용자를 찾을 수 없습니다", "MEMBER_NOT_FOUND", HttpStatus.NOT_FOUND));
        // DTO 변환
        MemberDto memberDto = new MemberDto();
        return memberDto.convertToDetailMemberDto(member);
    }



    public Page<Member> getMembers(String search, Pageable pageable) {
        if (search != null && !search.trim().isEmpty()) {
            // 이름, 사용자명, 이메일로 검색
            return memberRepository.findByNameContainingIgnoreCaseOrUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                    search, search, search, pageable);
        }

        return memberRepository.findAll(pageable);
    }





    public Member getUserById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new GlobalException("회원을 찾을 수 없습니다", "MEMBER_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    public Member createUser(Member member) {
        return memberRepository.save(member);
    }

    public void deleteUser(Long id) {
        memberRepository.deleteById(id);
    }


}

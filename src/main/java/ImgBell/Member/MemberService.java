package ImgBell.Member;



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

@RequiredArgsConstructor
@Service
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;




    public Member registerUser(MemberFormDto memberFormDto) {
        // 사용자 이름, 닉네임이 이미 존재하는지 확인
        if (memberRepository.existsByUsername(memberFormDto.getUsername())) {
            throw new RuntimeException("Username already exists");
        } else if (memberRepository.existsByDisplayName(memberFormDto.getDisplayName())) {
            throw new RuntimeException("Display name already exists");
        }
        // 비밀번호가 비어 있지 않으면 암호화
        if (memberFormDto.getPassword() == null || memberFormDto.getPassword().isEmpty()) {
            throw new RuntimeException("Password cannot be empty"); // 비밀번호가 없으면 예외 처리
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
            throw new IllegalArgumentException("아이디는 수정할 수 없습니다.");
        }

        Member editTargetMember = memberRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        System.out.println(editTargetMember);

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

    public ResponseEntity<String> validatePw(@RequestBody Map<String, String> requestBody){
        String id = requestBody.get("id"); // id 추출
        String pw = requestBody.get("pw"); // pw 추출

        var targetMember = memberRepository.findByUsername(id)
                .orElseThrow(() -> new RuntimeException("Value not found!"));
        var correctPassword = targetMember.getPassword();
        // 비밀번호 검증
        if (passwordEncoder.matches(pw, correctPassword)) {
            return ResponseEntity.ok().body("Valid");
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid password");
        }

    }

    public Page<Member> getMembers(String search, Pageable pageable) {
        if (search != null && !search.trim().isEmpty()) {
            // 이름, 사용자명, 이메일로 검색
            return memberRepository.findByNameContainingIgnoreCaseOrUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                    search, search, search, pageable);
        }

        return memberRepository.findAll(pageable);
    }




    public List<Member> getAllUsers() {
        return memberRepository.findAll();
    }

    public Optional<Member> getUserById(Long id) {
        return memberRepository.findById(id);
    }

    public Member createUser(Member member) {
        return memberRepository.save(member);
    }

    public void deleteUser(Long id) {
        memberRepository.deleteById(id);
    }


}

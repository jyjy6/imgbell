package ImgBell.Member;


import ImgBell.Member.Dto.MemberDto;
import ImgBell.Member.Dto.MemberFormDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Authentication authentication;

    @Mock
    private CustomUserDetails customUserDetails;

    @InjectMocks
    private MemberService memberService; // 테스트 대상

    @Test
    void 회원가입_성공() {
        // given
        MemberFormDto dto = MemberFormDto.builder()
                .username("user123")
                .displayName("nickname")
                .password("1234")
                .build();

        when(memberRepository.existsByUsername("user123")).thenReturn(false);
        when(memberRepository.existsByDisplayName("nickname")).thenReturn(false);
        when(passwordEncoder.encode("1234")).thenReturn("ENCODED_PW");

        Member expected = Member.builder().username("user123").build();
        when(memberRepository.save(any(Member.class))).thenReturn(expected);

        // when
        Member result = memberService.registerUser(dto);

        // then
        assertEquals("user123", result.getUsername());
        verify(memberRepository).save(any(Member.class));
    }

    @Test
    void 사용자이름_중복_예외() {
        // given
        MemberFormDto dto = MemberFormDto.builder()
                .username("user123")
                .displayName("nickname")
                .password("1234")
                .build();

        //when
        when(memberRepository.existsByUsername("user123")).thenReturn(true);

        //then
        assertThrows(RuntimeException.class, () -> memberService.registerUser(dto));
    }

    @Test
    void 닉네임_중복_예외() {
        // given
        MemberFormDto dto = MemberFormDto.builder()
                .username("user123")
                .displayName("nickname")
                .password("1234")
                .build();

        // when
        //멤버리포지토리에 유저네임user123이 있으면 false내주세요~
        when(memberRepository.existsByUsername("user123")).thenReturn(false);
        //닉네임nickname이 있으면 true(중복닉네임)내주세요~ 즉, 닉네임이 중복 ->오류나야함
        when(memberRepository.existsByDisplayName("nickname")).thenReturn(true);

        // then
        assertThrows(RuntimeException.class, () -> memberService.registerUser(dto));
    }

    @Test
    void 비밀번호_빈값_예외() {
        // given
        MemberFormDto dto = MemberFormDto.builder()
                .username("user123")
                .displayName("nickname")
                .password("") // 빈 비밀번호
                .build();

        when(memberRepository.existsByUsername("user123")).thenReturn(false);
        when(memberRepository.existsByDisplayName("nickname")).thenReturn(false);

        // when & then
        assertThrows(RuntimeException.class, () -> memberService.registerUser(dto));
    }

    @Test
    void 관리자계정_권한_부여() {
        // given
        MemberFormDto dto = MemberFormDto.builder()
                .username("admin")
                .displayName("adminnick")
                .password("adminpw")
                .build();

        when(memberRepository.existsByUsername("admin")).thenReturn(false);
        when(memberRepository.existsByDisplayName("adminnick")).thenReturn(false);
        when(passwordEncoder.encode("adminpw")).thenReturn("ENCODED_PW");

        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Member result = memberService.registerUser(dto);

        // then
        assertTrue(result.getRoleSet().contains("ROLE_ADMIN"));
        assertTrue(result.getRoleSet().contains("ROLE_SUPERADMIN"));
    }

    @Test
    void 사용자정보수정_성공_비밀번호포함() {
        // given
        String username = "user123";
        String newPassword = "newPassword";
        String encodedPassword = "ENCODED_NEW_PW";
        
        MemberFormDto memberFormDto = MemberFormDto.builder()
                .username(username)
                .displayName("newNickname")
                .password(newPassword)
                .build();

        Member existingMember = Member.builder()
                .username(username)
                .displayName("oldNickname")
                .password("OLD_ENCODED_PW")
                .build();

        // Mock Authentication과 CustomUserDetails
        when(authentication.getPrincipal()).thenReturn(customUserDetails);
        when(customUserDetails.getUsername()).thenReturn(username);
        when(memberRepository.findByUsername(username)).thenReturn(Optional.of(existingMember));
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);

        // when
        MemberDto result = memberService.editUser(memberFormDto, authentication);

        // then
        assertNotNull(result);
        verify(memberRepository).save(existingMember);
        verify(passwordEncoder).encode(newPassword);
    }

    @Test
    void 사용자정보수정_성공_비밀번호없음() {
        // given
        String username = "user123";
        
        MemberFormDto memberFormDto = MemberFormDto.builder()
                .username(username)
                .displayName("newNickname")
                .password("") // 빈 비밀번호
                .build();

        Member existingMember = Member.builder()
                .username(username)
                .displayName("oldNickname")
                .password("OLD_ENCODED_PW")
                .build();

        // Mock Authentication과 CustomUserDetails
        when(authentication.getPrincipal()).thenReturn(customUserDetails);
        when(customUserDetails.getUsername()).thenReturn(username);
        when(memberRepository.findByUsername(username)).thenReturn(Optional.of(existingMember));

        // when
        MemberDto result = memberService.editUser(memberFormDto, authentication);

        // then
        assertNotNull(result);
        verify(memberRepository).save(existingMember);
        // 비밀번호 인코딩이 호출되지 않았는지 확인
        verify(passwordEncoder, org.mockito.Mockito.never()).encode(any());
    }

    @Test
    void 사용자정보수정_성공_비밀번호null() {
        // given
        String username = "user123";
        
        MemberFormDto memberFormDto = MemberFormDto.builder()
                .username(username)
                .displayName("newNickname")
                .password(null) // null 비밀번호
                .build();

        Member existingMember = Member.builder()
                .username(username)
                .displayName("oldNickname")
                .password("OLD_ENCODED_PW")
                .build();

        // Mock Authentication과 CustomUserDetails
        when(authentication.getPrincipal()).thenReturn(customUserDetails);
        when(customUserDetails.getUsername()).thenReturn(username);
        when(memberRepository.findByUsername(username)).thenReturn(Optional.of(existingMember));

        // when
        MemberDto result = memberService.editUser(memberFormDto, authentication);

        // then
        assertNotNull(result);
        verify(memberRepository).save(existingMember);
        // 비밀번호 인코딩이 호출되지 않았는지 확인
        verify(passwordEncoder, org.mockito.Mockito.never()).encode(any());
    }

    @Test
    void 사용자정보수정_실패_다른사용자수정시도() {
        // given
        String loginUsername = "user123";
        String targetUsername = "otherUser";
        
        MemberFormDto memberFormDto = MemberFormDto.builder()
                .username(targetUsername)
                .displayName("newNickname")
                .password("password")
                .build();

        // Mock Authentication과 CustomUserDetails
        when(authentication.getPrincipal()).thenReturn(customUserDetails);
        when(customUserDetails.getUsername()).thenReturn(loginUsername);

        // when & then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> memberService.editUser(memberFormDto, authentication));
        
        assertEquals("아이디는 수정할 수 없습니다.", exception.getMessage());
    }

    @Test
    void 사용자정보수정_실패_존재하지않는사용자() {
        // given
        String username = "nonExistentUser";
        
        MemberFormDto memberFormDto = MemberFormDto.builder()
                .username(username)
                .displayName("newNickname")
                .password("password")
                .build();

        // Mock Authentication과 CustomUserDetails
        when(authentication.getPrincipal()).thenReturn(customUserDetails);
        when(customUserDetails.getUsername()).thenReturn(username);
        when(memberRepository.findByUsername(username)).thenReturn(Optional.empty());

        // when & then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> memberService.editUser(memberFormDto, authentication));
        
        assertEquals("Member not found", exception.getMessage());
    }
}

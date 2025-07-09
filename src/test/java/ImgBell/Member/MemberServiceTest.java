package ImgBell.Member;


import ImgBell.GlobalErrorHandler.GlobalException;
import ImgBell.Member.Dto.MemberDto;
import ImgBell.Member.Dto.MemberFormDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    @Test
    void 사용자_정보_요청_성공(){
        //given
        String username = "user1234";
        
        Member member = Member.builder()
                .username(username)
                .displayName("testNickname")
                .password("encodedPassword")
                .build();

        // Mock Authentication과 CustomUserDetails
        when(authentication.getPrincipal()).thenReturn(customUserDetails);
        when(customUserDetails.getUsername()).thenReturn(username);
        when(memberRepository.findByUsername(username)).thenReturn(Optional.of(member));

        //when
        MemberDto result = memberService.getUserInfo(authentication);

        //then
        assertNotNull(result);
        assertEquals(username, result.getUsername());
        assertEquals("testNickname", result.getDisplayName());
        verify(memberRepository).findByUsername(username);
    }

    @Test
    void 사용자_정보_요청_실패_Authentication이_null(){
        //given
        Authentication nullAuth = null;

        //when & then
        GlobalException exception = assertThrows(GlobalException.class, 
            () -> memberService.getUserInfo(nullAuth));
        
        assertEquals("로그인이 필요합니다", exception.getMessage());
        assertEquals("LOGIN_REQUIRED", exception.getErrorCode());
    }

    @Test
    void 사용자_정보_요청_실패_Principal이_null(){
        //given
        when(authentication.getPrincipal()).thenReturn(null);

        //when & then
        GlobalException exception = assertThrows(GlobalException.class, 
            () -> memberService.getUserInfo(authentication));
        
        assertEquals("로그인이 필요합니다", exception.getMessage());
        assertEquals("LOGIN_REQUIRED", exception.getErrorCode());
    }

    @Test
    void 사용자_정보_요청_실패_존재하지않는_사용자(){
        //given
        String username = "nonExistentUser";
        
        // Mock Authentication과 CustomUserDetails
        when(authentication.getPrincipal()).thenReturn(customUserDetails);
        when(customUserDetails.getUsername()).thenReturn(username);
        when(memberRepository.findByUsername(username)).thenReturn(Optional.empty());

        //when & then
        GlobalException exception = assertThrows(GlobalException.class, 
            () -> memberService.getUserInfo(authentication));
        
        assertEquals("사용자를 찾을 수 없습니다", exception.getMessage());
        assertEquals("MEMBER_NOT_FOUND", exception.getErrorCode());
        verify(memberRepository).findByUsername(username);
    }

    // ============== getMembers 메서드 테스트 ==============
    
    @Test
    void 회원목록조회_검색어없음() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        List<Member> members = Arrays.asList(
            Member.builder().username("user1").displayName("닉네임1").build(),
            Member.builder().username("user2").displayName("닉네임2").build()
        );
        Page<Member> expectedPage = new PageImpl<>(members, pageable, members.size());
        
        when(memberRepository.findAll(pageable)).thenReturn(expectedPage);

        // when
        Page<Member> result = memberService.getMembers(null, pageable);

        // then
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        verify(memberRepository).findAll(pageable);
        // 검색 메서드가 호출되지 않았는지 확인
        verify(memberRepository, org.mockito.Mockito.never())
            .findByNameContainingIgnoreCaseOrUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                any(), any(), any(), any());
    }

    @Test
    void 회원목록조회_빈검색어() {
        // given
        String search = ""; // 빈 문자열
        Pageable pageable = PageRequest.of(0, 10);
        List<Member> members = Arrays.asList(
            Member.builder().username("user1").displayName("닉네임1").build()
        );
        Page<Member> expectedPage = new PageImpl<>(members, pageable, members.size());
        
        when(memberRepository.findAll(pageable)).thenReturn(expectedPage);

        // when
        Page<Member> result = memberService.getMembers(search, pageable);

        // then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(memberRepository).findAll(pageable);
    }

    @Test
    void 회원목록조회_공백검색어() {
        // given
        String search = "   "; // 공백만 있는 문자열
        Pageable pageable = PageRequest.of(0, 10);
        List<Member> members = Arrays.asList(
            Member.builder().username("user1").displayName("닉네임1").build()
        );
        Page<Member> expectedPage = new PageImpl<>(members, pageable, members.size());
        
        when(memberRepository.findAll(pageable)).thenReturn(expectedPage);

        // when
        Page<Member> result = memberService.getMembers(search, pageable);

        // then
        assertNotNull(result);
        verify(memberRepository).findAll(pageable);
        // trim() 때문에 공백은 빈 문자열 취급되어 검색 메서드 호출 안됨
        verify(memberRepository, org.mockito.Mockito.never())
            .findByNameContainingIgnoreCaseOrUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                any(), any(), any(), any());
    }

    @Test
    void 회원목록조회_검색어있음() {
        // given
        String search = "test";
        Pageable pageable = PageRequest.of(0, 10);
        List<Member> searchResults = Arrays.asList(
            Member.builder().username("testuser").displayName("테스트닉네임").build()
        );
        Page<Member> expectedPage = new PageImpl<>(searchResults, pageable, searchResults.size());
        
        when(memberRepository.findByNameContainingIgnoreCaseOrUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            eq(search), eq(search), eq(search), eq(pageable))).thenReturn(expectedPage);

        // when
        Page<Member> result = memberService.getMembers(search, pageable);

        // then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("testuser", result.getContent().get(0).getUsername());
        
        // 정확한 파라미터로 검색 메서드가 호출되었는지 확인
        verify(memberRepository).findByNameContainingIgnoreCaseOrUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            eq(search), eq(search), eq(search), eq(pageable));
        
        // findAll은 호출되지 않았는지 확인
        verify(memberRepository, org.mockito.Mockito.never()).findAll(any(Pageable.class));
    }



}

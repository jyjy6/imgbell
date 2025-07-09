package ImgBell.Forum;


import ImgBell.GlobalErrorHandler.GlobalException;
import ImgBell.Image.Image;
import ImgBell.Member.CustomUserDetails;
import ImgBell.Member.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)  // 🔥 이것만 추가하면 됨!
public class ForumServiceTest {
    @InjectMocks
    private ForumService forumService;
    @Mock
    private ForumRepository forumRepository;
    @Mock
    private Authentication authentication;
    @Mock
    private CustomUserDetails customUserDetails;
    @Captor
    private ArgumentCaptor<Forum> forumCaptor;

    private Member testMember;
    private Forum testForum;

    @BeforeEach
    void setUp(){
        // 공통 Mock 설정 - 성공 테스트를 위한 기본 설정
        when(authentication.getPrincipal()).thenReturn(customUserDetails);
        when(authentication.isAuthenticated()).thenReturn(true); // 이것도 중요!
        when(customUserDetails.getUsername()).thenReturn("testuser");
        when(customUserDetails.getDisplayName()).thenReturn("테스트유저");
    }

    // 헬퍼 메서드들
    private ForumFormDto createForumDto(String title, String content, Forum.PostType type) {
        ForumFormDto dto = new ForumFormDto();
        dto.setTitle(title);
        dto.setContent(content);
        dto.setType(type);
        return dto;
    }

    private Forum createExistingForum(Long id, String title, String username) {
        return createExistingForum(id, title, username, "기존닉네임");
    }

    private Forum createExistingForum(Long id, String title, String username, String displayName) {
        return Forum.builder()
                .id(id)
                .title(title)
                .content("기존 내용")
                .authorUsername(username)
                .authorDisplayName(displayName)
                .type(Forum.PostType.NORMAL)
                .createdAt(java.time.LocalDateTime.now()) // 🔥 필수 필드 추가!
                .viewCount(0)                             // 🔥 필수 필드 추가!
                .likeCount(0)                             // 🔥 필수 필드 추가!
                .comments(new java.util.ArrayList<>())    // 🔥 필수 필드 추가!
                .build();
    }

    // ============== postForum 메서드 테스트 ==============

    @Test
    void 포럼게시글작성_성공_타입지정() {
        // given
        ForumFormDto forumDto = createForumDto("테스트 제목", "테스트 내용", Forum.PostType.HOT);

        // when
        forumService.postForum(forumDto, authentication);

        // then
        verify(forumRepository).save(forumCaptor.capture());

        Forum savedForum = forumCaptor.getValue();
        assertEquals("테스트 제목", savedForum.getTitle());
        assertEquals("테스트 내용", savedForum.getContent());
        assertEquals(Forum.PostType.HOT, savedForum.getType());
        assertEquals("testuser", savedForum.getAuthorUsername());
        assertEquals("테스트유저", savedForum.getAuthorDisplayName());
    }

    @Test
    void 포럼게시글작성_성공_타입null_기본NORMAL설정() {
        // given
        ForumFormDto forumDto = createForumDto("테스트 제목", "테스트 내용", null);

        // when
        forumService.postForum(forumDto, authentication);

        // then
        verify(forumRepository).save(forumCaptor.capture());

        Forum savedForum = forumCaptor.getValue();
        assertEquals("테스트 제목", savedForum.getTitle());
        assertEquals("테스트 내용", savedForum.getContent());
        assertEquals(Forum.PostType.NORMAL, savedForum.getType()); // null이면 NORMAL로 설정
        assertEquals("testuser", savedForum.getAuthorUsername());
        assertEquals("테스트유저", savedForum.getAuthorDisplayName());
    }

    @Test
    void 포럼게시글작성_실패_Authentication이null() {
        // given
        ForumFormDto forumDto = createForumDto("테스트 제목", "테스트 내용", Forum.PostType.NORMAL);

        // when & then
        assertThrows(GlobalException.class,
            () -> forumService.postForum(forumDto, null));

        // save가 호출되지 않았는지 확인
        verify(forumRepository, never()).save(any(Forum.class));
    }


    // ============== editForum 메서드 테스트 ==============

    @DisplayName("포럼 게시글 수정이 성공한다 - 타입 지정")
    @Test
    void editForum_Success_WithType() {
        // given
        Long forumId = 1L;
        ForumFormDto forumDto = createForumDto("수정된 제목", "수정된 내용", Forum.PostType.HOT);
        Forum existingForum = createExistingForum(forumId, "기존 제목", "testuser");

        when(forumRepository.findById(forumId)).thenReturn(Optional.of(existingForum));

        // when
        forumService.editForum(forumId, forumDto, authentication);

        // then
        verify(forumRepository).save(forumCaptor.capture());

        Forum savedForum = forumCaptor.getValue();
        assertEquals("수정된 제목", savedForum.getTitle());
        assertEquals("수정된 내용", savedForum.getContent());
        assertEquals(Forum.PostType.HOT, savedForum.getType());
        assertEquals("testuser", savedForum.getAuthorUsername());
        assertEquals("테스트유저", savedForum.getAuthorDisplayName());
    }

    @DisplayName("포럼 게시글 수정이 성공한다 - 타입 null시 NORMAL 설정")
    @Test
    void editForum_Success_TypeNullSetsNormal() {
        // given
        Long forumId = 1L;
        ForumFormDto forumDto = createForumDto("수정된 제목", "수정된 내용", null);
        Forum existingForum = createExistingForum(forumId, "기존 제목", "testuser");

        when(forumRepository.findById(forumId)).thenReturn(Optional.of(existingForum));

        // when
        forumService.editForum(forumId, forumDto, authentication);

        // then
        verify(forumRepository).save(forumCaptor.capture());

        Forum savedForum = forumCaptor.getValue();
        assertEquals("수정된 제목", savedForum.getTitle());
        assertEquals("수정된 내용", savedForum.getContent());
        assertEquals(Forum.PostType.NORMAL, savedForum.getType()); // null이면 NORMAL
        assertEquals("testuser", savedForum.getAuthorUsername());
        assertEquals("테스트유저", savedForum.getAuthorDisplayName());
    }

    @DisplayName("존재하지 않는 게시글 수정시 예외가 발생한다")
    @Test
    void editForum_ThrowsException_WhenForumNotFound() {
        // given
        Long nonExistentId = 999L;
        ForumFormDto forumDto = createForumDto("수정된 제목", "수정된 내용", Forum.PostType.NORMAL);

                when(forumRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // when & then - 예외가 정상적으로 발생해야 함
        GlobalException exception = assertThrows(GlobalException.class,
            () -> forumService.editForum(nonExistentId, forumDto, authentication));

        // 예외 내용 검증
        assertEquals("게시글을 찾을 수 없습니다", exception.getMessage());
        assertEquals("FORUM_NOT_FOUND", exception.getErrorCode());
        
        // save가 호출되지 않았는지 확인
        verify(forumRepository, never()).save(any(Forum.class));
    }

    @DisplayName("다른 사용자가 게시글 수정시 예외가 발생한다")
    @Test
    void editForum_ThrowsException_WhenUnauthorizedUser() {
        // given
        Long forumId = 1L;
        String originalAuthor = "originaluser";
        String hackerUsername = "hacker";

        ForumFormDto forumDto = createForumDto("해킹시도", "해킹내용", Forum.PostType.NORMAL);
        Forum existingForum = createExistingForum(forumId, "기존 제목", originalAuthor, "원작성자");

        // 다른 사용자로 Mock 설정 변경 (setUp의 공통 설정을 덮어씀)
        when(customUserDetails.getUsername()).thenReturn(hackerUsername);
        when(customUserDetails.getDisplayName()).thenReturn("해커");
        when(forumRepository.findById(forumId)).thenReturn(Optional.of(existingForum));

        // when & then - 권한 예외가 정상적으로 발생해야 함
        GlobalException exception = assertThrows(GlobalException.class,
            () -> forumService.editForum(forumId, forumDto, authentication));

        // 예외 내용 검증
        assertEquals("글쓴이만 수정할 수 있습니다", exception.getMessage());
        assertEquals("UNAUTHORIZED_EDIT", exception.getErrorCode());
        
        // save가 호출되지 않았는지 확인
        verify(forumRepository, never()).save(any(Forum.class));
    }

    @DisplayName("Authentication이 null이면 예외가 발생한다")
    @Test
    void editForum_ThrowsException_WhenAuthenticationIsNull() {
        // given
        Long forumId = 1L;
        ForumFormDto forumDto = createForumDto("수정된 제목", "수정된 내용", Forum.PostType.NORMAL);

        // when & then - NullPointerException이 발생해야 함
        assertThrows(NullPointerException.class,
            () -> forumService.editForum(forumId, forumDto, null));

        // findById나 save가 호출되지 않아야 함
        verify(forumRepository, never()).findById(any());
        verify(forumRepository, never()).save(any(Forum.class));
    }


    @DisplayName("포럼 게시글 상세 조회가 성공한다")
    @Test
    void getForum_Detail_By_Id_Success(){
        // given
        Long forumId = 1L;
        Forum existingForum = createExistingForum(forumId, "테스트 제목", "testuser");
        
        when(forumRepository.findById(forumId)).thenReturn(Optional.of(existingForum)); // 존재하는 포럼 반환

        // when
        ForumResponse result = forumService.getForumDetail(forumId);

        // then
        assertNotNull(result);
        assertEquals("테스트 제목", result.getTitle());
        assertEquals("testuser", result.getAuthorUsername());
        verify(forumRepository).findById(forumId);
    }

    @DisplayName("포럼 게시글 상세 조회가 잘못된 포럼Id 입력으로 실패한다")
    @Test
    void getForum_Detail_By_Id_Fail(){
        // given
        Long nonExistForumId = 9999L;

        when(forumRepository.findById(nonExistForumId)).thenReturn(Optional.empty()); // 존재하지 않는 포럼

        // when & then
        GlobalException exception = assertThrows(GlobalException.class,
            () -> forumService.getForumDetail(nonExistForumId));

        // 예외 메시지 확인
        assertEquals("그런 게시물 없습니다", exception.getMessage());
        assertEquals("FORUM_NOT_FOUND", exception.getErrorCode());
        
        // findById는 호출되어야 함 (예외 발생 전까지)
        verify(forumRepository).findById(nonExistForumId);
    }



}

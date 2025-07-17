package ImgBell.Kafka.Event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 📧 이메일 발송 이벤트 DTO
 * - Kafka를 통해 전달될 이메일 정보
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailEvent {
    
    /**
     * 수신자 이메일 주소
     */
    private String toEmail;
    
    /**
     * 수신자 이름 (닉네임)
     */
    private String toName;
    
    /**
     * 이메일 제목
     */
    private String subject;
    
    /**
     * 이메일 내용 (HTML 가능)
     */
    private String content;
    
    /**
     * 이메일 타입 (WELCOME, PASSWORD_RESET, NOTIFICATION 등)
     */
    private EmailType emailType;
    
    /**
     * 이메일 타입 열거형
     */
    public enum EmailType {
        WELCOME,           // 회원가입 환영 이메일
        PASSWORD_RESET,    // 비밀번호 재설정
        NOTIFICATION       // 일반 알림
    }
} 
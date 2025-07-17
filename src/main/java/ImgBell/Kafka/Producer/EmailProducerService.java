package ImgBell.Kafka.Producer;

import ImgBell.GlobalErrorHandler.GlobalException;
import ImgBell.Kafka.Event.EmailEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 📧 이메일 발송 Producer 서비스
 * - 이메일 이벤트를 Kafka 토픽으로 전송
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    /**
     * 회원가입 환영 이메일 발송 요청
     * 
     * @param userEmail 사용자 이메일
     * @param userName 사용자 이름 (닉네임)
     */
    public void sendWelcomeEmail(String userEmail, String userName) {
        // 입력값 유효성 검증
        if (userEmail == null || userEmail.trim().isEmpty()) {
            throw new GlobalException("이메일 주소가 필요합니다", "EMAIL_REQUIRED", HttpStatus.BAD_REQUEST);
        }
        
        if (userName == null || userName.trim().isEmpty()) {
            throw new GlobalException("사용자 이름이 필요합니다", "USERNAME_REQUIRED", HttpStatus.BAD_REQUEST);
        }
        
        EmailEvent emailEvent = new EmailEvent(
            userEmail,
            userName,
            "🎉 ImgBell에 오신 것을 환영합니다!",
            buildWelcomeEmailContent(userName),
            EmailEvent.EmailType.WELCOME
        );
        
        sendEmailEvent(emailEvent);
    }
    
    /**
     * 일반 이메일 발송 요청
     * 
     * @param emailEvent 이메일 이벤트 정보
     */
    public void sendEmailEvent(EmailEvent emailEvent) {
        // 이메일 이벤트 유효성 검증
        validateEmailEvent(emailEvent);
        
        try {
            log.info("📧 이메일 발송 요청: {} → {}", emailEvent.getEmailType(), emailEvent.getToEmail());
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send("email-sending", emailEvent);
            
            // 비동기 결과 처리
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("✅ 이메일 이벤트 전송 성공: {} → Topic: {}, Partition: {}, Offset: {}",
                        emailEvent.getToEmail(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                } else {
                    log.error("❌ 이메일 이벤트 전송 실패: {} → {}", emailEvent.getToEmail(), ex.getMessage());
                }
            });
            
        } catch (Exception e) {
            log.error("❌ 이메일 이벤트 전송 중 오류 발생: {}", e.getMessage(), e);
            throw new GlobalException(
                "이메일 발송 요청 처리 중 오류가 발생했습니다: " + e.getMessage(),
                "EMAIL_REQUEST_FAILED",
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
    
    /**
     * 이메일 이벤트 유효성 검증
     */
    private void validateEmailEvent(EmailEvent emailEvent) {
        if (emailEvent == null) {
            throw new GlobalException("이메일 이벤트가 null입니다", "EMAIL_EVENT_NULL", HttpStatus.BAD_REQUEST);
        }
        
        if (emailEvent.getToEmail() == null || emailEvent.getToEmail().trim().isEmpty()) {
            throw new GlobalException("수신자 이메일이 필요합니다", "EMAIL_RECIPIENT_REQUIRED", HttpStatus.BAD_REQUEST);
        }
        
        if (emailEvent.getEmailType() == null) {
            throw new GlobalException("이메일 타입이 필요합니다", "EMAIL_TYPE_REQUIRED", HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * 환영 이메일 HTML 내용 생성
     */
    private String buildWelcomeEmailContent(String userName) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h1 style="color: #4CAF50; text-align: center;">🎉 환영합니다!</h1>
                    <p><strong>%s</strong>님, ImgBell에 가입해주셔서 감사합니다!</p>
                    
                    <div style="background-color: #f9f9f9; padding: 15px; border-radius: 5px; margin: 20px 0;">
                        <h3>🚀 ImgBell에서 할 수 있는 것들:</h3>
                        <ul>
                            <li>📸 아름다운 이미지 업로드 및 공유</li>
                            <li>🤖 AI 이미지 분석 기능 체험</li>
                            <li>💬 다른 사용자들과 소통</li>
                            <li>🏆 인기 이미지 랭킹 확인</li>
                        </ul>
                    </div>
                    
                    <p style="text-align: center;">
                        <a href="https://imgbell.com" style="background-color: #4CAF50; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">
                            지금 시작하기
                        </a>
                    </p>
                    
                    <p style="text-align: center; color: #666; font-size: 12px; margin-top: 30px;">
                        ImgBell 팀 드림
                    </p>
                </div>
            </body>
            </html>
            """, userName);
    }
} 
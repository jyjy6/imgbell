package ImgBell.Kafka.Consumer;

import ImgBell.GlobalErrorHandler.GlobalException;
import ImgBell.Kafka.Event.EmailEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

/**
 * 📧 이메일 발송 Consumer 서비스
 * - Kafka에서 이메일 이벤트를 받아서 실제 이메일 발송 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailConsumerService {

    private final JavaMailSender mailSender;

    /**
     * 이메일 발송 토픽에서 메시지 수신 및 처리
     */
    @KafkaListener(
            topics = "${spring.kafka.topics.email-sending}",
            groupId = "${spring.kafka.email.consumer.group-id}"
    )
    public void handleEmailSending(
            @Payload EmailEvent emailEvent,
            Acknowledgment acknowledgment) {

        try {
            log.info("📨 이메일 발송 이벤트 수신: Type={}, To={}",
                    emailEvent.getEmailType(), emailEvent.getToEmail());

            // 실제 이메일 발송 처리
            sendActualEmail(emailEvent);

            // 수동 커밋 (이메일 발송 성공 시에만)
            acknowledgment.acknowledge();

            log.info("✅ 이메일 발송 완료: {} → {}", emailEvent.getEmailType(), emailEvent.getToEmail());

        } catch (Exception e) {
            log.error("❌ 이메일 발송 실패: {} → {}, 오류: {}",
                    emailEvent.getEmailType(), emailEvent.getToEmail(), e.getMessage(), e);

            // 실패 시 재시도 로직이나 DLQ(Dead Letter Queue)로 전송 가능
            // 여기서는 단순히 로그만 남김
        }
    }

    /**
     * 실제 이메일 발송 처리
     *
     * @param emailEvent 이메일 이벤트 정보
     */
    private void sendActualEmail(EmailEvent emailEvent) {
        // 이메일 이벤트 유효성 검증
        validateEmailEvent(emailEvent);

        // 이메일 타입별 처리
        switch (emailEvent.getEmailType()) {
            case WELCOME -> sendWelcomeEmail(emailEvent);
            case PASSWORD_RESET -> sendPasswordResetEmail(emailEvent);
            case NOTIFICATION -> sendNotificationEmail(emailEvent);
            default -> {
                log.warn("⚠️ 알 수 없는 이메일 타입: {}", emailEvent.getEmailType());
                throw new GlobalException(
                        "지원하지 않는 이메일 타입입니다: " + emailEvent.getEmailType(),
                        "UNSUPPORTED_EMAIL_TYPE",
                        HttpStatus.BAD_REQUEST
                );
            }
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

        if (emailEvent.getSubject() == null || emailEvent.getSubject().trim().isEmpty()) {
            throw new GlobalException("이메일 제목이 필요합니다", "EMAIL_SUBJECT_REQUIRED", HttpStatus.BAD_REQUEST);
        }

        if (emailEvent.getContent() == null || emailEvent.getContent().trim().isEmpty()) {
            throw new GlobalException("이메일 내용이 필요합니다", "EMAIL_CONTENT_REQUIRED", HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * 환영 이메일 발송
     */
    private void sendWelcomeEmail(EmailEvent emailEvent) {
        log.info("🎉 환영 이메일 발송 시작: {}", emailEvent.getToEmail());

        // TODO: 실제 이메일 발송 로직 구현
        realEmailSending(emailEvent);

        log.info("✅ 환영 이메일 발송 완료: {}", emailEvent.getToEmail());
    }

    /**
     * 비밀번호 재설정 이메일 발송
     */
    private void sendPasswordResetEmail(EmailEvent emailEvent) {
        log.info("🔐 비밀번호 재설정 이메일 발송 시작: {}", emailEvent.getToEmail());

        realEmailSending(emailEvent);

        log.info("✅ 비밀번호 재설정 이메일 발송 완료: {}", emailEvent.getToEmail());
    }

    /**
     * 일반 알림 이메일 발송
     */
    private void sendNotificationEmail(EmailEvent emailEvent) {
        log.info("🔔 알림 이메일 발송 시작: {}", emailEvent.getToEmail());

        realEmailSending(emailEvent);

        log.info("✅ 알림 이메일 발송 완료: {}", emailEvent.getToEmail());
    }

    /**
     * 실제 이메일 발송 (Gmail SMTP 사용)
     */
    private void realEmailSending(EmailEvent emailEvent) {
        try {
            // HTML 이메일 발송을 위한 MimeMessage 생성
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // 발신자 설정 (application.properties의 spring.mail.username 사용)
            helper.setFrom("ImgBell <noreply@imgbell.com>");

            // 수신자 설정
            helper.setTo(emailEvent.getToEmail());

            // 제목 설정
            helper.setSubject(emailEvent.getSubject());

            // HTML 내용 설정
            helper.setText(emailEvent.getContent(), true);

            // 실제 이메일 발송
            mailSender.send(message);

            log.info("""
                            📧 [실제 이메일 발송 완료]
                            받는 사람: {} ({})
                            제목: {}
                            내용 길이: {} 글자
                            타입: {}
                            """,
                    emailEvent.getToEmail(),
                    emailEvent.getToName(),
                    emailEvent.getSubject(),
                    emailEvent.getContent().length(),
                    emailEvent.getEmailType()
            );

        } catch (jakarta.mail.MessagingException e) {
            log.error("❌ 이메일 메시지 생성/발송 실패: {}", e.getMessage(), e);
            throw new GlobalException(
                    "이메일 발송 중 오류가 발생했습니다: " + e.getMessage(),
                    "EMAIL_SEND_FAILED",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        } catch (Exception e) {
            log.error("❌ 실제 이메일 발송 중 예상치 못한 오류: {}", e.getMessage(), e);
            throw new GlobalException(
                    "이메일 발송 중 시스템 오류가 발생했습니다",
                    "EMAIL_SYSTEM_ERROR",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
} 
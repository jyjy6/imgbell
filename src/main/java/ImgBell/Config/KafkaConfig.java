package ImgBell.Config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * �� Kafka 설정 클래스
 * - 이메일 발송 토픽 생성
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.topics.email-sending}")
    private String emailSendingTopicName;

    @Value("${spring.kafka.topics.es-sending}")
    private String esSendingTopicName;

    /**
     * 📧 이메일 발송 토픽 - 회원가입 환영 이메일 등
     */
    @Bean
    public NewTopic emailSendingTopic() {
        return TopicBuilder.name(emailSendingTopicName)
                .partitions(2)
                .replicas(1)
                .build();
    }

    /**
     * 📦 ES 연동 토픽 - 검색 색인용 메시지 처리
     */
    @Bean
    public NewTopic elasticSearchSendingTopic() {
        return TopicBuilder.name(esSendingTopicName)
                .partitions(2)
                .replicas(1)
                .build();
    }
}
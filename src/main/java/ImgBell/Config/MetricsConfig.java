package ImgBell.Config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 🔥 ImgBell 커스텀 메트릭 설정
 * Prometheus + Grafana 모니터링을 위한 애플리케이션별 메트릭 정의
 * 
 * 📊 메트릭 이름은 Prometheus/Grafana에서 보이는 실제 이름과 일치하도록 설정
 */
@Configuration
public class MetricsConfig {

    /**
     * 이미지 업로드 카운터 메트릭
     */
    @Bean
    public Counter imageUploadCounter(MeterRegistry meterRegistry) {
        return Counter.builder("imgbell_images_uploaded_total")
                .description("Total number of images uploaded")
                .register(meterRegistry);
    }

    /**
     * 이미지 다운로드 카운터 메트릭
     */
    @Bean
    public Counter imageDownloadCounter(MeterRegistry meterRegistry) {
        return Counter.builder("imgbell_images_downloaded_total")
                .description("Total number of images downloaded")
                .register(meterRegistry);
    }

    /**
     * 사용자 로그인 카운터 메트릭
     */
    @Bean
    public Counter userLoginCounter(MeterRegistry meterRegistry) {
        return Counter.builder("imgbell_users_login_total")
                .description("Total number of user logins")
                .register(meterRegistry);
    }

    /**
     * AI 이미지 분석 처리 시간 메트릭
     */
    @Bean
    public Timer aiAnalysisTimer(MeterRegistry meterRegistry) {
        return Timer.builder("imgbell_ai_analysis_duration_seconds")
                .description("Time taken for AI image analysis")
                .register(meterRegistry);
    }

    /**
     * Elasticsearch 검색 처리 시간 메트릭
     */
    @Bean
    public Timer searchTimer(MeterRegistry meterRegistry) {
        return Timer.builder("imgbell_search_duration_seconds")
                .description("Time taken for image search operations")
                .register(meterRegistry);
    }

    /**
     * 포럼 게시글 작성 카운터 메트릭
     */
    @Bean
    public Counter forumPostCounter(MeterRegistry meterRegistry) {
        return Counter.builder("imgbell_forum_posts_total")
                .description("Total number of forum posts created")
                .register(meterRegistry);
    }
} 
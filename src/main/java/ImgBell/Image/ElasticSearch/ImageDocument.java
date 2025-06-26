package ImgBell.Image.ElasticSearch;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "images")
public class ImageDocument {

    @Id
    private String id;

    // 기본 이미지 정보
    @Field(type = FieldType.Text, analyzer = "standard")
    private String imageName;

    @Field(type = FieldType.Keyword)
    private String imageUrl;

    @Field(type = FieldType.Keyword)
    private String fileType;

    // 업로더 정보
    @Field(type = FieldType.Long)
    private Long uploaderId;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String uploaderName;

    // 출처 정보
    @Field(type = FieldType.Text, analyzer = "standard")
    private String source;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String artist;

    // 통계 정보 (검색 정렬/필터링에 중요)
    @Field(type = FieldType.Integer)
    private Integer viewCount;

    @Field(type = FieldType.Integer)
    private Integer likeCount;

    // 등급 및 공개 설정
    @Field(type = FieldType.Keyword)
    private String imageGrade;

    @Field(type = FieldType.Boolean)
    private Boolean isPublic;

    // 타임스탬프 (날짜 범위 검색용)
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime createdAt;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime updatedAt;

    // 태그 정보 (검색의 핵심)
    @Field(type = FieldType.Nested)
    private List<TagDocument> tags;

    // 태그명만 별도로 저장 (빠른 검색용)
    @Field(type = FieldType.Text, analyzer = "standard")
    private List<String> tagNames;

    // 검색 최적화를 위한 추가 필드들

    // 전체 텍스트 검색용 (imageName + artist + tagNames 조합)
    @Field(type = FieldType.Text, analyzer = "standard")
    private String searchText;

    // 인기도 점수 (viewCount, likeCount 등을 조합한 점수)
    @Field(type = FieldType.Float)
    private Float popularityScore;


    // 내부 클래스: 태그 정보
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TagDocument {
        @Field(type = FieldType.Long)
        private Long id;

        @Field(type = FieldType.Text, analyzer = "standard")
        private String name;

        @Field(type = FieldType.Keyword)
        private String category;
    }

    // 검색 텍스트 생성 헬퍼 메소드
    public void generateSearchText() {
        StringBuilder sb = new StringBuilder();

        if (imageName != null) {
            sb.append(imageName).append(" ");
        }
        if (artist != null) {
            sb.append(artist).append(" ");
        }
        if (tagNames != null && !tagNames.isEmpty()) {
            sb.append(String.join(" ", tagNames));
        }

        this.searchText = sb.toString().trim();
    }

    // 인기도 점수 계산 헬퍼 메소드
    public void calculatePopularityScore() {
        // 가중치를 적용한 인기도 점수 계산
        int views = viewCount != null ? viewCount : 0;
        int likes = likeCount != null ? likeCount : 0;

        this.popularityScore = (float) ((views * 0.1) + (likes * 0.3));
    }


}
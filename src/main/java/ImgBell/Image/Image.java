package ImgBell.Image;


import ImgBell.Image.Comment.Comment;
import ImgBell.Image.Tag.Tag;
import ImgBell.Member.Member;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Image {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    private Long id;

    // 기본 이미지 정보
    @Column(updatable = false, nullable = false)
    private String imageUrl;

    private String imageName;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id")
    private Member uploader;
    // 업로더 이름을 별도로 저장 (회원 삭제 후에도 표시 가능)
    private String uploaderName;
    private String fileType;

    // 이미지 메타데이터
    private Integer width;    // 이미지 너비
    private Integer height;   // 이미지 높이
    private Long fileSize;    // 파일 크기 (바이트)

    // 출처 정보
    private String source;    // 원본 출처 URL
    private String artist;    // 아티스트/작가 이름

    // 인기도/통계 정보
    private Integer viewCount = 0;
    private Integer likeCount = 0;
    private Integer downloadCount = 0;

    // 컨텐츠 관리
    private String isAdult = "false";  // 성인물 여부
    private Boolean isPublic = true;  // 공개 여부
    private Boolean isApproved = false;  // 관리자 승인 여부

    // 타임스탬프
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // 태그 관계 (다대다)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            joinColumns = @JoinColumn(name = "image_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    // 댓글 관계 (일대다)
    @OneToMany(mappedBy = "image", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Comment> comments = new HashSet<>();


    // 생성자, getter, setter 등 필요한 메소드 추가

    // 태그 추가 헬퍼 메소드
    public void addTag(Tag tag) {
        tags.add(tag);
    }

    // 태그 제거 헬퍼 메소드
    public void removeTag(Tag tag) {
        tags.remove(tag);
    }
}

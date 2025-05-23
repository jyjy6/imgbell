package ImgBell.Forum;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Entity
public class Forum {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 50)
    private String authorDisplayName;

    @Column(nullable = false, length = 50)
    private String authorUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PostType type = PostType.NORMAL;

    @Column
    @Builder.Default
    private Integer viewCount = 0;

    @Column
    @Builder.Default
    private Integer likeCount = 0;


    @Column
    @Builder.Default
    private Boolean isDeleted = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // 댓글과의 관계 (양방향)
    @OneToMany(mappedBy = "forum", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ForumComment> comments;


    // 게시글 타입 enum
    public enum PostType {
        NOTICE("공지"),
        HOT("인기"),
        NORMAL("일반");

        private final String description;

        PostType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // 비즈니스 메서드들
    public void increaseViews() {
        this.viewCount++;
    }

    public void increaseLikes() {
        this.likeCount++;
    }

    public void markAsDeleted() {
        this.isDeleted = true;
    }

    public int getCommentCount() {
        return comments != null ? comments.size() : 0;
    }
}

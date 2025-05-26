package ImgBell.Forum.ForumComment;

import ImgBell.Forum.Forum;
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
public class ForumComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    private Long id;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 50)
    private String authorDisplayName;

    @Column(nullable = false, length = 50)
    private String authorUsername;

    @Column(nullable = false)
    @Builder.Default
    private Integer likeCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // 게시글과의 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forum_id", nullable = false)
    private Forum forum;

    // 부모 댓글과의 관계 (대댓글용)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private ForumComment parent;

    // 자식 댓글들과의 관계
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ForumComment> comments;

    // 비즈니스 메서드들
    public void increaseLikes() {
        this.likeCount++;
    }

    public void markAsDeleted() {
        this.isDeleted = true;
    }

    public boolean isReply() {
        return parent != null;
    }

    public int getReplyCount() {
        return comments != null ? comments.size() : 0;
    }
}
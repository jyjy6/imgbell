package ImgBell.Image.Tag;

import ImgBell.Image.Image;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
public class Tag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    // 태그 카테고리 (character, copyright, artist, general 등)
    private String category;

    // 태그 설명
    @Column(length = 1000)
    private String description;

    // 태그 사용 횟수
    private Integer usageCount = 0;

    // 이미지와의 관계 (다대다)
    @ManyToMany(mappedBy = "tags")
    private Set<Image> images = new HashSet<>();

}

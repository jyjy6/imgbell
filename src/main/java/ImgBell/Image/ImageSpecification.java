package ImgBell.Image;

import ImgBell.Image.Tag.Tag;
import ImgBell.ImageLike.ImageLike;
import ImgBell.Member.Member;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class ImageSpecification {

    public static Specification<Image> isPublic() {

        return (root, query, cb) -> cb.equal(root.get("isPublic"), true);
    }

    public static Specification<Image> hasTag(String tag) {
        return (root, query, cb) -> {
            if (tag == null || tag.isEmpty()) {
                return null;
            }
            Join<Image, Tag> tagJoin = root.join("tags", JoinType.INNER);
            return cb.equal(tagJoin.get("name"), tag);
        };
    }

    public static Specification<Image> hasGrade(String grade) {
        return (root, query, cb) -> {
            if (grade == null || grade.isEmpty()) {
                return null;
            }
            return cb.equal(root.get("imageGrade"), Image.ImageGrade.valueOf(grade));
        };
    }


    public static Specification<Image> hasImageName(String imageName) {
        return (root, query, cb) -> {
            if (imageName == null || imageName.isEmpty()) {
                return null;
            }
            return cb.like(cb.lower(root.get("imageName")), "%" + imageName.toLowerCase() + "%");
        };
    }


    public static Specification<Image> hasUploaderName(String uploaderName) {
        return (root, query, cb) -> {
            if (uploaderName == null || uploaderName.isEmpty()) {
                return null;
            }
            // Image 엔티티의 uploaderName 필드 직접 사용
            return cb.like(cb.lower(root.get("uploaderName")), "%" + uploaderName.toLowerCase() + "%");
        };
    }

    public static Specification<Image> hasArtist(String artist) {
        return (root, query, cb) -> {
            if (artist == null || artist.isEmpty()) {
                return null;
            }
            // Image 엔티티의 artist 필드 직접 사용
            return cb.like(cb.lower(root.get("artist")), "%" + artist.toLowerCase() + "%");
        };
    }

    public static Specification<Image> likedByMember(List<Long> likedImageIds) {
        return (root, query, cb) -> {
            if (likedImageIds == null || likedImageIds.isEmpty()) {
                return cb.disjunction(); // 빈 결과 반환
            }
            return root.get("id").in(likedImageIds);
        };
    }




    public static Specification<Image> searchAll(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isEmpty()) {
                return null;
            }
            query.distinct(true);

            String lowercaseKeyword = "%" + keyword.toLowerCase() + "%";

            // 이미지 이름 검색
            Predicate imageNamePredicate = cb.like(
                    cb.lower(root.get("imageName")),
                    lowercaseKeyword
            );

            // 업로더 이름 검색 - Image 엔티티의 uploaderName 필드 사용
            Predicate uploaderNamePredicate = cb.like(
                    cb.lower(root.get("uploaderName")),
                    lowercaseKeyword
            );

            // 아티스트 검색 - Image엔티티의 artist 필드 사용
            Predicate artistPredicate = cb.like(
                    cb.lower(root.get("artist")),
                    lowercaseKeyword
            );

            // 태그 검색
            Join<Image, Tag> tagJoin = root.join("tags", JoinType.LEFT);
            Predicate tagPredicate = cb.like(
                    cb.lower(tagJoin.get("name")),
                    lowercaseKeyword
            );

            // OR 조건으로 4개의 조건 결합
            return cb.or(
                    imageNamePredicate,
                    uploaderNamePredicate,
                    artistPredicate,
                    tagPredicate
            );
        };
    }

}
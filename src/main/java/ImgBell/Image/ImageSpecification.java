package ImgBell.Image;

import ImgBell.Image.Tag.Tag;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

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
}
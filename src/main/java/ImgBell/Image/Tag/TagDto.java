package ImgBell.Image.Tag;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;



@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TagDto {
    private String name;
    private String category;
    private String description;
    private Integer usageCount;
}

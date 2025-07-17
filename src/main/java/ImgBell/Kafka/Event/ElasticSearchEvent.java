package ImgBell.Kafka.Event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ElasticSearchEvent {
    private Long imageId;
    private String action; // "sync" or "delete"
}

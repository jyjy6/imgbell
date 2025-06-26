package ImgBell.Image.ElasticSearch;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ImageSearchRepository extends ElasticsearchRepository<ImageDocument, String> {
}

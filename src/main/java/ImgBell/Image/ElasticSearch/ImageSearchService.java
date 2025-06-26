package ImgBell.Image.ElasticSearch;


import co.elastic.clients.elasticsearch._types.FieldValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageSearchService {

    private final ImageSearchRepository imageSearchRepository;
    private final ElasticsearchTemplate elasticsearchTemplate;

    /**
     * 🎯 스마트 이미지 검색
     * - 이미지명, 작가명, 태그명에서 검색
     * - 오타 허용, 부분 검색 지원
     * - 인기도 기반 정렬
     */
    public List<ImageDocument> smartSearch(String keyword) {
        return smartSearch(keyword, null, null, 20);
    }

    public List<ImageDocument> smartSearch(String keyword, String imageGrade, Boolean isPublic, int size) {
        try {
            log.info("🚀 이미지 검색 시작: keyword={}, grade={}, public={}", keyword, imageGrade, isPublic);

            Query query = NativeQuery.builder()
                    .withQuery(q -> q.bool(b -> {
                        // 🎯 키워드 검색 조건들 (should = OR 조건)
                        b.should(s -> s
                                        .multiMatch(m -> m
                                                .query(keyword)
                                                .fields("imageName^3", "artist^2", "searchText^1.5") // 🎯 이미지명이 가장 중요
                                                .boost(3.0f)
                                        )
                                )
                                .should(s -> s
                                        .multiMatch(m -> m
                                                .query(keyword)
                                                .fields("imageName^2", "artist^1.5", "uploaderName^1")
                                                .fuzziness("AUTO") // 🎯 오타 허용
                                                .boost(2.0f)
                                        )
                                )
                                // 🏷️ 태그 검색 (Nested 쿼리)
                                .should(s -> s
                                        .nested(n -> n
                                                .path("tags")
                                                .query(nq -> nq
                                                        .multiMatch(tm -> tm
                                                                .query(keyword)
                                                                .fields("tags.name^2", "tags.category^1")
                                                        )
                                                )
                                                .boost(2.5f) // 🎯 태그 매치는 높은 점수
                                        )
                                )
                                // 🔍 태그명 직접 검색 (빠른 검색용)
                                .should(s -> s
                                        .match(m -> m
                                                .field("tagNames")
                                                .query(keyword)
                                                .boost(2.0f)
                                        )
                                )
                                // 🌟 부분 검색 (와일드카드)
                                .should(s -> s
                                        .wildcard(w -> w
                                                .field("imageName")
                                                .value("*" + keyword.toLowerCase() + "*")
                                                .boost(1.0f)
                                        )
                                )
                                .minimumShouldMatch("1"); // 최소 하나는 매치

                        // 🎯 필수 조건들 (must = AND 조건)
                        if (imageGrade != null) {
                            b.must(m -> m.term(t -> t.field("imageGrade").value(imageGrade)));
                        }
                        if (isPublic != null) {
                            b.must(m -> m.term(t -> t.field("isPublic").value(isPublic)));
                        }
                        // 승인된 이미지만

                        return b;
                    }))
                    .withSort(Sort.by(Sort.Direction.DESC, "_score")) // 🏆 관련도 순
                    .withSort(Sort.by(Sort.Direction.DESC, "popularityScore")) // 🌟 인기도 순
                    .withSort(Sort.by(Sort.Direction.DESC, "createdAt")) // 📅 최신순
                    .withMaxResults(size)
                    .build();

            SearchHits<ImageDocument> searchHits = elasticsearchTemplate.search(query, ImageDocument.class);

            List<ImageDocument> results = searchHits.stream()
                    .map(hit -> {
                        ImageDocument image = hit.getContent();
                        log.info("🎯 점수: {:.2f} - 이미지: {} (작가: {})",
                                hit.getScore(), image.getImageName(), image.getArtist());
                        return image;
                    })
                    .collect(Collectors.toList());

            log.info("🚀 이미지 검색 완료: {} 개 결과", results.size());
            return results;

        } catch (Exception e) {
            log.error("🚨 이미지 검색 실패: ", e);
            return new ArrayList<>();
        }
    }

    /**
     * 🏷️ 태그 기반 검색
     */
    public List<ImageDocument> searchByTags(List<String> tagNames, int size) {
        try {
            log.info("🏷️ 태그 검색: {}", tagNames);

            Query query = NativeQuery.builder()
                    .withQuery(q -> q.bool(b -> b
                            .must(m -> m.terms(t -> t.field("tagNames").terms(terms -> terms.value(
                                    tagNames.stream().map(FieldValue::of).collect(Collectors.toList())
                            ))))
                            .must(m -> m.term(t -> t.field("isPublic").value(true)))
                    ))
                    .withSort(Sort.by(Sort.Direction.DESC, "popularityScore"))
                    .withSort(Sort.by(Sort.Direction.DESC, "likeCount"))
                    .withMaxResults(size)
                    .build();

            SearchHits<ImageDocument> searchHits = elasticsearchTemplate.search(query, ImageDocument.class);
            return searchHits.stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("🚨 태그 검색 실패: ", e);
            return new ArrayList<>();
        }
    }

    /**
     * 🔥 인기 이미지 검색
     */
    public List<ImageDocument> getPopularImages(int size) {
        try {
            Query query = NativeQuery.builder()
                    .withQuery(q -> q.bool(b -> b
                            .must(m -> m.term(t -> t.field("isPublic").value(true)))
                    ))
                    .withSort(Sort.by(Sort.Direction.DESC, "popularityScore"))
                    .withSort(Sort.by(Sort.Direction.DESC, "viewCount"))
                    .withMaxResults(size)
                    .build();

            SearchHits<ImageDocument> searchHits = elasticsearchTemplate.search(query, ImageDocument.class);
            return searchHits.stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("🚨 인기 이미지 검색 실패: ", e);
            return new ArrayList<>();
        }
    }

    /**
     * 🆕 최신 이미지 검색
     */
    public List<ImageDocument> getRecentImages(int size) {
        try {
            Query query = NativeQuery.builder()
                    .withQuery(q -> q.bool(b -> b
                            .must(m -> m.term(t -> t.field("isPublic").value(true)))
                    ))
                    .withSort(Sort.by(Sort.Direction.DESC, "createdAt"))
                    .withMaxResults(size)
                    .build();

            SearchHits<ImageDocument> searchHits = elasticsearchTemplate.search(query, ImageDocument.class);
            return searchHits.stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("🚨 최신 이미지 검색 실패: ", e);
            return new ArrayList<>();
        }
    }

    /**
     * 👤 특정 업로더의 이미지 검색
     */
    public List<ImageDocument> searchByUploader(Long uploaderId, int size) {
        try {
            Query query = NativeQuery.builder()
                    .withQuery(q -> q.bool(b -> b
                            .must(m -> m.term(t -> t.field("uploaderId").value(uploaderId)))
                            .must(m -> m.term(t -> t.field("isPublic").value(true)))
                    ))
                    .withSort(Sort.by(Sort.Direction.DESC, "createdAt"))
                    .withMaxResults(size)
                    .build();

            SearchHits<ImageDocument> searchHits = elasticsearchTemplate.search(query, ImageDocument.class);
            return searchHits.stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("🚨 업로더별 검색 실패: ", e);
            return new ArrayList<>();
        }
    }

    /**
     * 🔍 자동완성 (이미지명 기반)
     */
    public List<String> autoComplete(String prefix, int size) {
        try {
            Query query = NativeQuery.builder()
                    .withQuery(q -> q.bool(b -> b
                            .must(m -> m.prefix(p -> p
                                    .field("imageName")
                                    .value(prefix.toLowerCase())
                            ))
                            .must(m -> m.term(t -> t.field("isPublic").value(true)))
                    ))
                    .withSort(Sort.by(Sort.Direction.DESC, "popularityScore"))
                    .withMaxResults(size)
                    .build();

            SearchHits<ImageDocument> searchHits = elasticsearchTemplate.search(query, ImageDocument.class);
            return searchHits.stream()
                    .map(hit -> hit.getContent().getImageName())
                    .distinct()
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("🚨 자동완성 실패: ", e);
            return new ArrayList<>();
        }
    }
}

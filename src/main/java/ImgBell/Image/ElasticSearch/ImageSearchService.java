package ImgBell.Image.ElasticSearch;


import co.elastic.clients.elasticsearch._types.FieldValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    private final ElasticsearchTemplate elasticsearchTemplate; // Spring Data Elasticsearch의 핵심 클래스로 ES 작업을 수행

    /**
     * 🎯 스마트 이미지 검색 (페이지네이션 지원)
     * - 이미지명, 작가명, 태그명에서 검색
     * - 오타 허용, 부분 검색 지원
     * - 인기도 기반 정렬
     * - 페이지네이션 지원
     */
    public Page<ImageDocument> smartSearch(String keyword, String imageGrade, Boolean isPublic, int page, int size) {
        try {
            log.info("🚀 이미지 검색 시작: keyword={}, grade={}, public={}, page={}, size={}", 
                    keyword, imageGrade, isPublic, page, size);
            
            // Pageable 객체 생성
            Pageable pageable = PageRequest.of(page, size);
            
            // NativeQuery: Elasticsearch의 네이티브 쿼리를 Java로 작성할 수 있게 해주는 Spring Data Elasticsearch 클래스
            Query query = NativeQuery.builder()
                    // withQuery(): 검색 쿼리를 정의하는 메서드
                    // q -> q.bool(): 람다식으로 BoolQuery를 생성 (q는 QueryBuilders의 인스턴스)
                    .withQuery(q -> q.bool(b -> {
                        /**
                         * bool 쿼리 설명:
                         * - Elasticsearch의 복합 쿼리로 여러 조건을 조합
                         * - must: AND 조건 (반드시 만족해야 함)
                         * - should: OR 조건 (하나 이상 만족하면 됨, 점수에 영향)
                         * - must_not: NOT 조건 (만족하지 않아야 함)
                         * - filter: 점수에 영향 없이 필터링만
                         */
                        
                        // should(): OR 조건들을 정의 (여러 조건 중 하나라도 만족하면 됨)
                        // s -> s.multiMatch(): 람다식으로 MultiMatchQuery 생성 (s는 QueryBuilders)
                        b.should(s -> s.multiMatch(m -> m
                                        /**
                                         * multiMatch 쿼리 설명:
                                         * - 하나의 검색어로 여러 필드를 동시에 검색
                                         * - fields()에서 ^숫자는 boost 값 (가중치)
                                         * - imageName^3: 이미지명 매치시 점수 3배
                                         * - artist^2: 작가명 매치시 점수 2배  
                                         * - searchText^1.5: 검색텍스트 매치시 점수 1.5배
                                         */
                                        .query(keyword) // 검색할 키워드
                                        .fields("imageName^3", "artist^2", "searchText^1.5") // 검색 대상 필드들과 가중치
                                        .boost(3.0f) // 이 쿼리 전체의 점수를 3배로 증가
                                ))
                                // 두 번째 should 조건: 오타 허용 검색
                                .should(s -> s
                                        .multiMatch(m -> m
                                                .query(keyword)
                                                .fields("imageName^2", "artist^1.5", "uploaderName^1")
                                                .fuzziness("AUTO") // 🎯 오타 허용: AUTO는 문자열 길이에 따라 자동으로 편집거리 설정
                                                .boost(2.0f)
                                        )
                                )
                                // 🏷️ 태그 검색 (Nested 쿼리)
                                .should(s -> s
                                        /**
                                         * nested 쿼리 설명:
                                         * - 중첩된 객체(배열) 내에서 검색할 때 사용
                                         * - tags는 ImageDocument 내의 중첩된 Tag 객체들
                                         * - path("tags"): 중첩 객체의 경로 지정
                                         */
                                        .nested(n -> n
                                                .path("tags") // 중첩 객체 경로
                                                .query(nq -> nq // 중첩 객체 내에서 실행할 쿼리
                                                        .multiMatch(tm -> tm
                                                                .query(keyword)
                                                                .fields("tags.name^2", "tags.category^1"))
                                                )
                                                .boost(2.5f) // 🎯 태그 매치는 높은 점수
                                        )
                                )
                                // 🔍 태그명 직접 검색 (빠른 검색용)
                                .should(s -> s
                                        /**
                                         * match 쿼리 설명:
                                         * - 단일 필드에서 텍스트 검색
                                         * - 분석기를 통해 텍스트를 분석한 후 검색
                                         * - tagNames는 태그명들을 평면화한 필드
                                         */
                                        .match(m -> m
                                                .field("tagNames")
                                                .query(keyword)
                                                .boost(2.0f)
                                        )
                                )
                                // 🌟 부분 검색 (와일드카드)
                                .should(s -> s
                                        /**
                                         * wildcard 쿼리 설명:
                                         * - 와일드카드(*,?) 패턴을 사용한 검색
                                         * - *keyword*: 키워드를 포함하는 모든 문자열 매치
                                         * - 성능상 느릴 수 있으므로 신중히 사용
                                         */
                                        .wildcard(w -> w
                                                .field("imageName")
                                                .value("*" + keyword.toLowerCase() + "*")
                                                .boost(1.0f)
                                        )
                                )
                                /**
                                 * minimumShouldMatch 설명:
                                 * - should 조건 중 최소 몇 개가 만족되어야 하는지 지정
                                 * - "1": 최소 1개의 should 조건이 만족되어야 함
                                 * - 숫자 또는 퍼센트로 지정 가능
                                 */
                                .minimumShouldMatch("1"); // 최소 하나는 매치

                        /**
                         * must(): AND 조건들을 정의 (모든 조건이 반드시 만족되어야 함)
                         * term 쿼리: 정확한 값 매치 (분석되지 않은 키워드 검색)
                         */
                        // 🎯 필수 조건들 (must = AND 조건)
                        if (imageGrade != null) {
                            b.must(m -> m.term(t -> t.field("imageGrade").value(imageGrade)));
                        }
                        if (isPublic != null) {
                            b.must(m -> m.term(t -> t.field("isPublic").value(isPublic)));
                        }

                        return b; // BoolQuery.Builder 반환
                    }))
                    /**
                     * withSort(): 정렬 조건들을 정의
                     * - _score: Elasticsearch가 계산한 관련도 점수
                     * - DESC: 내림차순 정렬
                     * - 여러 정렬 조건을 체이닝하면 1차, 2차, 3차 정렬 기준이 됨
                     */
                    .withSort(Sort.by(Sort.Direction.DESC, "_score")) // 🏆 관련도 순 (1차)
                    .withSort(Sort.by(Sort.Direction.DESC, "popularityScore")) // 🌟 인기도 순 (2차)
                    .withSort(Sort.by(Sort.Direction.DESC, "createdAt")) // 📅 최신순 (3차)
                    .withPageable(pageable) // 페이지네이션 적용
                    .build(); // Query 객체 생성

            // elasticsearchTemplate.search(): 실제 검색 실행
            SearchHits<ImageDocument> searchHits = elasticsearchTemplate.search(query, ImageDocument.class);

            // Stream API를 사용하여 검색 결과를 처리
            List<ImageDocument> results = searchHits.stream()
                    .map(hit -> {
                        ImageDocument image = hit.getContent(); // 검색된 문서 내용
                        log.info("🎯 점수: {:.2f} - 이미지: {} (작가: {})",
                                hit.getScore(), image.getImageName(), image.getArtist()); // 검색 점수 로깅
                        return image;
                    })
                    .collect(Collectors.toList());

            // Page 객체 생성 (전체 결과 수는 searchHits.getTotalHits()에서 가져옴)
            Page<ImageDocument> resultPage = new PageImpl<>(results, pageable, searchHits.getTotalHits());

            log.info("🚀 이미지 검색 완료: {} 개 결과 (전체: {}, 페이지: {}/{})", 
                    results.size(), searchHits.getTotalHits(), page + 1, resultPage.getTotalPages());
            
            return resultPage;

        } catch (Exception e) {
            log.error("🚨 이미지 검색 실패: ", e);
            return new PageImpl<>(new ArrayList<>(), PageRequest.of(page, size), 0);
        }
    }

    /**
     * 🏷️ 태그 기반 검색 (페이지네이션 지원)
     * terms 쿼리를 사용하여 여러 태그 중 하나라도 포함된 이미지 검색
     */
    public Page<ImageDocument> searchByTags(List<String> tagNames, int page, int size) {
        try {
            log.info("🏷️ 태그 검색: {}, page={}, size={}", tagNames, page, size);

            Pageable pageable = PageRequest.of(page, size);

            Query query = NativeQuery.builder()
                    .withQuery(q -> q.bool(b -> b
                            /**
                             * terms 쿼리 설명:
                             * - 하나의 필드에서 여러 값 중 하나라도 일치하는 문서 검색
                             * - SQL의 IN 절과 유사
                             * - tagNames.stream().map(FieldValue::of): 문자열을 FieldValue로 변환
                             */
                            .must(m -> m.terms(t -> t.field("tagNames").terms(terms -> terms.value(
                                    tagNames.stream().map(FieldValue::of).collect(Collectors.toList())
                            ))))
                            .must(m -> m.term(t -> t.field("isPublic").value(true))) // 공개 이미지만
                    ))
                    .withSort(Sort.by(Sort.Direction.DESC, "popularityScore"))
                    .withSort(Sort.by(Sort.Direction.DESC, "likeCount"))
                    .withPageable(pageable)
                    .build();

            SearchHits<ImageDocument> searchHits = elasticsearchTemplate.search(query, ImageDocument.class);
            List<ImageDocument> results = searchHits.stream()
                    .map(SearchHit::getContent) // 메서드 참조로 검색 결과에서 문서 내용만 추출
                    .collect(Collectors.toList());

            return new PageImpl<>(results, pageable, searchHits.getTotalHits());

        } catch (Exception e) {
            log.error("🚨 태그 검색 실패: ", e);
            return new PageImpl<>(new ArrayList<>(), PageRequest.of(page, size), 0);
        }
    }

    /**
     * 🔥 인기 이미지 검색 (페이지네이션 지원)
     * 단순한 필터링과 정렬만 사용
     */
    public Page<ImageDocument> getPopularImages(int page, int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);

            Query query = NativeQuery.builder()
                    .withQuery(q -> q.bool(b -> b
                            .must(m -> m.term(t -> t.field("isPublic").value(true))) // 공개 이미지만 필터링
                    ))
                    .withSort(Sort.by(Sort.Direction.DESC, "popularityScore")) // 인기도 순 정렬
                    .withSort(Sort.by(Sort.Direction.DESC, "viewCount")) // 조회수 순 정렬
                    .withPageable(pageable)
                    .build();

            SearchHits<ImageDocument> searchHits = elasticsearchTemplate.search(query, ImageDocument.class);
            List<ImageDocument> results = searchHits.stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());

            return new PageImpl<>(results, pageable, searchHits.getTotalHits());

        } catch (Exception e) {
            log.error("🚨 인기 이미지 검색 실패: ", e);
            return new PageImpl<>(new ArrayList<>(), PageRequest.of(page, size), 0);
        }
    }

    /**
     * 🆕 최신 이미지 검색 (페이지네이션 지원)
     * 생성일시 기준 내림차순 정렬
     */
    public Page<ImageDocument> getRecentImages(int page, int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);

            Query query = NativeQuery.builder()
                    .withQuery(q -> q.bool(b -> b
                            .must(m -> m.term(t -> t.field("isPublic").value(true)))
                    ))
                    .withSort(Sort.by(Sort.Direction.DESC, "createdAt")) // 최신순 정렬
                    .withPageable(pageable)
                    .build();

            SearchHits<ImageDocument> searchHits = elasticsearchTemplate.search(query, ImageDocument.class);
            List<ImageDocument> results = searchHits.stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());

            return new PageImpl<>(results, pageable, searchHits.getTotalHits());

        } catch (Exception e) {
            log.error("🚨 최신 이미지 검색 실패: ", e);
            return new PageImpl<>(new ArrayList<>(), PageRequest.of(page, size), 0);
        }
    }

    /**
     * 👤 특정 업로더의 이미지 검색 (페이지네이션 지원)
     * uploaderId로 필터링하여 특정 사용자가 업로드한 이미지만 검색
     */
    public Page<ImageDocument> searchByUploader(Long uploaderId, int page, int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);

            Query query = NativeQuery.builder()
                    .withQuery(q -> q.bool(b -> b
                            .must(m -> m.term(t -> t.field("uploaderId").value(uploaderId))) // 특정 업로더 필터링
                            .must(m -> m.term(t -> t.field("isPublic").value(true)))
                    ))
                    .withSort(Sort.by(Sort.Direction.DESC, "createdAt"))
                    .withPageable(pageable)
                    .build();

            SearchHits<ImageDocument> searchHits = elasticsearchTemplate.search(query, ImageDocument.class);
            List<ImageDocument> results = searchHits.stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());

            return new PageImpl<>(results, pageable, searchHits.getTotalHits());

        } catch (Exception e) {
            log.error("🚨 업로더별 검색 실패: ", e);
            return new PageImpl<>(new ArrayList<>(), PageRequest.of(page, size), 0);
        }
    }

    /**
     * 🔍 자동완성 (이미지명 기반)
     * prefix 쿼리를 사용하여 입력한 문자로 시작하는 이미지명들 검색
     */
    public List<String> autoComplete(String prefix, int size) {
        try {
            Query query = NativeQuery.builder()
                    .withQuery(q -> q.bool(b -> b
                            /**
                             * prefix 쿼리 설명:
                             * - 지정한 접두사로 시작하는 텍스트를 검색
                             * - 자동완성 기능에 주로 사용
                             * - 검색 성능이 좋음
                             */
                            .must(m -> m.prefix(p -> p
                                    .field("imageName")
                                    .value(prefix.toLowerCase()) // 소문자로 변환하여 대소문자 구분 없이 검색
                            ))
                            .must(m -> m.term(t -> t.field("isPublic").value(true)))
                    ))
                    .withSort(Sort.by(Sort.Direction.DESC, "popularityScore")) // 인기도 순으로 자동완성 제안
                    .withMaxResults(size)
                    .build();

            SearchHits<ImageDocument> searchHits = elasticsearchTemplate.search(query, ImageDocument.class);
            return searchHits.stream()
                    .map(hit -> hit.getContent().getImageName()) // 이미지명만 추출
                    .distinct() // 중복 제거
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("🚨 자동완성 실패: ", e);
            return new ArrayList<>();
        }
    }
}

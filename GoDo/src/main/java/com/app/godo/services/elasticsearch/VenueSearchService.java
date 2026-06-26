package com.app.godo.services.elasticsearch;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.json.JsonData;
import com.app.godo.dtos.search.SearchHighlightDto;
import com.app.godo.dtos.search.VenueSearchQueryDto;
import com.app.godo.dtos.search.VenueSearchResultDto;
import com.app.godo.models.es.VenueSearchDocument;
import com.app.godo.utils.SerbianNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VenueSearchService {

    private final ElasticsearchOperations elasticsearchOperations;


    public Page<VenueSearchResultDto> search(VenueSearchQueryDto query, Pageable pageable) {

        // Step 1: Build the main boolean query
        BoolQuery.Builder mainQuery = new BoolQuery.Builder();
        boolean hasCriteria = false;

        // Step 2: Add text search conditions (name, description, pdfDescription)
        if (hasValue(query.getName())) {
            addTextQuery(mainQuery, "name", query.getName(), query.getOperator());
            hasCriteria = true;
        }

        if (hasValue(query.getDescription())) {
            addTextQuery(mainQuery, "description", query.getDescription(), query.getOperator());
            hasCriteria = true;
        }

        if (hasValue(query.getPdfDescription())) {
            addTextQuery(mainQuery, "pdfDescription", query.getPdfDescription(), query.getOperator());
            hasCriteria = true;
        }

        // Step 3: Add range queries (review count, rating)
        if (query.getMinReviews() != null || query.getMaxReviews() != null) {
            addRangeQuery(mainQuery, "reviewCount",
                    query.getMinReviews() != null ? query.getMinReviews().doubleValue() : null,
                    query.getMaxReviews() != null ? query.getMaxReviews().doubleValue() : null);
            hasCriteria = true;
        }

        if (query.getMinRating() != null || query.getMaxRating() != null) {
            String ratingField = getRatingField(query.getRatingCategory());
            addRangeQuery(mainQuery, ratingField, query.getMinRating(), query.getMaxRating());
            hasCriteria = true;
        }

        // Step 4: If no criteria provided, return all venues
        co.elastic.clients.elasticsearch._types.query_dsl.Query finalQuery;
        if (hasCriteria) {
            finalQuery = mainQuery.build()._toQuery();
        } else {
            finalQuery = QueryBuilders.matchAll(m -> m);
        }

        // Step 5: Build the NativeQuery with pagination, sorting, and highlighting
        NativeQueryBuilder nativeQueryBuilder = NativeQuery.builder()
                .withQuery(finalQuery)
                .withPageable(pageable);

        // Add sorting
        if (pageable.getSort().isSorted()) {
            pageable.getSort().forEach(order -> {
                String sortField = order.getProperty().equals("name") ? "name_keyword" : order.getProperty();
                SortOrder direction = order.isAscending() ? SortOrder.Asc : SortOrder.Desc;
                nativeQueryBuilder.withSort(s -> s.field(f -> f.field(sortField).order(direction)));
            });
        }

        // Add highlighting - wraps matched text in <em> tags
        // so the frontend can show WHERE the search term was found
        Highlight highlight = new Highlight(
                List.of(
                        new HighlightField("name"),
                        new HighlightField("description"),
                        new HighlightField("pdfDescription")
                )
        );
        nativeQueryBuilder.withHighlightQuery(new HighlightQuery(highlight, null));

        NativeQuery nativeQuery = nativeQueryBuilder.build();

        // Step 6: Execute the search
        SearchHits<VenueSearchDocument> searchHits =
                elasticsearchOperations.search(nativeQuery, VenueSearchDocument.class);

        // Step 7: Convert results to DTOs with highlights
        List<VenueSearchResultDto> results = searchHits.getSearchHits().stream()
                .map(this::mapToResultDto)
                .toList();

        return PageableExecutionUtils.getPage(results, pageable, searchHits::getTotalHits);
    }

    // ========== MORE LIKE THIS ==========

    public List<VenueSearchResultDto> moreLikeThis(Long venueId) {
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q
                        .moreLikeThis(mlt -> mlt
                                .fields("name", "description", "pdfDescription")
                                .like(l -> l
                                        .document(d -> d
                                                .index("godo_venues")
                                                .id(String.valueOf(venueId))
                                        )
                                )
                                .minTermFreq(1)
                                .maxQueryTerms(25)
                                .minDocFreq(1)
                        )
                )
                .build();

        SearchHits<VenueSearchDocument> searchHits =
                elasticsearchOperations.search(query, VenueSearchDocument.class);

        return searchHits.getSearchHits().stream()
                .map(this::mapToResultDto)
                .toList();
    }

    // helpers
    private void addTextQuery(BoolQuery.Builder boolQuery, String field,
                              String value, String operator) {

        co.elastic.clients.elasticsearch._types.query_dsl.Query textQuery;
        String trimmed = value.trim();

        if (isPhrase(trimmed)) {
            String phrase = trimmed.substring(1, trimmed.length() - 1);
            textQuery = QueryBuilders.matchPhrase(m -> m.field(field).query(phrase));

        } else if (isPrefix(trimmed)) {
            String prefix = SerbianNormalizer.normalize(trimmed.substring(0, trimmed.length() - 1));
            textQuery = QueryBuilders.prefix(p -> p.field(field).value(prefix));

        } else if (isFuzzy(trimmed)) {
            String term = SerbianNormalizer.normalize(trimmed.substring(1));
            textQuery = QueryBuilders.fuzzy(f -> f.field(field).value(term).fuzziness("AUTO"));

        } else {
            textQuery = QueryBuilders.match(m -> m.field(field).query(trimmed));
        }

        // AND = must match ALL conditions
        // OR  = must match AT LEAST ONE condition
        if ("OR".equalsIgnoreCase(operator)) {
            boolQuery.should(textQuery);
            boolQuery.minimumShouldMatch("1");
        } else {
            boolQuery.must(textQuery);
        }
    }


    private void addRangeQuery(BoolQuery.Builder boolQuery, String field,
                               Double min, Double max) {
        boolQuery.filter(QueryBuilders.range(r -> r
                .number(n -> {
                    n.field(field);
                    if (min != null) n.gte(min);
                    if (max != null) n.lte(max);
                    return n;
                })
        ));
    }

    private String getRatingField(String category) {
        if (category == null) return "averageRating";

        return switch (category.toLowerCase()) {
            case "performance" -> "performanceRating";
            case "ambient" -> "ambientRating";
            case "venue" -> "venueRating";
            case "overallimpression" -> "overallImpressionRating";
            default -> "averageRating";
        };
    }

    private VenueSearchResultDto mapToResultDto(SearchHit<VenueSearchDocument> hit) {
        VenueSearchResultDto dto = VenueSearchResultDto.fromDocument(hit.getContent());

        // Build highlight snippets WITH a label for which field matched.
        // getHighlightFields() returns a map: field name -> list of snippets
        List<SearchHighlightDto> highlights = new ArrayList<>();
        hit.getHighlightFields().forEach((field, snippets) -> {
            String label = friendlyFieldName(field);
            snippets.forEach(snippet -> highlights.add(new SearchHighlightDto(label, snippet)));
        });
        dto.setHighlights(highlights);

        return dto;
    }

    // Maps the internal Elasticsearch field name to a user-friendly label
    private String friendlyFieldName(String field) {
        return switch (field) {
            case "name" -> "Name";
            case "description" -> "Description";
            case "pdfDescription" -> "PDF";
            default -> field;
        };
    }

    // ========== QUERY TYPE DETECTION ==========

    private boolean isPhrase(String value) {
        return value.startsWith("\"") && value.endsWith("\"") && value.length() > 1;
    }

    private boolean isPrefix(String value) {
        return value.endsWith("*") && value.length() > 1;
    }

    private boolean isFuzzy(String value) {
        return value.startsWith("~") && value.length() > 1;
    }

    private boolean hasValue(String value) {
        return value != null && !value.trim().isEmpty();
    }
}

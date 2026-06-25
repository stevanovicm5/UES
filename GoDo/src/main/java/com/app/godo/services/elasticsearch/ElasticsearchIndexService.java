package com.app.godo.services.elasticsearch;

import com.app.godo.models.es.VenueSearchDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchIndexService {

    private final ElasticsearchOperations elasticsearchOperations;

    public void createIndexIfNotExists() {
        IndexOperations indexOps = elasticsearchOperations
                .indexOps(VenueSearchDocument.class);

        if (!indexOps.exists()) {
            indexOps.createWithMapping();
            log.info("Created Elasticsearch index 'godo_venues' with Serbian analyzer");
        } else {
            log.info("Elasticsearch index 'godo_venues' already exists");
        }
    }

    public void deleteIndex() {
        IndexOperations indexOps = elasticsearchOperations
                .indexOps(VenueSearchDocument.class);

        if (indexOps.exists()) {
            indexOps.delete();
            log.info("Deleted Elasticsearch index 'godo_venues'");
        }
    }

    public boolean indexExists() {
        return elasticsearchOperations
                .indexOps(VenueSearchDocument.class)
                .exists();
    }
}

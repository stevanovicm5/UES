package com.app.godo.initialization;

import com.app.godo.services.elasticsearch.ElasticsearchIndexService;
import com.app.godo.services.elasticsearch.VenueElasticsearchSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchInitializer {

    private final ElasticsearchIndexService indexService;
    private final VenueElasticsearchSyncService syncService;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeElasticsearch() {
        try {
            log.info("Starting Elasticsearch initialization...");

            indexService.createIndexIfNotExists();

            syncService.indexAllVenues();

            log.info("Elasticsearch initialization completed successfully");
        } catch (Exception e) {
            log.error("Failed to initialize Elasticsearch: {}", e.getMessage());
        }
    }
}

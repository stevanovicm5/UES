package com.app.godo.repositories.elasticsearch;

import com.app.godo.models.es.VenueSearchDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VenueElasticsearchRepository
        extends ElasticsearchRepository<VenueSearchDocument, Long> {

    List<VenueSearchDocument> findByName(String name);

    List<VenueSearchDocument> findByType(String type);
}

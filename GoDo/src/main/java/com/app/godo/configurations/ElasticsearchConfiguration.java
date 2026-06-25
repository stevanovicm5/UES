package com.app.godo.configurations;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories(
    basePackages = "com.app.godo.repositories.elasticsearch")
public class ElasticsearchConfiguration {
}

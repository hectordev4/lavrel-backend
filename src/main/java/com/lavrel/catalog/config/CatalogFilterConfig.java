package com.lavrel.catalog.config;

import com.lavrel.catalog.domain.ContentPolicy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CatalogFilterProperties.class)
class CatalogFilterConfig {

    @Bean
    ContentPolicy contentPolicy(CatalogFilterProperties properties) {
        return new ContentPolicy(properties);
    }
}

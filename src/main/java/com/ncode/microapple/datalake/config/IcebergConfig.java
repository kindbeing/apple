package com.ncode.microapple.datalake.config;

import org.apache.iceberg.catalog.Catalog;
import org.apache.iceberg.rest.RESTCatalog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class IcebergConfig {

    @Value("${iceberg.catalog.uri:http://localhost:8181}")
    private String catalogUri;

    @Value("${iceberg.warehouse:s3://iceberg-warehouse/}")
    private String warehouse;

    @Bean
    public Catalog icebergCatalog() {
        RESTCatalog catalog = new RESTCatalog();
        catalog.initialize("microapple_catalog", Map.of(
            "uri", catalogUri,
            "warehouse", warehouse,
            "io-impl", "org.apache.iceberg.aws.s3.S3FileIO",
            "s3.endpoint", "http://localhost:9000",
            "s3.access-key-id", "minioadmin",
            "s3.secret-access-key", "minioadmin123",
            "s3.path-style-access", "true"
        ));
        return catalog;
    }
}
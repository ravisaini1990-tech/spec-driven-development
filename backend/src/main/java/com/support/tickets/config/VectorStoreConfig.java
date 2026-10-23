package com.support.tickets.config;

import org.springframework.context.annotation.Configuration;

/**
 * PGVector store is auto-configured by Spring AI via application.yml.
 * This class is a placeholder for any future vector store customisation
 * (e.g. custom distance metrics, index parameters).
 *
 * Configuration reference:
 *   spring.ai.vectorstore.pgvector.initialize-schema=true
 *   spring.ai.vectorstore.pgvector.dimensions=1536
 *   spring.ai.vectorstore.pgvector.distance-type=COSINE_DISTANCE
 *   spring.ai.vectorstore.pgvector.index-type=HNSW
 */
@Configuration
public class VectorStoreConfig {
    // Spring AI auto-configures PgVectorStore bean from application.yml properties.
    // No manual bean definition needed.
}

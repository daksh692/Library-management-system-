package com.library.lms.config;

import com.mongodb.client.MongoClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Enables multi-document transactions and {@code @CreatedDate} auditing.
 *
 * <p><strong>Requires a replica set.</strong> See {@code 02-data-integrity.md} §0.
 * Against a standalone mongod, any {@code @Transactional} method will fail at runtime.</p>
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {

    @Bean
    public MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }

    @Bean
    public MongoTemplate mongoTemplate(MongoDatabaseFactory dbFactory) {
        return new MongoTemplate(dbFactory);
    }
}

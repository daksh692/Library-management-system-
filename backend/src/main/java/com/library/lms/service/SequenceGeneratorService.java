package com.library.lms.service;

import com.library.lms.model.Counter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

/**
 * Hands out collision-free monotonic numbers via an atomic {@code $inc}.
 *
 * <p><strong>Monotonic, but not contiguous:</strong>
 * This sequence is guaranteed to go up, but gaps will occur. If a transaction draws a 
 * sequence number and then rolls back, that number is lost. This is acceptable for 
 * queue positions and member IDs, where ordering and uniqueness matter, but contiguousness 
 * does not.</p>
 */
@Service
@RequiredArgsConstructor
public class SequenceGeneratorService {

    private final MongoTemplate mongoTemplate;

    /**
     * @param key an arbitrary counter name, e.g. {@code "queue:" + bookId}
     * @return the next value, starting at 1
     */
    public long nextValue(String key) {
        Counter counter = mongoTemplate.findAndModify(
                new Query(Criteria.where("_id").is(key)),
                new Update().inc("seq", 1),
                FindAndModifyOptions.options().returnNew(true).upsert(true),
                Counter.class
        );
        return counter == null ? 1L : counter.getSeq();
    }
}

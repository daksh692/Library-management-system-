package com.library.lms.repository;

import com.library.lms.model.Book;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

/**
 * Implementation of {@link BookRepositoryCustom}.
 *
 * <p>The class name suffix {@code Impl} is required — Spring Data discovers custom
 * repository fragments by that convention.</p>
 *
 * <p><strong>Atomic Operations vs. Read-Then-Write:</strong>
 * We use atomic {@code findAndModify} operations here rather than a typical fetch, mutate, 
 * and save sequence. This prevents race conditions where two concurrent requests could
 * read the same {@code availableCopies} value and both successfully borrow the final copy.
 * By pushing the decrement and the {@code > 0} condition into a single database command,
 * we ensure correctness without needing heavy pessimistic locking.</p>
 */
@RequiredArgsConstructor
public class BookRepositoryImpl implements BookRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    private static final FindAndModifyOptions RETURN_NEW =
            FindAndModifyOptions.options().returnNew(true);

    @Override
    public Book tryReserveCopy(String bookId) {
        // The gt(0) criterion is the whole point: the check and the write are one operation,
        // so two concurrent callers cannot both succeed on the last copy.
        Query query = new Query(Criteria.where("_id").is(bookId)
                .and("availableCopies").gt(0)
                .and("isDeleted").is(false));

        Update update = new Update().inc("availableCopies", -1);

        return mongoTemplate.findAndModify(query, update, RETURN_NEW, Book.class);
    }

    @Override
    public Book releaseCopy(String bookId) {
        Query query = new Query(Criteria.where("_id").is(bookId));
        Update update = new Update().inc("availableCopies", 1);
        return mongoTemplate.findAndModify(query, update, RETURN_NEW, Book.class);
    }

    @Override
    public Book writeOffCopy(String bookId) {
        Query query = new Query(Criteria.where("_id").is(bookId)
                .and("totalCopies").gt(0));
        Update update = new Update().inc("totalCopies", -1);
        return mongoTemplate.findAndModify(query, update, RETURN_NEW, Book.class);
    }
}

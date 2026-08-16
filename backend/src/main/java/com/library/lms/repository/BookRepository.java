package com.library.lms.repository;

import com.library.lms.model.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends MongoRepository<Book, String>, BookRepositoryCustom {
    List<Book> findByIsbn(String isbn);
    List<Book> findByGenre(String genre);

    /** Newest first, for the New Collections grid. */
    List<Book> findByIsDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    /** Recommendation source: anything in the genres a patron has read before. */
    List<Book> findByIsDeletedFalseAndGenreIn(List<String> genres);

    /** Paginated catalogue browse. */
    Page<Book> findByIsDeletedFalse(Pageable pageable);

    /** All non-deleted books. */
    List<Book> findByIsDeletedFalse();

    long countByIsDeletedFalse();

    @Query("{ 'isDeleted': false, $or: [ " +
           "{ 'name':   { $regex: ?0, $options: 'i' } }, " +
           "{ 'author': { $regex: ?0, $options: 'i' } }, " +
           "{ 'genre':  { $regex: ?0, $options: 'i' } }, " +
           "{ 'isbn':   { $regex: ?0, $options: 'i' } } ] }")
    List<Book> search(String query);
}

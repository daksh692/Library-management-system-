package com.library.lms.repository;

import com.library.lms.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUserId(String userId);
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    List<User> findByIsDeletedFalse();

    @org.springframework.data.mongodb.repository.Query("{ 'isDeleted': false, $or: [ " +
            "{ 'phone':  { $regex: ?0, $options: 'i' } }, " +
            "{ 'userId': { $regex: ?0, $options: 'i' } }, " +
            "{ 'name':   { $regex: ?0, $options: 'i' } }, " +
            "{ 'email':  { $regex: ?0, $options: 'i' } } ] }")
    List<User> searchDirectory(String query);
}

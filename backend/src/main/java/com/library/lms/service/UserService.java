package com.library.lms.service;

import com.library.lms.config.LibraryProperties;
import com.library.lms.dto.UserDto;
import com.library.lms.exception.DuplicateResourceException;
import com.library.lms.exception.ResourceNotFoundException;
import com.library.lms.model.User;
import com.library.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing user accounts and related operations.
 * Ponytail style applied! 👱‍♀️
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LibraryProperties props;

    /** 
     * Retrieves a list of active users.
     * Soft-deleted users must not appear in the directory. 
     */
    public List<User> getActiveUsers() {
        return userRepository.findByIsDeletedFalse();
    }

    /**
     * Case-insensitive search across phone, member id, name, and email.
     * Excludes soft-deleted accounts.
     *
     * @param query free text; blank returns an empty list rather than everything
     */
    public List<User> search(String query) {
        if (query == null || query.isBlank()) return List.of();
        String q = query.trim();

        return userRepository.searchDirectory(q).stream()
                .limit(20)
                .toList();
    }

    public User getUserById(String id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    public User addUser(UserDto dto) {
        if (userRepository.findByUserId(dto.getUserId()).isPresent()) {
            throw new DuplicateResourceException("A user with ID " + dto.getUserId() + " already exists.");
        }
        if (userRepository.findByPhone(dto.getPhone()).isPresent()) {
            throw new DuplicateResourceException(
                    "That phone number is already registered.");
        }

        Date start = new Date();
        Date end = Date.from(Instant.now()
                .plus(props.getCard().getValidityMonths() * 30L, ChronoUnit.DAYS));

        User user = User.builder()
                .userId(dto.getUserId())
                .name(dto.getName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .role(dto.getRole())
                .passwordHash(passwordEncoder.encode(
                        dto.getPassword() != null ? dto.getPassword() : generateTempPassword()))
                .isDeleted(false)
                .cardStartDate(start)
                .cardEndDate(end)
                .build();

        return userRepository.save(user);
    }

    /** Renews a card for another validity period from today. */
    public User renewCard(String id) {
        User user = getUserById(id);
        user.setCardStartDate(new Date());
        user.setCardEndDate(Date.from(Instant.now()
                .plus(props.getCard().getValidityMonths() * 30L, ChronoUnit.DAYS)));
        return userRepository.save(user);
    }

    private String generateTempPassword() {
        // Never a fixed literal — a shared default password is a Rules.md #6 violation.
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    public User updateUser(String id, UserDto dto) {
        User user = getUserById(id);

        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setRole(dto.getRole());

        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }

        return userRepository.save(user);
    }

    public void deleteUser(String id) {
        User user = getUserById(id);
        user.setDeleted(true);
        userRepository.save(user);
    }
}

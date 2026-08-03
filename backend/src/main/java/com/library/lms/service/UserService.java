package com.library.lms.service;

import com.library.lms.dto.UserDto;
import com.library.lms.model.User;
import com.library.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(String id) {
        return userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User addUser(UserDto dto) {
        if (userRepository.findByUserId(dto.getUserId()).isPresent()) {
            throw new RuntimeException("User ID already exists");
        }

        User user = User.builder()
                .userId(dto.getUserId())
                .name(dto.getName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .role(dto.getRole())
                .passwordHash(passwordEncoder.encode(dto.getPassword() != null ? dto.getPassword() : "default123"))
                .isDeleted(false)
                .cardStartDate(new Date())
                .build();

        return userRepository.save(user);
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

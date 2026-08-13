package com.library.lms.service;

import com.library.lms.model.User;
import com.library.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Bridges Spring Security's authentication manager with our {@link User} entity.
 *
 * <p>Translates our human-readable {@code userId} (e.g. {@code LIB-2026-0001}) into the
 * "username" concept expected by Spring's filter chain.</p>
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUserId(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with userId: " + username));
    }
}

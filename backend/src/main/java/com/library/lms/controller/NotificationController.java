package com.library.lms.controller;

import com.library.lms.exception.ResourceNotFoundException;
import com.library.lms.model.Notification;
import com.library.lms.model.User;
import com.library.lms.repository.UserRepository;
import com.library.lms.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * Patron notification inbox. Every operation is scoped to the caller — the
 * recipient is resolved from the JWT, never from a request parameter.
 */
@RestController
@RequestMapping("/api/user/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(Principal principal) {
        String uid = resolve(principal).getId();
        List<Notification> items = notificationService.forUser(uid);
        return ResponseEntity.ok(Map.of(
                "items", items,
                "unread", notificationService.unreadCount(uid)
        ));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable String id, Principal principal) {
        notificationService.markRead(id, resolve(principal).getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead(Principal principal) {
        notificationService.markAllRead(resolve(principal).getId());
        return ResponseEntity.noContent().build();
    }

    private User resolve(Principal principal) {
        return userRepository.findByUserId(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getName()));
    }
}

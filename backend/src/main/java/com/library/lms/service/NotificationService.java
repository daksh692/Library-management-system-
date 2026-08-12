package com.library.lms.service;

import com.library.lms.model.Book;
import com.library.lms.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public void notifyQueued(User user, Book book, int sequence) {
        log.info("Mock Notification: {} queued for '{}' at position {}", user.getUserId(), book.getName(), sequence);
    }

    public void notifyIssued(User user, Book book, Date dueDate) {
        log.info("Mock Notification: '{}' issued to {} (due {})", book.getName(), user.getUserId(), dueDate);
    }

    public void notifyReadyForPickup(User user, Book book, int windowHours) {
        log.info("Mock Notification: '{}' is ready for pickup by {} (held for {} hours)", book.getName(), user.getUserId(), windowHours);
    }

    public void notifyPenalty(User user, Book book, double penalty) {
        log.info("Mock Notification: Penalty of ${} applied to {} for '{}'", penalty, user.getUserId(), book.getName());
    }

    public void notifyHoldExpired(User user, Book book) {
        log.info("Mock Notification: Hold expired for '{}' for user {}", book.getName(), user.getUserId());
    }

    public void sendDueSoonReminders() {
        log.info("Mock Notification: Sent due soon reminders");
    }
}

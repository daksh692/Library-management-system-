package com.library.lms.service;

/** Strategy for outbound email. Implementations must never throw. */
public interface EmailSender {

    /**
     * @return true if the message was accepted for delivery
     */
    boolean send(String to, String subject, String body);
}

package com.library.lms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Phase A default: writes the email to the log instead of sending it.
 *
 * <p>Active unless {@code app.email.enabled=true}. Lets the whole notification
 * path be developed and demonstrated with no SMTP account.</p>
 */
@Component
@ConditionalOnProperty(name = "app.email.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    @Override
    public boolean send(String to, String subject, String body) {
        log.info("""
                [EMAIL — not sent, app.email.enabled=false]
                  To:      {}
                  Subject: {}
                  Body:    {}""", to, subject, body);
        return false;   // honest: nothing was delivered, so emailedAt stays null
    }
}

package com.library.lms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class KeepAliveService {

    private static final Logger logger = LoggerFactory.getLogger(KeepAliveService.class);
    private final RestTemplate restTemplate = new RestTemplate();

    // Render automatically populates this environment variable for Web Services
    @Value("${RENDER_EXTERNAL_URL:}")
    private String renderExternalUrl;

    // Run every 14 minutes (840,000 ms) to prevent the free tier from sleeping (sleeps after 15 mins)
    @Scheduled(fixedRate = 840000)
    public void pingSelf() {
        if (renderExternalUrl != null && !renderExternalUrl.trim().isEmpty()) {
            try {
                String url = renderExternalUrl + "/api/public/books/policy";
                logger.info("Keeping alive! Pinging: " + url);
                restTemplate.getForObject(url, String.class);
            } catch (Exception e) {
                logger.error("Keep-alive ping failed", e);
            }
        } else {
            logger.debug("RENDER_EXTERNAL_URL is not set. Keep-alive ping skipped.");
        }
    }
}

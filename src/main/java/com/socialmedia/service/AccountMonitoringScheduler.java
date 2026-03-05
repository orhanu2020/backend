package com.socialmedia.service;

import com.socialmedia.model.AccountMonitoringEntry;
import com.socialmedia.service.twitter.TwitterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class AccountMonitoringScheduler {

    private static final Logger logger = LoggerFactory.getLogger(AccountMonitoringScheduler.class);
    
    private final AccountMonitoringService monitoringService;
    private final TwitterService twitterService;

    public AccountMonitoringScheduler(AccountMonitoringService monitoringService, 
                                     TwitterService twitterService) {
        this.monitoringService = monitoringService;
        this.twitterService = twitterService;
    }

    @Scheduled(cron = "0 6 10 * * ?") // Run daily at 10:06 AM
    public void processMonitoringEntries() {
        logger.info("Starting scheduled monitoring task at 10:06 AM");
        
        List<AccountMonitoringEntry> entries = monitoringService.getAllEntries();
        
        if (entries.isEmpty()) {
            logger.info("No monitoring entries found, skipping scheduled task");
            return;
        }
        
        logger.info("Processing {} monitoring entries", entries.size());
        
        for (AccountMonitoringEntry entry : entries) {
            try {
                processEntry(entry);
            } catch (Exception e) {
                logger.error("Failed to process monitoring entry: {}:{}:{}:{}:{}", 
                        entry.getKeyword(), entry.getPlatform(), entry.getKeywordTypeNormalized(),
                        entry.getStartTime(), entry.getEndTime(), e);
            }
        }
        
        logger.info("Completed scheduled monitoring task");
    }

    private void processEntry(AccountMonitoringEntry entry) {
        String platform = entry.getPlatform() != null ? entry.getPlatform().toLowerCase() : "";
        String keyword = entry.getKeyword() != null ? entry.getKeyword().trim() : (entry.getAccountName() != null ? entry.getAccountName().trim() : "");
        String keywordType = entry.getKeywordTypeNormalized();
        
        if (!"twitter".equals(platform)) {
            logger.warn("Unsupported platform: {} for keyword: {}", platform, keyword);
            return;
        }
        if (keyword.isEmpty()) {
            logger.warn("Skipping entry with empty keyword");
            return;
        }
        
        Instant startTime = entry.getStartTime().atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endTime = entry.getEndTime().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        
        logger.info("Processing entry - Keyword: {}, Type: {}, Platform: {}, Start: {}, End: {}", 
                keyword, keywordType, platform, entry.getStartTime(), entry.getEndTime());
        
        if ("tweet".equals(keywordType)) {
            twitterService.loadTweetsByKeywordAsync(keyword, "Latest", startTime, endTime);
            logger.info("Started async tweet-by-keyword loading for: {}", keyword);
        } else {
            twitterService.loadTweetsAsync(keyword, startTime, endTime);
            logger.info("Started async tweet loading for account: {}", keyword);
        }
    }
}



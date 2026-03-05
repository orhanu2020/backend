package com.socialmedia.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.socialmedia.model.AccountMonitoringEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AccountMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(AccountMonitoringService.class);
    private static final String MONITORING_FILE = "account-monitoring.json";
    private final ObjectMapper objectMapper;
    private final Path filePath;

    public AccountMonitoringService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.filePath = Paths.get(MONITORING_FILE);
        initializeFile();
    }

    private void initializeFile() {
        try {
            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
                objectMapper.writeValue(filePath.toFile(), new ArrayList<AccountMonitoringEntry>());
                logger.info("Created monitoring file: {}", filePath.toAbsolutePath());
            }
        } catch (IOException e) {
            logger.error("Failed to initialize monitoring file", e);
        }
    }

    public List<AccountMonitoringEntry> getAllEntries() {
        try {
            File file = filePath.toFile();
            if (!file.exists() || file.length() == 0) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(file, new TypeReference<List<AccountMonitoringEntry>>() {});
        } catch (IOException e) {
            logger.error("Failed to read monitoring entries", e);
            return new ArrayList<>();
        }
    }

    public void addEntry(AccountMonitoringEntry entry) {
        if (entry.getEndTime().isBefore(entry.getStartTime())) {
            throw new IllegalArgumentException("End time cannot be before start time");
        }
        normalizeKeywordType(entry);
        List<AccountMonitoringEntry> entries = getAllEntries();
        entries.add(entry);
        saveEntries(entries);
        logger.info("Added monitoring entry: {}:{}:{}:{}:{}", 
                entry.getKeyword(), entry.getPlatform(), entry.getKeywordTypeNormalized(), entry.getStartTime(), entry.getEndTime());
    }

    public void addEntries(List<AccountMonitoringEntry> newEntries) {
        List<AccountMonitoringEntry> entries = getAllEntries();
        entries.addAll(newEntries);
        saveEntries(entries);
        logger.info("Added {} monitoring entries", newEntries.size());
    }

    public void removeEntry(int index) {
        List<AccountMonitoringEntry> entries = getAllEntries();
        if (index >= 0 && index < entries.size()) {
            AccountMonitoringEntry removed = entries.remove(index);
            saveEntries(entries);
            logger.info("Removed monitoring entry: {}:{}:{}:{}:{}", 
                    removed.getKeyword(), removed.getPlatform(), removed.getKeywordTypeNormalized(), removed.getStartTime(), removed.getEndTime());
        } else {
            throw new IndexOutOfBoundsException("Invalid index: " + index);
        }
    }

    private void normalizeKeywordType(AccountMonitoringEntry entry) {
        if (entry.getKeywordType() == null || entry.getKeywordType().isBlank()) {
            entry.setKeywordType("account");
        } else {
            entry.setKeywordType(entry.getKeywordType().trim().toLowerCase());
        }
    }

    /**
     * Parses multiline text. Format: keyword:platform:keywordType:startTime:endTime (one per line).
     * keywordType is "account" or "tweet" (case insensitive).
     * Legacy format keyword:platform:startTime:endTime is accepted and treated as keywordType=account.
     */
    public List<AccountMonitoringEntry> parseMultilineText(String multilineText) {
        List<AccountMonitoringEntry> entries = new ArrayList<>();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        String[] lines = multilineText.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            
            String[] parts = line.split(":");
            if (parts.length != 4 && parts.length != 5) {
                logger.warn("Invalid format in line: {}. Expected: keyword:platform:keywordType:startTime:endTime or keyword:platform:startTime:endTime", line);
                continue;
            }
            
            try {
                String keyword;
                String platform;
                String keywordType = "account";
                LocalDate startTime;
                LocalDate endTime;
                if (parts.length == 5) {
                    keyword = parts[0].trim();
                    platform = parts[1].trim();
                    String rawType = parts[2].trim();
                    if (!rawType.equalsIgnoreCase("account") && !rawType.equalsIgnoreCase("tweet")) {
                        logger.warn("Invalid keywordType in line: {}. Must be 'account' or 'tweet'", line);
                        continue;
                    }
                    keywordType = rawType.toLowerCase();
                    startTime = LocalDate.parse(parts[3].trim(), dateFormatter);
                    endTime = LocalDate.parse(parts[4].trim(), dateFormatter);
                } else {
                    keyword = parts[0].trim();
                    platform = parts[1].trim();
                    startTime = LocalDate.parse(parts[2].trim(), dateFormatter);
                    endTime = LocalDate.parse(parts[3].trim(), dateFormatter);
                }
                
                if (endTime.isBefore(startTime)) {
                    logger.warn("End time is before start time in line: {}", line);
                    continue;
                }
                
                entries.add(AccountMonitoringEntry.builder()
                        .keyword(keyword)
                        .platform(platform)
                        .keywordType(keywordType)
                        .startTime(startTime)
                        .endTime(endTime)
                        .build());
            } catch (DateTimeParseException e) {
                logger.warn("Invalid date format in line: {}. Expected format: yyyy-MM-dd", line);
            }
        }
        
        return entries;
    }

    private void saveEntries(List<AccountMonitoringEntry> entries) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), entries);
        } catch (IOException e) {
            logger.error("Failed to save monitoring entries", e);
            throw new RuntimeException("Failed to save monitoring entries", e);
        }
    }
}


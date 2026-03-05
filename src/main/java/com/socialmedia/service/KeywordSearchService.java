package com.socialmedia.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.socialmedia.model.KeywordSearchEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class KeywordSearchService {

    private static final Logger logger = LoggerFactory.getLogger(KeywordSearchService.class);
    private static final String KEYWORD_FILE = "keyword-search.json";
    private final ObjectMapper objectMapper;
    private final Path filePath;

    public KeywordSearchService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.filePath = Paths.get(KEYWORD_FILE);
        initializeFile();
    }

    private void initializeFile() {
        try {
            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
                objectMapper.writeValue(filePath.toFile(), new ArrayList<KeywordSearchEntry>());
                logger.info("Created keyword search file: {}", filePath.toAbsolutePath());
            }
        } catch (IOException e) {
            logger.error("Failed to initialize keyword search file", e);
        }
    }

    public List<KeywordSearchEntry> getAllEntries() {
        try {
            File file = filePath.toFile();
            if (!file.exists() || file.length() == 0) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(file, new TypeReference<List<KeywordSearchEntry>>() {});
        } catch (IOException e) {
            logger.error("Failed to read keyword search entries", e);
            return new ArrayList<>();
        }
    }

    public void addEntry(KeywordSearchEntry entry) {
        // Validate entry
        if (entry.getKeyword() == null || entry.getKeyword().trim().isEmpty()) {
            throw new IllegalArgumentException("Keyword cannot be empty");
        }
        if (entry.getSearchType() == null || (!entry.getSearchType().equalsIgnoreCase("tweet") 
                && !entry.getSearchType().equalsIgnoreCase("user"))) {
            throw new IllegalArgumentException("Search type must be 'tweet' or 'user'");
        }
        if (entry.getSearchType().equalsIgnoreCase("tweet")) {
            if (entry.getQueryType() == null || 
                    (!entry.getQueryType().equalsIgnoreCase("Latest") 
                    && !entry.getQueryType().equalsIgnoreCase("Top"))) {
                throw new IllegalArgumentException("Query type must be 'Latest' or 'Top' for tweet search");
            }
        }
        
        List<KeywordSearchEntry> entries = getAllEntries();
        entries.add(entry);
        saveEntries(entries);
        logger.info("Added keyword search entry: {}:{}:{}", 
                entry.getKeyword(), entry.getSearchType(), entry.getQueryType());
    }

    public void addEntries(List<KeywordSearchEntry> newEntries) {
        List<KeywordSearchEntry> entries = getAllEntries();
        entries.addAll(newEntries);
        saveEntries(entries);
        logger.info("Added {} keyword search entries", newEntries.size());
    }

    public void removeEntry(int index) {
        List<KeywordSearchEntry> entries = getAllEntries();
        if (index >= 0 && index < entries.size()) {
            KeywordSearchEntry removed = entries.remove(index);
            saveEntries(entries);
            logger.info("Removed keyword search entry: {}:{}:{}", 
                    removed.getKeyword(), removed.getSearchType(), removed.getQueryType());
        } else {
            throw new IndexOutOfBoundsException("Invalid index: " + index);
        }
    }

    public List<KeywordSearchEntry> parseMultilineText(String multilineText) {
        List<KeywordSearchEntry> entries = new ArrayList<>();
        
        String[] lines = multilineText.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            
            // Format: keyword:searchType:queryType:platform
            // For user search: keyword:user:twitter
            // For tweet search: keyword:tweet:Latest:twitter or keyword:tweet:Top:twitter
            String[] parts = line.split(":");
            if (parts.length < 3 || parts.length > 4) {
                logger.warn("Invalid format in line: {}. Expected format: keyword:searchType:queryType:platform or keyword:searchType:platform", line);
                continue;
            }
            
            try {
                String keyword = parts[0].trim();
                String searchType = parts[1].trim().toLowerCase();
                String queryType = null;
                String platform = "twitter";
                
                if (parts.length == 4) {
                    // Format: keyword:searchType:queryType:platform
                    queryType = parts[2].trim();
                    platform = parts[3].trim();
                } else if (parts.length == 3) {
                    // Format: keyword:searchType:platform or keyword:searchType:queryType
                    if (parts[2].trim().equalsIgnoreCase("Latest") || parts[2].trim().equalsIgnoreCase("Top")) {
                        queryType = parts[2].trim();
                    } else {
                        platform = parts[2].trim();
                    }
                }
                
                if (!searchType.equals("tweet") && !searchType.equals("user")) {
                    logger.warn("Invalid search type in line: {}. Must be 'tweet' or 'user'", line);
                    continue;
                }
                
                if (searchType.equals("tweet") && (queryType == null || 
                        (!queryType.equalsIgnoreCase("Latest") && !queryType.equalsIgnoreCase("Top")))) {
                    logger.warn("Invalid query type for tweet search in line: {}. Must be 'Latest' or 'Top'", line);
                    continue;
                }
                
                entries.add(KeywordSearchEntry.builder()
                        .keyword(keyword)
                        .searchType(searchType)
                        .queryType(queryType)
                        .platform(platform)
                        .build());
            } catch (Exception e) {
                logger.warn("Error parsing line: {}. Error: {}", line, e.getMessage());
            }
        }
        
        return entries;
    }

    private void saveEntries(List<KeywordSearchEntry> entries) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), entries);
        } catch (IOException e) {
            logger.error("Failed to save keyword search entries", e);
            throw new RuntimeException("Failed to save keyword search entries", e);
        }
    }
}

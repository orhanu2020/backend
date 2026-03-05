package com.socialmedia.controller;

import com.socialmedia.model.AccountMonitoringEntry;
import com.socialmedia.service.AccountMonitoringService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/monitoring")
@Validated
public class AccountMonitoringController {

    private final AccountMonitoringService monitoringService;

    public AccountMonitoringController(AccountMonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @GetMapping("/entries")
    public ResponseEntity<List<AccountMonitoringEntry>> getAllEntries() {
        return ResponseEntity.ok(monitoringService.getAllEntries());
    }

    @PostMapping("/entries")
    public ResponseEntity<Map<String, String>> addEntry(
            @RequestParam @NotBlank(message = "Keyword cannot be blank") String keyword,
            @RequestParam @NotBlank(message = "Platform cannot be blank") String platform,
            @RequestParam(required = false) String keywordType,
            @RequestParam @NotBlank(message = "Start time cannot be blank") String startTime,
            @RequestParam @NotBlank(message = "End time cannot be blank") String endTime) {
        
        java.time.LocalDate start = java.time.LocalDate.parse(startTime);
        java.time.LocalDate end = java.time.LocalDate.parse(endTime);
        
        if (end.isBefore(start)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "End time cannot be before start time"
            ));
        }
        String type = (keywordType == null || keywordType.isBlank()) ? "account" : keywordType.trim().toLowerCase();
        if (!type.equals("account") && !type.equals("tweet")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Keyword type must be 'account' or 'tweet'"
            ));
        }
        
        AccountMonitoringEntry entry = AccountMonitoringEntry.builder()
                .keyword(keyword.trim())
                .platform(platform)
                .keywordType(type)
                .startTime(start)
                .endTime(end)
                .build();
        
        try {
            monitoringService.addEntry(entry);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Entry added successfully"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/entries/multiline")
    public ResponseEntity<Map<String, Object>> addEntriesFromMultiline(
            @RequestParam @NotBlank(message = "Multiline text cannot be blank") String multilineText) {
        
        List<AccountMonitoringEntry> entries = monitoringService.parseMultilineText(multilineText);
        
        if (entries.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "No valid entries found in multiline text"
            ));
        }
        
        monitoringService.addEntries(entries);
        
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Entries added successfully",
                "count", entries.size()
        ));
    }

    @DeleteMapping("/entries/{index}")
    public ResponseEntity<Map<String, String>> removeEntry(@PathVariable int index) {
        try {
            monitoringService.removeEntry(index);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Entry removed successfully"
            ));
        } catch (IndexOutOfBoundsException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Invalid index: " + index
            ));
        }
    }
}


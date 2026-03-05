package com.socialmedia.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountMonitoringEntry {
    /** Keyword: account name when keywordType=account, or word in tweet when keywordType=tweet. */
    @JsonAlias("accountName")
    private String keyword;
    private String platform;
    /** "account" or "tweet" (case insensitive). Default "account" when null. */
    private String keywordType;
    private LocalDate startTime;
    private LocalDate endTime;

    /** Returns keywordType normalized to lowercase, or "account" if null/blank. */
    public String getKeywordTypeNormalized() {
        if (keywordType == null || keywordType.isBlank()) return "account";
        return keywordType.trim().toLowerCase();
    }

    /** For backward compatibility: display/serialization alias when only accountName was used. */
    public String getAccountName() {
        return keyword;
    }
}




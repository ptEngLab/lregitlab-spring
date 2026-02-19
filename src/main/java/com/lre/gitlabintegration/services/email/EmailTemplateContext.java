package com.lre.gitlabintegration.services.email;

import lombok.Builder;
import lombok.Data;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Top-level context object passed to the email service.
 * All fields map 1-to-1 to Thymeleaf template variables.
 */
@Data
@Builder
public class EmailTemplateContext {

    // ── Run Info ──────────────────────────────────────────────────────────────
    private String runId;
    private String testName;

    /** "PASSED" or "FAILED" — derived automatically by the service */
    private String status;

    private String startTime;
    private String endTime;
    private String duration;

    // ── Sub-objects ───────────────────────────────────────────────────────────
    private List<LGInfo>          lgInfo;
    private Metrics               metrics;
    private List<TransactionData> transactionData;
    private List<ErrorsData>      errorsData;

    // ── Thresholds supplied by the caller ─────────────────────────────────────
    private TestThresholds thresholds;

    // ── Computed by service — do NOT set manually ─────────────────────────────
    private List<ThresholdBreach> thresholdBreaches;

    // ── Totals ────────────────────────────────────────────────────────────────
    private int totalTransactions;
    private int totalErrors;

    // ── Email routing ─────────────────────────────────────────────────────────
    private List<String> recipients;
    private List<String> cc;

    // ── Attachment ────────────────────────────────────────────────────────────
    /** Full file system path to the Excel file produced by your results module. */
    private String attachmentFilePath;
    /** Display name shown in the email, e.g. "LoadRunner_Results_RUN-001.xlsx" */
    private String attachmentFileName;

    // ── Branding ──────────────────────────────────────────────────────────────
    private String logoUrl;
    private String generatedAt;


    // =========================================================================
    // Inner classes
    // =========================================================================

    @Data
    @Builder
    public static class LGInfo {
        private String lgName;
        private int    vuserCount;

        private static final Pattern LG_PATTERN =
                Pattern.compile("([A-Za-z0-9_.:-]+)\\s*(?:\\((\\d+)\\))?");

        /**
         * Parses a LoadRunner LG string into a list of LGInfo objects.
         * Example input: "LG1(10); 10.0.0.1(15); LG3(10);"
         */
        public static List<LGInfo> fromLoadRunnerString(String lgString) {
            if (lgString == null || lgString.isBlank()) {
                return Collections.emptyList();
            }
            return Arrays.stream(lgString.split(";"))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .map(LGInfo::parseLG)
                    .collect(Collectors.toList());
        }

        /**
         * Parses a single LG entry using regex.
         * Returns LGInfo with lgName and vuserCount (0 if not specified).
         */
        private static LGInfo parseLG(String lgEntry) {
            Matcher matcher = LG_PATTERN.matcher(lgEntry);
            if (matcher.matches()) {
                String name     = matcher.group(1);
                String countStr = matcher.group(2);
                int vuserCount  = countStr != null ? Integer.parseInt(countStr) : 0;
                return LGInfo.builder()
                        .lgName(name)
                        .vuserCount(vuserCount)
                        .build();
            } else {
                // Fallback: treat the whole entry as the name with no user count
                return LGInfo.builder()
                        .lgName(lgEntry)
                        .vuserCount(0)
                        .build();
            }
        }
    }

    @Data
    @Builder
    public static class Metrics {
        private int transactionPassed;
        private int transactionFailed;
        private int virtualUsers;
    }

    @Data
    @Builder
    public static class TransactionData {
        private String name;
        private String min;
        private String avg;
        private String max;
        private String p90;
        private String p95;
        private int    pass;
        private int    fail;
    }

    @Data
    @Builder
    public static class ErrorsData {
        private String script;
        private String injector;
        private String errorCode;
        private String message;
        private int    count;
        private int    affectedVusers;
    }

    /**
     * User-configured pass/fail limits.
     * Only two thresholds are evaluated:
     *   1. Max failed transactions (total count across the run)
     *   2. Max total errors (sum of all error occurrences)
     */
    @Data
    @Builder
    public static class TestThresholds {
        /** Maximum number of failed transactions allowed. Null = not checked. */
        private Integer maxFailedTransactions;

        /** Maximum total error count allowed. Null = not checked. */
        private Integer maxTotalErrors;
    }

    /**
     * One entry per violated threshold — produced by the service.
     * Rendered as a summary sentence in the email.
     */
    @Data
    @Builder
    public static class ThresholdBreach {
        private String thresholdName;   // e.g. "Max Failed Transactions"
        private String configuredLimit; // e.g. "50"
        private String actualValue;     // e.g. "83"
        private String sentence;        // full human-readable summary sentence
    }
}
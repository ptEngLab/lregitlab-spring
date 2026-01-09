package com.lre.gitlabintegration.dto.sync;

import lombok.Builder;

import java.util.List;

@Builder
public record SyncResponse(
        boolean success,
        SyncSummary summary,
        CategorizedChanges changes
) {

    @Builder
    public record SyncSummary(
            int uploaded,
            int deleted,
            int unchanged,
            int failed
    ) {
    }

    @Builder
    public record CategorizedChanges(
            List<ScriptChange> uploaded,
            List<ScriptChange> deleted,
            List<ScriptChange> unchanged,
            List<ScriptChange> failed
    ) {


    }

    @Builder
    public record ScriptChange(
            String path,
            String scriptName,
            String commitSha,
            String action,
            String status,
            String message,
            String testFolderPath
    ) {

        public static ScriptChange success(
                String path,
                String scriptName,
                String commitSha,
                String action,
                String testFolderPath
        ) {
            return ScriptChange.builder()
                    .path(path)
                    .scriptName(scriptName)
                    .commitSha(commitSha)
                    .action(action)
                    .status("SUCCESS")
                    .message(action + " successfully")
                    .testFolderPath(testFolderPath)
                    .build();
        }

        public static ScriptChange failure(
                String path,
                String scriptName,
                String commitSha,
                String action,
                String message,
                String testFolderPath
        ) {
            return ScriptChange.builder()
                    .path(path)
                    .scriptName(scriptName)
                    .commitSha(commitSha)
                    .action(action)
                    .status("FAILED")
                    .message(message)
                    .testFolderPath(testFolderPath)
                    .build();
        }

        public static ScriptChange unchanged(
                String path,
                String scriptName,
                String commitSha,
                String testFolderPath
        ) {
            return ScriptChange.builder()
                    .path(path)
                    .scriptName(scriptName)
                    .commitSha(commitSha)
                    .action("UNCHANGED")
                    .status("SUCCESS")
                    .message("No changes detected")
                    .testFolderPath(testFolderPath)
                    .build();
        }
    }
}

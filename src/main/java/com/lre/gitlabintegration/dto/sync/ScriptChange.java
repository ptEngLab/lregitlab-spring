package com.lre.gitlabintegration.dto.sync;

import lombok.Builder;

@Builder
public record ScriptChange(
        String path,
        String scriptName,
        String commitSha,
        String action,
        String status,
        String message,
        String testFolderPath,
        Integer lreScriptId

) {

    public static ScriptChange success(
            String path,
            String scriptName,
            String commitSha,
            String action,
            String testFolderPath,
            Integer lreScriptId

    ) {
        return ScriptChange.builder()
                .path(path)
                .scriptName(scriptName)
                .commitSha(commitSha)
                .action(action)
                .status("SUCCESS")
                .message(action + " successfully")
                .testFolderPath(testFolderPath)
                .lreScriptId(lreScriptId)
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

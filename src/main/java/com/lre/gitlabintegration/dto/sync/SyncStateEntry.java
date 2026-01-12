package com.lre.gitlabintegration.dto.sync;

public record SyncStateEntry(
        String path,
        String commitSha,
        String committedDate,
        Integer lreScriptId
) {
    public boolean isEmpty() {
        return path == null || path.isBlank();
    }
}

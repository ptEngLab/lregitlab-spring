package com.lre.gitlabintegration.dto.sync;

import com.lre.gitlabintegration.dto.gitlab.GitLabCommit;

import java.util.List;

public record SyncResult(
        List<GitLabCommit> scriptsToUpload,
        List<SyncStateEntry> scriptsToDelete,
        List<GitLabCommit> unchangedScripts
) {
    public boolean hasChanges() {
        return !scriptsToUpload.isEmpty() || !scriptsToDelete.isEmpty();
    }

    public int totalScripts() {
        return scriptsToUpload.size()
                + scriptsToDelete.size()
                + unchangedScripts.size();
    }
}

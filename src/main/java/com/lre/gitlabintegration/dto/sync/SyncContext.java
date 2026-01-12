package com.lre.gitlabintegration.dto.sync;

import com.lre.gitlabintegration.dto.gitlab.GitLabCommit;

import java.util.List;

public record SyncContext(
        SyncRequest request,
        List<SyncStateEntry> previous,
        List<GitLabCommit> current,
        boolean initial
) {
    public boolean isInitial() {
        return initial;
    }
}

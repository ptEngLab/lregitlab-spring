package com.lre.gitlabintegration.dto.sync;

import lombok.Builder;

@Builder
public record SyncResponse(
        boolean success,
        SyncSummary summary,
        CategorizedChanges changes
) {}

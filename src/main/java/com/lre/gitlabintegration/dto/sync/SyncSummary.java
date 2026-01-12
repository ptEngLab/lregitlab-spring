package com.lre.gitlabintegration.dto.sync;

import lombok.Builder;

@Builder
public record SyncSummary(
        int uploaded,
        int deleted,
        int unchanged,
        int failed
) {}

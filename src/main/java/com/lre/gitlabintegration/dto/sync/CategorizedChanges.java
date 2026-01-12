package com.lre.gitlabintegration.dto.sync;

import lombok.Builder;

import java.util.List;

@Builder
public record CategorizedChanges(
        List<ScriptChange> uploaded,
        List<ScriptChange> deleted,
        List<ScriptChange> unchanged,
        List<ScriptChange> failed
) {}

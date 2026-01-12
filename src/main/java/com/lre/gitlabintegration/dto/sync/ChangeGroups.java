package com.lre.gitlabintegration.dto.sync;

import java.util.List;

public record ChangeGroups(
        List<ScriptChange> uploadedSuccess,
        List<ScriptChange> deletedSuccess,
        List<ScriptChange> failed
) {}

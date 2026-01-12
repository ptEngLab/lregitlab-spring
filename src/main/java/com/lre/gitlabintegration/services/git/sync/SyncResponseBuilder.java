package com.lre.gitlabintegration.services.git.sync;

import com.lre.gitlabintegration.dto.gitlab.GitLabCommit;
import com.lre.gitlabintegration.dto.sync.*;
import com.lre.gitlabintegration.dto.testplan.TestPlanCreationRequest;
import com.lre.gitlabintegration.util.path.PathUtils;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class SyncResponseBuilder {

    public static SyncResponse initialSync(List<ScriptChange> successful, List<ScriptChange> failed) {

        boolean success = failed.isEmpty();

        SyncSummary summary = new SyncSummary(
                successful.size(), 0, 0, failed.size());

        CategorizedChanges changes = new CategorizedChanges(
                successful, List.of(), List.of(), failed);

        return SyncResponse.builder()
                .success(success)
                .summary(summary)
                .changes(changes)
                .build();
    }

    public static SyncResponse incremental(List<ScriptChange> updatedSuccess,
                                           List<ScriptChange> deletedSuccess,
                                           List<ScriptChange> unchanged,
                                           List<ScriptChange> failed) {

        boolean success = failed.isEmpty();

        SyncSummary summary = new SyncSummary(
                updatedSuccess.size(), deletedSuccess.size(), unchanged.size(), failed.size());

        CategorizedChanges changes = new CategorizedChanges(
                updatedSuccess, deletedSuccess, unchanged, failed
        );

        return SyncResponse.builder()
                .success(success)
                .summary(summary)
                .changes(changes)
                .build();
    }

    public static SyncResponse noChanges(List<ScriptChange> unchanged) {
        SyncSummary summary = new SyncSummary(
                0, 0, unchanged.size(), 0
        );

        CategorizedChanges changes = new CategorizedChanges(
                List.of(), List.of(), unchanged, List.of()
        );

        return SyncResponse.builder()
                .success(true)
                .summary(summary)
                .changes(changes)
                .build();
    }

    public static SyncResponse failureResponse(String phase, Exception e, SyncRequest req) {
        String msg = rootMessage(e);

        ScriptChange failed = ScriptChange.failure(
                "SYSTEM", req != null ? req.getLreProject() : "SYNC",
                "emptySha", phase, msg, "Subject\\"
        );

        SyncSummary summary = new SyncSummary(
                0, 0, 0, 1
        );

        CategorizedChanges changes = new CategorizedChanges(
                List.of(), List.of(), List.of(), List.of(failed)
        );

        return SyncResponse.builder()
                .success(true)
                .summary(summary)
                .changes(changes)
                .build();
    }

    private static String rootMessage(Throwable t) {
        Throwable current = t;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() != null ? current.getMessage() : current.toString();
    }

    public static List<ScriptChange> buildUnchangedList(List<GitLabCommit> commits) {
        return commits.stream()
                .map(commit -> {
                    TestPlanCreationRequest info = PathUtils.fromGitPath(commit.getPath());
                    String folderPath = PathUtils.normalizePathWithSubject(info.getPath());
                    String commitSha = commit.getSha().substring(0, Math.min(8, commit.getSha().length()));
                    return ScriptChange.unchanged(
                            commit.getPath(),
                            info.getName(),
                            commitSha,
                            folderPath
                    );
                })
                .toList();
    }
}

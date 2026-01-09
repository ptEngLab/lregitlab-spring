package com.lre.gitlabintegration.services.git.sync;

import com.lre.gitlabintegration.dto.gitlab.GitLabCommit;
import com.lre.gitlabintegration.dto.sync.SyncRequest;
import com.lre.gitlabintegration.dto.sync.SyncResponse;
import com.lre.gitlabintegration.dto.testplan.TestPlanCreationRequest;
import com.lre.gitlabintegration.util.path.PathUtils;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class SyncResponseBuilder {

    public static SyncResponse initialSync(List<SyncResponse.ScriptChange> successful, List<SyncResponse.ScriptChange> failed) {

        boolean success = failed.isEmpty();

        SyncResponse.SyncSummary summary = new SyncResponse.SyncSummary(
                successful.size(), 0, 0, failed.size());

        SyncResponse.CategorizedChanges changes = new SyncResponse.CategorizedChanges(
                successful, List.of(), List.of(), failed);

        return SyncResponse.builder()
                .success(success)
                .summary(summary)
                .changes(changes)
                .build();
    }

    public static SyncResponse incremental(List<SyncResponse.ScriptChange> updatedSuccess,
                                           List<SyncResponse.ScriptChange> deletedSuccess,
                                           List<SyncResponse.ScriptChange> unchanged,
                                           List<SyncResponse.ScriptChange> failed) {

        boolean success = failed.isEmpty();

        SyncResponse.SyncSummary summary = new SyncResponse.SyncSummary(
                updatedSuccess.size(), deletedSuccess.size(), unchanged.size(), failed.size());

        SyncResponse.CategorizedChanges changes = new SyncResponse.CategorizedChanges(
                updatedSuccess, deletedSuccess, unchanged, failed
        );

        return SyncResponse.builder()
                .success(success)
                .summary(summary)
                .changes(changes)
                .build();
    }

    public static SyncResponse noChanges(List<SyncResponse.ScriptChange> unchanged) {
        SyncResponse.SyncSummary summary = new SyncResponse.SyncSummary(
                0, 0, unchanged.size(), 0
        );

        SyncResponse.CategorizedChanges changes = new SyncResponse.CategorizedChanges(
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

        SyncResponse.ScriptChange failed = SyncResponse.ScriptChange.failure(
                "SYSTEM", req != null ? req.getLreProject() : "SYNC",
                "emptySha", phase, msg, "Subject\\"
        );

        SyncResponse.SyncSummary summary = new SyncResponse.SyncSummary(
                0, 0, 0, 1
        );

        SyncResponse.CategorizedChanges changes = new SyncResponse.CategorizedChanges(
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

    public static List<SyncResponse.ScriptChange> buildUnchangedList(List<GitLabCommit> commits) {
        return commits.stream()
                .map(commit -> {
                    TestPlanCreationRequest info = PathUtils.fromGitPath(commit.getPath());
                    String folderPath = PathUtils.normalizePathWithSubject(info.getPath());
                    String commitSha = commit.getSha().substring(0, Math.min(8, commit.getSha().length()));
                    return SyncResponse.ScriptChange.unchanged(
                            commit.getPath(),
                            info.getName(),
                            commitSha,
                            folderPath
                    );
                })
                .toList();
    }
}

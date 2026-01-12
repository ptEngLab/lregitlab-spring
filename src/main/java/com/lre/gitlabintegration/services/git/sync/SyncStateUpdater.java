package com.lre.gitlabintegration.services.git.sync;

import com.lre.gitlabintegration.dto.gitlab.GitLabCommit;
import com.lre.gitlabintegration.dto.sync.ScriptChange;
import com.lre.gitlabintegration.dto.sync.SyncContext;
import com.lre.gitlabintegration.dto.sync.SyncRequest;
import com.lre.gitlabintegration.dto.sync.SyncResult;
import com.lre.gitlabintegration.dto.sync.SyncStateEntry;
import com.lre.gitlabintegration.repository.SyncStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncStateUpdater {

    private final SyncStateRepository stateRepository;

    public void updateStateWithPartialSuccess(
            SyncContext ctx,
            SyncResult result,
            List<ScriptChange> uploadedSuccess,
            List<ScriptChange> deletedSuccess
    ) {
        SyncRequest req = ctx.request();
        List<SyncStateEntry> previous = ctx.previous();
        List<GitLabCommit> current = ctx.current();

        Map<String, SyncStateEntry> previousByPath = indexByPath(previous, SyncStateEntry::path);
        Map<String, GitLabCommit> currentByPath = indexByPath(current, GitLabCommit::getPath);

        Set<String> uploadedPaths = uploadedSuccess.stream()
                .map(ScriptChange::path)
                .collect(Collectors.toSet());

        Set<String> deletedPaths = deletedSuccess.stream()
                .map(ScriptChange::path)
                .collect(Collectors.toSet());

        LinkedHashMap<String, GitLabCommit> state = new LinkedHashMap<>();
        Map<String, Integer> idsByPath = new HashMap<>();

        // 1) unchanged
        for (GitLabCommit commit : result.unchangedScripts()) {
            putCommit(commit, null, state, idsByPath, previousByPath);
        }

        // 2) successful uploads
        for (ScriptChange change : uploadedSuccess) {
            GitLabCommit commit = currentByPath.get(change.path());
            putCommit(commit, change.lreScriptId(), state, idsByPath, previousByPath);
        }

        // 3) failed uploads
        for (String path : failedUploadPaths(result, uploadedPaths)) {
            GitLabCommit commit = currentByPath.get(path);
            SyncStateEntry prev = previousByPath.get(path);
            if (prev != null) putCommit(commit, prev.lreScriptId(), state, idsByPath, previousByPath);

        }

        // 4) failed deletes
        for (String path : failedDeletePaths(result, deletedPaths)) {
            GitLabCommit commit = currentByPath.get(path);
            putCommit(commit, null, state, idsByPath, previousByPath);
            log.debug("Keeping script {} in state for delete retry", path);
        }

        // 5) remove successful deletes
        for (String path : deletedPaths) {
            state.remove(path);
            idsByPath.remove(path);
        }

        List<GitLabCommit> newState = List.copyOf(state.values());
        stateRepository.saveCommits(req, newState, idsByPath);

        logStateSummary(result, uploadedPaths, deletedPaths, newState);
    }

    private <T> Map<String, T> indexByPath(List<T> list, Function<T, String> pathExtractor) {
        return list.stream()
                .filter(Objects::nonNull)
                .filter(e -> pathExtractor.apply(e) != null)
                .collect(Collectors.toMap(
                        pathExtractor,
                        Function.identity(),
                        (a, b) -> a
                ));
    }

    private Set<String> failedUploadPaths(SyncResult result, Set<String> uploadedPaths) {
        return result.scriptsToUpload().stream()
                .map(GitLabCommit::getPath)
                .filter(path -> path != null && !uploadedPaths.contains(path))
                .collect(Collectors.toSet());
    }

    private Set<String> failedDeletePaths(SyncResult result, Set<String> deletedPaths) {
        return result.scriptsToDelete().stream()
                .map(SyncStateEntry::path)
                .filter(path -> path != null && !deletedPaths.contains(path))
                .collect(Collectors.toSet());
    }

    private void putCommit(
            GitLabCommit commit,
            Integer explicitId,
            Map<String, GitLabCommit> state,
            Map<String, Integer> idsByPath,
            Map<String, SyncStateEntry> previousByPath
    ) {
        if (commit == null || commit.getPath() == null) return;

        state.put(commit.getPath(), commit);

        if (explicitId != null) {
            idsByPath.put(commit.getPath(), explicitId);
            return;
        }

        SyncStateEntry prev = previousByPath.get(commit.getPath());
        if (prev != null && prev.lreScriptId() != null) {
            idsByPath.put(commit.getPath(), prev.lreScriptId());
        }
    }

    private void logStateSummary(
            SyncResult result,
            Set<String> uploadedPaths,
            Set<String> deletedPaths,
            List<GitLabCommit> newState
    ) {
        int failedUploads = result.scriptsToUpload().size() - uploadedPaths.size();
        int failedDeletes = result.scriptsToDelete().size() - deletedPaths.size();

        log.info(
                "Rebuilt state: {} total scripts (uploaded={}, deleted={}, unchanged={}, failedUploads={}, failedDeletes={})",
                newState.size(),
                uploadedPaths.size(),
                deletedPaths.size(),
                result.unchangedScripts().size(),
                failedUploads,
                failedDeletes
        );
    }
}

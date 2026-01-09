package com.lre.gitlabintegration.services.git.sync;

import com.lre.gitlabintegration.dto.gitlab.GitLabCommit;
import com.lre.gitlabintegration.dto.sync.SyncContext;
import com.lre.gitlabintegration.dto.sync.SyncResponse;
import com.lre.gitlabintegration.dto.sync.SyncResult;
import com.lre.gitlabintegration.repository.SyncStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncStateUpdater {

    private final SyncStateRepository stateRepository;


    /**
     * Updates state based on partial success results.
     * New state = unchanged + successful uploads + failed uploads (old) + failed deletes (old) - successful deletes
     */
    public void updateStateWithPartialSuccess(
            SyncContext ctx,
            SyncResult result,
            List<SyncResponse.ScriptChange> uploadedSuccess,
            List<SyncResponse.ScriptChange> deletedSuccess
    ) {
        List<GitLabCommit> previous = ctx.previous();
        List<GitLabCommit> current = ctx.current();

        // Extract paths
        Set<String> uploadedPaths = extractPaths(uploadedSuccess);
        Set<String> deletedPaths = extractPaths(deletedSuccess);

        // Build new state
        Map<String, GitLabCommit> state = new LinkedHashMap<>();

        // 1. Add unchanged scripts
        addUnchangedScripts(state, result);

        // 2. Add successfully uploaded scripts from current
        addSuccessfulUploads(state, current, uploadedPaths);

        // 3. Add failed uploads from previous (for retry)
        addFailedUploads(state, previous, result, uploadedPaths);

        // 4. Add failed deletes from previous (for retry)
        addFailedDeletes(state, previous, result, deletedPaths);

        // 5. Remove successfully deleted scripts
        removeSuccessfulDeletes(state, deletedPaths);

        // Save new state
        List<GitLabCommit> newState = List.copyOf(state.values());
        stateRepository.saveCommits(ctx.request(), newState);

        // Calculate metrics
        int failedUploads = result.scriptsToUpload().size() - uploadedPaths.size();
        int failedDeletes = result.scriptsToDelete().size() - deletedPaths.size();

        log.info(
                "Updated state: {} total scripts ({} uploaded, {} deleted, {} unchanged, {} failed uploads, {} failed deletes)",
                newState.size(),
                uploadedPaths.size(),
                deletedPaths.size(),
                result.unchangedScripts().size(),
                failedUploads,
                failedDeletes
        );
    }
        /**
         * Adds unchanged scripts to state.
         */
    private void addUnchangedScripts(Map<String, GitLabCommit> state,
                                     SyncResult result) {
        for (GitLabCommit commit : result.unchangedScripts()) {
            state.put(commit.getPath(), commit);
        }
    }

    /**
     * Adds successfully uploaded scripts from current commits.
     */
    private void addSuccessfulUploads(Map<String, GitLabCommit> state,
                                      List<GitLabCommit> current,
                                      Set<String> uploadedPaths) {
        for (GitLabCommit commit : current) {
            if (uploadedPaths.contains(commit.getPath())) {
                state.put(commit.getPath(), commit);
            }
        }
    }


    /**
     * Adds failed uploads from previous state (to retry next time).
     */
    private void addFailedUploads(Map<String, GitLabCommit> state,
                                  List<GitLabCommit> previous,
                                  SyncResult result,
                                  Set<String> uploadedPaths) {

        // Get paths that failed to upload
        Set<String> failedUploadPaths = result.scriptsToUpload().stream()
                .map(GitLabCommit::getPath)
                .filter(path -> !uploadedPaths.contains(path))
                .collect(Collectors.toSet());

        if (failedUploadPaths.isEmpty()) {
            return;
        }

        // Create map of previous commits for quick lookup
        Map<String, GitLabCommit> previousByPath = previous.stream()
                .collect(Collectors.toMap(
                        GitLabCommit::getPath,
                        commit -> commit,
                        (GitLabCommit a, GitLabCommit b) -> a
                ));

        // Add failed uploads from previous state
        for (String path : failedUploadPaths) {
            GitLabCommit oldCommit = previousByPath.get(path);
            if (oldCommit != null) {
                state.put(path, oldCommit);
            }
        }
    }

    /**
     * Adds failed deletes from previous state (to retry next time).
     */
    private void addFailedDeletes(Map<String, GitLabCommit> state,
                                  List<GitLabCommit> previous,
                                  SyncResult result,
                                  Set<String> deletedPaths) {

        // Get paths that failed to delete
        Set<String> failedDeletePaths = result.scriptsToDelete().stream()
                .map(GitLabCommit::getPath)
                .filter(path -> !deletedPaths.contains(path))
                .collect(Collectors.toSet());

        if (failedDeletePaths.isEmpty()) {
            return;
        }

        // Create map of previous commits for quick lookup
        Map<String, GitLabCommit> previousByPath = previous.stream()
                .collect(Collectors.toMap(
                        GitLabCommit::getPath,
                        commit -> commit,
                        (GitLabCommit a, GitLabCommit b) -> a
                ));

        // Add failed deletes from previous state
        for (String path : failedDeletePaths) {
            GitLabCommit oldCommit = previousByPath.get(path);
            if (oldCommit != null) {
                state.put(path, oldCommit);
                log.debug("Keeping script {} in state for delete retry", path);
            }
        }
    }


    /**
     * Removes successfully deleted scripts from state.
     */
    private void removeSuccessfulDeletes(Map<String, GitLabCommit> state,
                                         Set<String> deletedPaths) {
        for (String path : deletedPaths) {
            state.remove(path);
        }
    }

    /**
     * Extracts paths from script changes.
     */
    private Set<String> extractPaths(List<SyncResponse.ScriptChange> changes) {
        return changes.stream()
                .map(SyncResponse.ScriptChange::path)
                .collect(Collectors.toSet());
    }


}

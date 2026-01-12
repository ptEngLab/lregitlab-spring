package com.lre.gitlabintegration.services.git.sync;

import com.lre.gitlabintegration.client.api.LreTestPlanApiClient;
import com.lre.gitlabintegration.dto.gitlab.GitLabCommit;
import com.lre.gitlabintegration.dto.lrescript.Script;
import com.lre.gitlabintegration.dto.sync.ScriptChange;
import com.lre.gitlabintegration.dto.sync.SyncRequest;
import com.lre.gitlabintegration.dto.sync.SyncStateEntry;
import com.lre.gitlabintegration.dto.testplan.TestPlan;
import com.lre.gitlabintegration.dto.testplan.TestPlanCreationRequest;
import com.lre.gitlabintegration.util.path.PathUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.lre.gitlabintegration.util.constants.AppConstants.STATUS_FAILED;
import static com.lre.gitlabintegration.util.constants.AppConstants.STATUS_SUCCESS;
import static com.lre.gitlabintegration.util.path.PathUtils.normalizePathWithSubject;
import static org.apache.commons.lang3.StringUtils.truncate;

@Slf4j
@Service
@RequiredArgsConstructor
public class LreSyncService {

    private final GitScriptPackager scriptPackager;
    private final LreScriptManager scriptManager;
    private final LreTestPlanApiClient testPlanApiClient;

    private static final int MESSAGE_LIMIT = 200;
    private static final String ACTION_UPLOAD = "UPLOAD";
    private static final String ACTION_DELETE = "DELETE";


    /**
     * Uploads scripts from Git to LRE
     *
     * @return List of script changes with status
     */
    public List<ScriptChange> uploadScripts(
            SyncRequest req,
            List<GitLabCommit> commits
    ) {
        if (commits == null || commits.isEmpty()) {
            log.info("No commits to upload");
            return List.of();
        }

        String domain = req.getLreDomain();
        String project = req.getLreProject();

        log.info("Uploading {} scripts to LRE", commits.size());

        List<TestPlan> testPlans = testPlanApiClient.fetchAllTestPlans(domain, project);
        log.debug("Fetched {} test plans for batch upload", testPlans.size()
        );

        List<ScriptChange> changes = new ArrayList<>();

        for (GitLabCommit commit : commits) {
            TestPlanCreationRequest info = PathUtils.fromGitPath(commit.getPath());

            String folderPath = normalizePathWithSubject(info.getPath());

            String scriptName = info.getName();
            String commitSha = shortSha(commit.getSha());
            GitScriptPackager.PackagedScript packagedScript = null;

            try {
                log.debug("Preparing script for upload: {}", commit.getPath());
                packagedScript = scriptPackager.prepare(req.getGitlabProjectId(), commit);
                Script script = scriptManager.upload(domain, project, folderPath, packagedScript.zipPath(), testPlans);

                changes.add(ScriptChange.success(commit.getPath(), scriptName, commitSha, ACTION_UPLOAD, folderPath, script.getId()));

                log.debug("Successfully uploaded script: {}", scriptName);

            } catch (Exception e) {
                String msg = truncate(rootMessage(e), MESSAGE_LIMIT);

                log.error("Failed to upload script '{}' (commit {}): {}", commit.getPath(), commitSha, msg, e);

                changes.add(
                        ScriptChange.failure(
                                commit.getPath(), scriptName, commitSha, ACTION_UPLOAD, msg, folderPath)
                );
            } finally {
                if (packagedScript != null) {
                    scriptPackager.cleanUp(packagedScript);
                }
            }
        }

        Counts counts = countStatuses(changes);

        log.info("Upload complete: {} succeeded, {} failed", counts.success(), counts.failed());

        return changes;
    }

    /**
     * Deletes scripts from LRE that no longer exist in Git
     *
     * @return List of script changes with status
     */
    public List<ScriptChange> deleteScripts(SyncRequest req, List<SyncStateEntry> commits) {
        if (commits == null || commits.isEmpty()) {
            log.info("No scripts to delete");
            return List.of();
        }


        log.info("Deleting {} scripts from LRE", commits.size());

        List<ScriptChange> changes = new ArrayList<>();

        for (SyncStateEntry stateEntry : commits) {

            String commitSha = shortSha(stateEntry.commitSha());

            TestPlanCreationRequest info = PathUtils.fromGitPath(stateEntry.path());

            String folderPath = normalizePathWithSubject(info.getPath());

            String scriptName = info.getName();

            try {
                scriptManager.delete(req.getLreDomain(), req.getLreProject(), folderPath, scriptName);

                changes.add(
                        ScriptChange.success(stateEntry.path(), scriptName, commitSha, ACTION_DELETE, folderPath, null));

                log.debug("Successfully deleted script: {}", scriptName);

            } catch (Exception e) {
                String msg = truncate(rootMessage(e), MESSAGE_LIMIT);

                log.error("Failed to delete script '{}' (commit {}): {}", stateEntry.path(), commitSha, msg, e);

                changes.add(ScriptChange.failure(stateEntry.path(), scriptName, commitSha, ACTION_DELETE, msg, folderPath));
            }
        }


        Counts counts = countStatuses(changes);

        log.info("Delete complete: {} succeeded, {} failed", counts.success(), counts.failed());

        return changes;
    }

    /**
     * Logs a summary of sync operations
     */
    public void logSyncSummary(
            List<ScriptChange> changes
    ) {
        if (changes.isEmpty()) {
            log.info("No sync operations performed");
            return;
        }

        Counts counts = countStatuses(changes);

        log.info("Sync Summary: {} total, {} succeeded, {} failed", changes.size(), counts.success(), counts.failed());

        // Log failures
        changes.stream()
                .filter(c -> STATUS_FAILED.equals(c.status()))
                .forEach(c -> log.warn("Failed: {} - {}", c.scriptName(), c.message()));
    }

    /**
     * Gets the root cause message from an exception
     */
    private String rootMessage(Throwable t) {
        if (t == null) return "Unknown error";
        Throwable cur = t;
        while (cur.getCause() != null) cur = cur.getCause();
        String msg = cur.getMessage();
        return msg != null ? msg : cur.getClass().getSimpleName();
    }

    private static Counts countStatuses(List<ScriptChange> changes) {
        long success = 0;
        long failed = 0;

        for (var c : changes) {
            if (STATUS_SUCCESS.equals(c.status())) success++;
            else if (STATUS_FAILED.equals(c.status())) failed++;
        }
        return new Counts(success, failed);
    }

    private static String shortSha(String sha) {
        if (sha == null || sha.isBlank()) return "-";
        return sha.substring(0, Math.min(8, sha.length()));
    }


    private record Counts(long success, long failed) {
    }

}

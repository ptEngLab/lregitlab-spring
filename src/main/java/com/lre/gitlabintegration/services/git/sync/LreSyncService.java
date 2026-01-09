package com.lre.gitlabintegration.services.git.sync;

import com.lre.gitlabintegration.client.api.LreTestPlanApiClient;
import com.lre.gitlabintegration.dto.gitlab.GitLabCommit;
import com.lre.gitlabintegration.dto.sync.SyncRequest;
import com.lre.gitlabintegration.dto.sync.SyncResponse;
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

    /**
     * Uploads scripts from Git to LRE
     *
     * @return List of script changes with status
     */
    public List<SyncResponse.ScriptChange> uploadScripts(
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

        List<TestPlan> testPlans =
                testPlanApiClient.fetchAllTestPlans(domain, project);
        log.debug(
                "Fetched {} test plans for batch upload",
                testPlans.size()
        );

        List<SyncResponse.ScriptChange> changes = new ArrayList<>();

        for (GitLabCommit commit : commits) {
            TestPlanCreationRequest info = PathUtils.fromGitPath(commit.getPath());

            String folderPath = normalizePathWithSubject(info.getPath());

            String scriptName = info.getName();
            String commitSha = commit.getSha().substring(0, Math.min(8, commit.getSha().length()));

            GitScriptPackager.PackagedScript packagedScript = null;

            try {
                log.debug("Preparing script for upload: {}", commit.getPath());
                packagedScript = scriptPackager.prepare(req.getGitlabProjectId(), commit);
                scriptManager.upload(domain, project, folderPath, packagedScript.zipPath(), testPlans);

                changes.add(SyncResponse.ScriptChange.success(commit.getPath(), scriptName, commitSha, "UPLOADED", folderPath));

                log.debug("Successfully uploaded script: {}", scriptName);

            } catch (Exception e) {
                String msg =
                        truncate(rootMessage(e), MESSAGE_LIMIT);

                log.error(
                        "Failed to upload script '{}' (commit {}): {}",
                        commit.getPath(),
                        commitSha,
                        msg,
                        e
                );

                changes.add(
                        SyncResponse.ScriptChange.failure(
                                commit.getPath(),
                                scriptName,
                                commitSha,
                                "UPLOAD",
                                msg,
                                folderPath
                        )
                );
            } finally {
                if (packagedScript != null) {
                    scriptPackager.cleanUp(packagedScript);
                }
            }
        }

        long successCount =
                changes.stream()
                        .filter(c ->
                                STATUS_SUCCESS.equals(c.status()))
                        .count();

        long failCount =
                changes.stream()
                        .filter(c ->
                                STATUS_FAILED.equals(c.status()))
                        .count();

        log.info(
                "Upload complete: {} succeeded, {} failed",
                successCount,
                failCount
        );

        return changes;
    }

    /**
     * Deletes scripts from LRE that no longer exist in Git
     *
     * @return List of script changes with status
     */
    public List<SyncResponse.ScriptChange> deleteScripts(
            SyncRequest req,
            List<GitLabCommit> commits
    ) {
        if (commits == null || commits.isEmpty()) {
            log.info("No scripts to delete");
            return List.of();
        }

        log.info("Deleting {} scripts from LRE", commits.size());

        List<SyncResponse.ScriptChange> changes = new ArrayList<>();

        for (GitLabCommit commit : commits) {
            String commitSha =
                    commit.getSha()
                            .substring(0,
                                    Math.min(8, commit.getSha().length()));

            TestPlanCreationRequest info =
                    PathUtils.fromGitPath(commit.getPath());

            String folderPath =
                    normalizePathWithSubject(info.getPath());

            String scriptName = info.getName();

            try {
                scriptManager.delete(
                        req.getLreDomain(),
                        req.getLreProject(),
                        folderPath,
                        scriptName
                );

                changes.add(
                        SyncResponse.ScriptChange.success(
                                commit.getPath(),
                                scriptName,
                                commitSha,
                                "DELETED",
                                folderPath
                        )
                );

                log.debug(
                        "Successfully deleted script: {}",
                        scriptName
                );

            } catch (Exception e) {
                String msg =
                        truncate(rootMessage(e), MESSAGE_LIMIT);

                log.error(
                        "Failed to delete script '{}' (commit {}): {}",
                        commit.getPath(),
                        commitSha,
                        msg
                );

                changes.add(
                        SyncResponse.ScriptChange.failure(
                                commit.getPath(),
                                scriptName,
                                commitSha,
                                "DELETE",
                                msg,
                                folderPath
                        )
                );
            }
        }

        long successCount =
                changes.stream()
                        .filter(c ->
                                STATUS_SUCCESS.equals(c.status()))
                        .count();

        long failCount =
                changes.stream()
                        .filter(c ->
                                STATUS_FAILED.equals(c.status()))
                        .count();

        log.info(
                "Delete complete: {} succeeded, {} failed",
                successCount,
                failCount
        );

        return changes;
    }

    /**
     * Logs a summary of sync operations
     */
    public void logSyncSummary(
            List<SyncResponse.ScriptChange> changes
    ) {
        if (changes.isEmpty()) {
            log.info("No sync operations performed");
            return;
        }

        long successCount =
                changes.stream()
                        .filter(c ->
                                STATUS_SUCCESS.equals(c.status()))
                        .count();

        long failCount =
                changes.stream()
                        .filter(c ->
                                STATUS_FAILED.equals(c.status()))
                        .count();

        log.info(
                "Sync Summary: {} total, {} succeeded, {} failed",
                changes.size(),
                successCount,
                failCount
        );

        // Log failures
        changes.stream()
                .filter(c ->
                        STATUS_FAILED.equals(c.status()))
                .forEach(c ->
                        log.warn(
                                "Failed: {} - {}",
                                c.scriptName(),
                                c.message()
                        ));
    }

    /**
     * Gets the root cause message from an exception
     */
    private String rootMessage(Throwable t) {
        if (t == null) {
            return "Unknown error";
        }

        Throwable cur = t;
        while (cur.getCause() != null) {
            cur = cur.getCause();
        }

        String msg = cur.getMessage();
        return msg != null
                ? msg
                : cur.getClass().getSimpleName();
    }
}

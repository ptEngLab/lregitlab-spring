package com.lre.gitlabintegration.services.git.sync;

import com.lre.gitlabintegration.client.api.LreScriptApiClient;
import com.lre.gitlabintegration.client.api.LreTestPlanApiClient;
import com.lre.gitlabintegration.dto.lrescript.Script;
import com.lre.gitlabintegration.dto.lrescript.ScriptUploadRequest;
import com.lre.gitlabintegration.dto.testplan.TestPlan;
import com.lre.gitlabintegration.exceptions.LreException;
import com.lre.gitlabintegration.util.path.PathUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles uploading and deleting scripts in LoadRunner Enterprise.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LreScriptManager {

    private final LreScriptApiClient scriptApiClient;
    private final LreTestPlanApiClient testPlanApiClient;

    /**
     * Uploads a script to LRE
     */
    public Script upload(String domain, String project, String folderPath, Path scriptZip, List<TestPlan> testPlans) {
        log.debug("Uploading script {} to LRE, domain = {}, project = {}, folder = {}", scriptZip.getFileName(), domain, project, folderPath);
        String normalizedPath = PathUtils.normalizePathWithSubject(folderPath);
        String validatedFolderPath = validateAndEnsureTestPlanPath(domain, project, normalizedPath, testPlans);
        ScriptUploadRequest req = new ScriptUploadRequest(validatedFolderPath);
        return scriptApiClient.uploadScript(domain, project, req, scriptZip);
    }

    /**
     * Deletes a script from LRE
     */
    public void delete(String domain, String project, String folderPath, String scriptName) {
        Script lreScript = findScriptByName(domain, project, folderPath, scriptName);

        scriptApiClient.deleteScript(domain, project, lreScript.getId());

        log.debug("Deleted script: {} / {}", folderPath, scriptName);
    }

    public Script findScriptByName(String domain, String project, String testFolderPath, String scriptName) {
        String normalizedPath = PathUtils.normalizePathWithSubject(testFolderPath).toLowerCase(Locale.ROOT);
        String normalizedName = scriptName.toLowerCase(Locale.ROOT);
        log.debug(
                "Searching for script. Domain = {}, Project = {}, Folder = {}, Name = {}",
                domain, project, testFolderPath, scriptName);
        List<Script> scriptCache = scriptApiClient.getAllScripts(domain, project);

        for (Script s : scriptCache) {
            String sFolder = PathUtils.normalizePathWithSubject(s.getTestFolderPath()).toLowerCase(Locale.ROOT);
            String sName = s.getName().toLowerCase(Locale.ROOT);
            if (normalizedPath.equals(sFolder) && normalizedName.equals(sName)) return s;
        }

        String msg = String.format("No Script named '%s' found under folder %s", scriptName, testFolderPath);
        log.warn(msg);
        throw new LreException(msg);
    }

    private String validateAndEnsureTestPlanPath(String domain, String project, String inputFolderPath, List<TestPlan> currentTestPlans) {
        Set<String> existingPathStrings = currentTestPlans.stream()
                .map(plan -> PathUtils.normalizePathWithSubject(plan.getFullPath()).toLowerCase())
                .collect(Collectors.toSet());

        String[] pathParts = inputFolderPath.split("\\\\");
        StringBuilder currentPathBuilder = new StringBuilder("Subject");

        // Process subdirectories starting from index 1
        for (int i = 1; i < pathParts.length; i++) {
            String pathPart = pathParts[i];
            String parentPath = currentPathBuilder.toString();

            // Build current path
            currentPathBuilder.append("\\").append(pathPart);
            String currentPath = currentPathBuilder.toString();

            if (!existingPathStrings.contains(currentPath.toLowerCase())) {
                TestPlan createdTestPlan = testPlanApiClient.createTestPlan(domain, project, parentPath, pathPart);
                currentTestPlans.add(createdTestPlan);
                existingPathStrings.add(currentPath.toLowerCase());
            }
        }

        String finalPath = currentPathBuilder.toString();
        log.debug("Test plan validated/created: {}", finalPath);
        return finalPath;
    }
}

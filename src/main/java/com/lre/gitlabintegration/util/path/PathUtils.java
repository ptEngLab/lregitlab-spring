package com.lre.gitlabintegration.util.path;

import com.lre.gitlabintegration.dto.testplan.TestPlanCreationRequest;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

@UtilityClass
public class PathUtils {

    public static String normalizePathWithSubject(String path) {
        if (StringUtils.isBlank(path)) {
            return "Subject";
        }

        // Step 1: Normalize all slashes to backslashes
        String normalized = path.replace("/", "\\");

        // Step 2: Remove repeated backslashes with possessive quantifier
        normalized = normalized.replaceAll("\\\\++", "\\");

        // Step 3: Trim leading backslashes with possessive quantifier
        normalized = normalized.replaceAll("^\\\\++", "");

        // Step 4: Trim trailing backslashes with possessive quantifier
        while (normalized.endsWith("\\")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        // Step 5: Ensure "Subject" prefix (case-insensitive)
        if (!normalized.toLowerCase().startsWith("subject\\")) {
            normalized = "Subject\\" + normalized;
        }

        return normalized;
    }

    public static String replaceBackSlash(String input) {
        return input.replace("\\", "/");
    }

    public static TestPlanCreationRequest fromGitPath(String gitPath) {
        String normalized = replaceBackSlash(gitPath);
        int lastSlash = normalized.lastIndexOf('/');
        String name = normalized.substring(lastSlash + 1);
        String folder = lastSlash > 0
                ? normalized.substring(0, lastSlash)
                : "Gitlab";
        return new TestPlanCreationRequest(folder, name);
    }
}

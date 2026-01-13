package com.lre.gitlabintegration.services.git.sync;

import com.lre.gitlabintegration.client.api.GitLabApiClient;
import com.lre.gitlabintegration.dto.gitlab.GitLabCommit;
import com.lre.gitlabintegration.exceptions.LreException;
import com.lre.gitlabintegration.util.io.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
public class GitScriptPackager {

    private final GitLabApiClient gitLabApiClient;
    private final Path rootSyncDir;

    public GitScriptPackager(GitLabApiClient gitLabApiClient) {
        this.gitLabApiClient = gitLabApiClient;
        this.rootSyncDir = Paths.get(System.getProperty("java.io.tmpdir"), "lre_sync");
    }

    public record PackagedScript(Path zipPath, Path tempDir) {
    }

    public PackagedScript prepare(long projectId, GitLabCommit commit) throws IOException {

        Files.createDirectories(rootSyncDir);

        Path commitTempDir = createCommitTempDir(commit);
        Path archiveDir = commitTempDir.resolve("archive");
        Path extractedDir = commitTempDir.resolve("extracted");
        Path packagedDir = commitTempDir.resolve("packaged");

        Files.createDirectories(archiveDir);
        Files.createDirectories(extractedDir);
        Files.createDirectories(packagedDir);

        try {
            Path archivePath = archiveDir.resolve("gitlab-archive.zip");
            log.debug(
                    "Downloading repository archive for commit {} into {}",
                    commit.getSha(),
                    archivePath
            );

            boolean ok = gitLabApiClient.downloadRepositoryArchive(
                    projectId,
                    commit.getSha(),
                    commit.getPath(),
                    archivePath
            );

            if (!ok) {
                throw new LreException(
                        "Failed to download repository archive for: " + commit.getPath()
                );
            }

            FileUtils.unzip(archivePath.toFile(), extractedDir.toFile());

            Path usrParent = findUsrParent(extractedDir);

            Path packagedZip = packagedDir.resolve(generateZipFileName(commit));
            FileUtils.createZipFile(usrParent, packagedZip);

            log.debug(
                    "Packaged {} into {} ({} bytes)",
                    commit.getPath(),
                    packagedZip,
                    Files.size(packagedZip)
            );

            // Delete intermediate folders
            FileUtils.deleteFolder(archiveDir);
            FileUtils.deleteFolder(extractedDir);

            return new PackagedScript(packagedZip, commitTempDir);

        } catch (Exception e) {
            FileUtils.deleteFolder(commitTempDir);
            throw e;
        }
    }

    private Path findUsrParent(Path root) throws IOException {
        try (var stream = Files.walk(root)) {
            return stream
                    .filter(p -> p.toString().endsWith(".usr"))
                    .findFirst()
                    .map(Path::getParent)
                    .orElseThrow(() ->
                            new IOException(
                                    ".usr file not found in extracted archive: " + root
                            ));
        }
    }

    private String generateZipFileName(GitLabCommit commit) {
        String path = commit.getPath();
        String lastSegment =
                path.contains("/")
                        ? path.substring(path.lastIndexOf('/') + 1)
                        : path;

        String safePath = lastSegment
                .replaceAll("[^a-zA-Z0-9._-]", "_")
                .toLowerCase()
                .replace(".usr", "");

        String shaShort =
                commit.getSha().length() > 8
                        ? commit.getSha().substring(0, 8).toLowerCase()
                        : commit.getSha().toLowerCase();

        return String.format("%s-%s.zip", safePath, shaShort);
    }

    /**
     * Creates a lowercase commit-specific temp directory under rootSyncDir
     * using the last segment of commit.path.
     */
    private Path createCommitTempDir(GitLabCommit commit) {
        String path = commit.getPath();
        String lastSegment =
                path.contains("/")
                        ? path.substring(path.lastIndexOf('/') + 1)
                        : path;

        String safeName = lastSegment
                .replaceAll("[^a-zA-Z0-9._-]", "_")
                .toLowerCase();

        String uniqueName = safeName + "-" + System.nanoTime();
        return rootSyncDir.resolve(uniqueName);
    }

    public void cleanUp(PackagedScript packagedScript) {
        if (packagedScript != null) {
            FileUtils.deleteFolder(packagedScript.tempDir());
        }
    }
}

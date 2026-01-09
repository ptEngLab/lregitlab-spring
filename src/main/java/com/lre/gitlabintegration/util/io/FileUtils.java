package com.lre.gitlabintegration.util.io;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@UtilityClass
public class FileUtils {

    public static void createZipFile(Path sourceDir, Path zipFile) throws IOException {
        try (ZipOutputStream zos =
                     new ZipOutputStream(Files.newOutputStream(zipFile))) {

            Files.walkFileTree(sourceDir, new SimpleFileVisitor<>() {

                @Override
                public @NonNull FileVisitResult preVisitDirectory(
                        @NonNull Path dir,
                        @NonNull BasicFileAttributes attrs
                ) throws IOException {

                    String entryName =
                            sourceDir
                                    .relativize(dir)
                                    .toString()
                                    .replace(File.separator, "/") + "/";

                    zos.putNextEntry(new ZipEntry(entryName));
                    zos.closeEntry();

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public @NonNull FileVisitResult visitFile(
                        @NonNull Path file,
                        @NonNull BasicFileAttributes attrs
                ) throws IOException {

                    String entryName =
                            sourceDir
                                    .relativize(file)
                                    .toString()
                                    .replace(File.separator, "/");

                    zos.putNextEntry(new ZipEntry(entryName));
                    Files.copy(file, zos);
                    zos.closeEntry();

                    return FileVisitResult.CONTINUE;
                }
            });

        } catch (IOException e) {
            log.error(
                    "Error creating zip file {}: {}",
                    zipFile,
                    e.getMessage(),
                    e
            );
            throw e;
        }
    }

    public static void unzip(File zipFile, File destDir) throws IOException {
        Path destPath = destDir.toPath().toAbsolutePath().normalize();
        Files.createDirectories(destPath);

        try (FileSystem zipFs =
                     FileSystems.newFileSystem(zipFile.toPath(), (ClassLoader) null)) {

            extractZipContents(zipFs, destPath);

        } catch (UncheckedIOException e) {
            throw e.getCause();
        } catch (IOException e) {
            throw new IOException(
                    "Failed to unzip file: " + zipFile.getAbsolutePath(),
                    e
            );
        }
    }

    private static void extractZipContents(
            FileSystem zipFs,
            Path destPath
    ) throws IOException {

        for (Path root : zipFs.getRootDirectories()) {
            try (var stream = Files.walk(root)) {
                stream.forEach(source ->
                        extractEntry(root, source, destPath));
            }
        }
    }

    private static void extractEntry(
            Path root,
            Path source,
            Path destPath
    ) {
        try {
            Path relative = root.relativize(source);
            if (relative.toString().isEmpty()) {
                return;
            }

            Path destination =
                    destPath.resolve(relative.toString()).normalize();

            if (!destination.startsWith(destPath)) {
                throw new IOException(
                        "Zip entry outside target dir: " + relative
                );
            }

            if (Files.isSymbolicLink(source)) {
                throw new IOException(
                        "Symbolic link entries not allowed: " + relative
                );
            }

            if (Files.isDirectory(source)) {
                Files.createDirectories(destination);
            } else {
                Files.createDirectories(destination.getParent());
                Files.copy(
                        source,
                        destination,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to extract: " + source,
                    e
            );
        }
    }

    public static void deleteFolder(Path folder) {
        if (folder == null || !Files.exists(folder)) {
            return;
        }

        try {
            Files.walkFileTree(folder, new SimpleFileVisitor<>() {

                @Override
                public @NonNull FileVisitResult visitFile(
                        @NonNull Path file,
                        @NonNull BasicFileAttributes attrs
                ) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public @NonNull FileVisitResult postVisitDirectory(
                        @NonNull Path dir,
                        IOException exc
                ) throws IOException {
                    Files.deleteIfExists(dir);
                    return FileVisitResult.CONTINUE;
                }
            });

        } catch (IOException e) {
            log.warn(
                    "Failed to delete folder '{}': {}",
                    folder,
                    e.getMessage()
            );
        }
    }
}

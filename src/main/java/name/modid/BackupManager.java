package com.worldbackup;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.*;

public class BackupManager {

    private final Path savesPath;
    private final Path backupPath;
    private final Path logFile;
    private final ConfigManager config;

    public BackupManager() {
        this.config     = new ConfigManager();
        this.savesPath  = Paths.get(config.savesPath);
        this.backupPath = Paths.get(config.backupPath);
        this.logFile    = backupPath.resolve("backup_log.txt");
    }

    public String runBackup() {
        try {
            Files.createDirectories(backupPath);

            // Clean up any leftover temp folders from previous crashed runs
            cleanupTempFolders();

            if (!Files.exists(savesPath)) {
                return "ERROR: Saves folder not found at: " + savesPath +
                       "\nOpen worldbackup.config in your Downloads folder and update saves_path.";
            }

            String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm"));

            String result;

            if (config.enableCompression) {
                result = runCompressedBackup(timestamp);
            } else {
                result = runCopyBackup(timestamp);
            }

            if (config.enableRotation) rotateBackups();

            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: " + e.getMessage();
        }
    }

    private String runCompressedBackup(String timestamp) throws IOException {
        // Step 1: Copy saves to temp folder (avoids locked file issues)
        Path tempFolder = backupPath.resolve("temp_" + timestamp);
        try {
            Files.createDirectories(tempFolder);
            copyFolder(savesPath, tempFolder);

            // Step 2: Write zip to a .tmp file first
            Path tmpZip  = backupPath.resolve("Backup_" + timestamp + ".tmp");
            Path finalZip = backupPath.resolve("Backup_" + timestamp + ".zip");

            // Write and FULLY close the zip before doing anything else
            writeZip(tempFolder, tmpZip);

            // Step 3: Verify zip has real content before renaming
            if (Files.exists(tmpZip) && Files.size(tmpZip) > 100) {
                // Rename .tmp to .zip only after successful close
                Files.move(tmpZip, finalZip, StandardCopyOption.REPLACE_EXISTING);
                if (config.enableLogging) log("Backup completed: " + finalZip);
                return "SUCCESS: Saved -> " + finalZip.getFileName();
            } else {
                Files.deleteIfExists(tmpZip);
                return "ERROR: Zip was empty after writing. Try enable_compression=false";
            }

        } finally {
            // Always clean up temp folder even if something goes wrong
            if (Files.exists(tempFolder)) {
                deleteRecursively(tempFolder);
            }
        }
    }

    private String runCopyBackup(String timestamp) throws IOException {
        Path destination = backupPath.resolve("Backup_" + timestamp);
        copyFolder(savesPath, destination);
        if (config.enableLogging) log("Backup completed: " + destination);
        return "SUCCESS: Saved -> " + destination.getFileName();
    }

    private void writeZip(Path source, Path zipFile) throws IOException {
        // Use explicit stream management to ensure close() always completes
        ZipOutputStream zos = new ZipOutputStream(
            new BufferedOutputStream(new FileOutputStream(zipFile.toFile())));
        try {
            zos.setLevel(ZipOutputStream.DEFLATED);

            Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (!dir.equals(source)) {
                        String dirName = source.relativize(dir).toString().replace("\\", "/") + "/";
                        zos.putNextEntry(new ZipEntry(dirName));
                        zos.closeEntry();
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String entryName = source.relativize(file).toString().replace("\\", "/");
                    zos.putNextEntry(new ZipEntry(entryName));
                    Files.copy(file, zos);
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    System.out.println("[WorldBackup] Skipping: " + file + " (" + exc.getMessage() + ")");
                    return FileVisitResult.CONTINUE;
                }
            });
        } finally {
            // Explicitly flush and close — this writes the zip central directory
            zos.flush();
            zos.close();
        }
    }

    private void copyFolder(Path source, Path destination) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path target = destination.resolve(source.relativize(dir));
                Files.createDirectories(target);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path target = destination.resolve(source.relativize(file));
                Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                System.out.println("[WorldBackup] Skipping locked file: " + file);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void cleanupTempFolders() throws IOException {
        // Delete any temp_ folders left over from previous crashed runs
        try (var stream = Files.list(backupPath)) {
            stream.filter(p -> p.getFileName().toString().startsWith("temp_"))
                  .forEach(p -> {
                      try { deleteRecursively(p); }
                      catch (IOException e) { System.out.println("[WorldBackup] Could not clean temp: " + p); }
                  });
        }
        // Also delete any .tmp zip files left over
        try (var stream = Files.list(backupPath)) {
            stream.filter(p -> p.getFileName().toString().endsWith(".tmp"))
                  .forEach(p -> {
                      try { Files.deleteIfExists(p); }
                      catch (IOException e) { System.out.println("[WorldBackup] Could not clean .tmp: " + p); }
                  });
        }
    }

    private void rotateBackups() throws IOException {
        // Only count real Backup_ files/folders — ignore temp_ and .tmp
        List<Path> backups = Files.list(backupPath)
            .filter(p -> {
                String name = p.getFileName().toString();
                return name.startsWith("Backup_") && !name.endsWith(".tmp");
            })
            .sorted((a, b) -> {
                // Sort oldest first so we delete from the front
                try {
                    long timeA = Files.getLastModifiedTime(a).toMillis();
                    long timeB = Files.getLastModifiedTime(b).toMillis();
                    return Long.compare(timeA, timeB);
                } catch (IOException e) {
                    return 0;
                }
            })
            .collect(Collectors.toList());

        System.out.println("[WorldBackup] Found " + backups.size() + " backups, max is " + config.maxBackups);

        while (backups.size() > config.maxBackups) {
            Path oldest = backups.remove(0);
            System.out.println("[WorldBackup] Deleting old backup: " + oldest.getFileName());
            deleteRecursively(oldest);
        }
    }

    private void deleteRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            Files.deleteIfExists(path);
        }
    }

    private void log(String message) {
        try (BufferedWriter writer = Files.newBufferedWriter(logFile,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            writer.write("[" + LocalDateTime.now() + "] " + message);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

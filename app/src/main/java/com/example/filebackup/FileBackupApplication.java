package com.example.filebackup;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileBackupApplication {

    public static ScheduledExecutorService executor = null;

    public static void main(String[] args) {
        String mode = args == null || args.length == 0 ? null : args[0];
        if (!"stop".equals(mode)) {
            start();
        } else {
            stop();
        }
    }

    private static void start() {
        if (executor != null) {
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.schedule(FileBackupApplication::run, 1, TimeUnit.HOURS);
        run();
    }

    private static void stop() {
        if (executor == null) {
            return;
        }
        executor.shutdownNow();
        executor = null;
    }

    private static void run() {
        try {
            runInternal();
        } catch (Exception e) {
            log.info("Failed to run", e);
        }
    }

    private static void runInternal() {
        log.info("Starting running");

        Path cwd = getApplicationDirectory();

        log.info("cwd={}", cwd);

        FileBackupConfig config = loadConfig(findConfig(cwd));

        Path target = Paths.get(config.getTargetDirectory());
        Path output = Paths.get(config.getOutputDirectory())
                .resolve(config.getOutputFilePrefix() + "." + getTimestamp() + ".zip");

        log.info("Creating zip from {} to {}", target, output);

        try (OutputStream os = Files.newOutputStream(output); ZipOutputStream zos = new ZipOutputStream(os)) {
            Files.walkFileTree(target, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    String path = getDirectoryName(target, dir);
                    if (!"/".equals(path)) {
                        zos.putNextEntry(new ZipEntry(path));
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String path = getFileName(target, file);
                    zos.putNextEntry(new ZipEntry(path));
                    Files.copy(file, zos);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new FileBackupException("Failed to create zip: target=" + target + ", output=" + output, e);
        }

        log.info("Finished running");
    }

    private static Path getApplicationDirectory() {
        try {
            return Paths.get(FileBackupApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new FileBackupException("Failed to get application directory", e);
        }
    }

    private static Path findConfig(Path path) {
        String fileName = "config.properties";
        for (int i = 0; i < 3; i++) {
            log.info("Finding config: path={}", path);
            if (path.resolve(fileName).toFile().exists()) {
                return path.resolve(fileName);
            }
            path = path.getParent();
        }
        throw new FileBackupException("Failed to find config: path=" + path);
    }

    private static FileBackupConfig loadConfig(Path path) {
        Properties properties = new Properties();
        try (InputStream is = Files.newInputStream(path)) {
            properties.load(is);
        } catch (IOException e) {
            throw new FileBackupException("Failed to load config: path=" + path, e);
        }
        return FileBackupConfig.builder()
                .targetDirectory(properties.getProperty("target.directory"))
                .outputDirectory(properties.getProperty("output.directory"))
                .outputFilePrefix(properties.getProperty("output.fileprefix"))
                .build();
    }

    private static String getTimestamp() {
        LocalDateTime now = ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuuMMddHHmmss");
        return now.format(formatter);
    }

    private static String getDirectoryName(Path root, Path dir) {
        String path = getFileName(root, dir);
        return path.endsWith("/") ? path : path + '/';
    }

    private static String getFileName(Path root, Path file) {
        return root.relativize(file).toString().replace('\\', '/');
    }

    @Value
    @Builder
    private static class FileBackupConfig {

        private final String targetDirectory;
        private final String outputDirectory;
        private final String outputFilePrefix;
    }

    @SuppressWarnings("serial")
    private static class FileBackupException extends RuntimeException {

        public FileBackupException(String message) {
            super(message);
        }

        public FileBackupException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

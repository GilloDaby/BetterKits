package com.gillodaby.betterkits.util;

import com.hypixel.hytale.logger.HytaleLogger;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class KitAssetManager {

    private static final String UI_BASE_PATH = "kits/";
    private static final String JAR_BASE_PATH = "Common/UI/Custom/kits/";

    private final Path jarPath;
    private final Path dataFolder;
    private final HytaleLogger logger;

    public KitAssetManager(Path jarPath, Path dataFolder, HytaleLogger logger) {
        this.jarPath = jarPath;
        this.dataFolder = dataFolder;
        this.logger = logger;
    }

    public void packKitAssets() {
        try {
            logger.at(Level.INFO).log("Starting kit asset packing into " + JAR_BASE_PATH + "...");
            Files.createDirectories(dataFolder, new FileAttribute[0]);
            logger.at(Level.INFO).log("Data folder: " + dataFolder.toAbsolutePath());

            Map<String, Path> assets = new HashMap<>();
            if (Files.exists(dataFolder) && Files.isDirectory(dataFolder)) {
                Files.list(dataFolder)
                    .filter(this::isRegularFile)
                    .filter(this::isImageFile)
                    .forEach(path -> acceptAsset(assets, path));
            } else {
                logger.at(Level.WARNING).log("Data folder does not exist or is not a directory!");
            }

            if (assets.isEmpty()) {
                logger.at(Level.INFO).log("No assets to pack.");
                return;
            }

            boolean needsRepack = checkIfNeedsRepack(assets);
            logger.at(Level.INFO).log("Needs repack: " + needsRepack);
            if (!needsRepack) {
                logger.at(Level.INFO).log("Kit assets already up to date.");
                return;
            }

            boolean repacked = repackJarWithAssets(assets);
            if (repacked) {
                logger.at(Level.INFO).log("Packed " + assets.size() + " kit assets into JAR. Restart server to apply.");
            } else {
                logger.at(Level.WARNING).log("Failed to repack JAR (file not found or other error).");
            }
        } catch (Exception e) {
            logger.at(Level.WARNING).log("Failed to pack kit assets: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean checkIfNeedsRepack(Map<String, Path> assets) throws IOException {
        if (Files.exists(jarPath) && Files.isRegularFile(jarPath)) {
            try (InputStream input = Files.newInputStream(jarPath);
                 ZipInputStream zip = new ZipInputStream(new BufferedInputStream(input))) {
                int matches = 0;
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    if (assets.containsKey(entry.getName())) {
                        matches++;
                    }
                }
                logger.at(Level.INFO).log("Found " + matches + " existing assets in JAR out of " + assets.size() + " required.");
                return matches != assets.size();
            }
        }
        logger.at(Level.WARNING).log("JAR file not found at: " + jarPath);
        return true;
    }

    private boolean repackJarWithAssets(Map<String, Path> assets) throws IOException {
        logger.at(Level.INFO).log("Repacking JAR with new assets...");
        if (!Files.exists(jarPath) || !Files.isRegularFile(jarPath)) {
            return false;
        }

        Path tempJar = jarPath.resolveSibling(jarPath.getFileName().toString() + ".tmp");
        boolean success = false;

        try (InputStream input = Files.newInputStream(jarPath);
             ZipInputStream zip = new ZipInputStream(new BufferedInputStream(input));
             OutputStream out = Files.newOutputStream(tempJar);
             ZipOutputStream zout = new ZipOutputStream(out)) {

            Map<String, Path> pending = new HashMap<>(assets);
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                ZipEntry newEntry = new ZipEntry(name);
                newEntry.setTime(entry.getTime());
                zout.putNextEntry(newEntry);

                if (pending.containsKey(name)) {
                    logger.at(Level.INFO).log("Overwriting existing JAR entry: " + name);
                    Files.copy(pending.get(name), zout);
                    pending.remove(name);
                } else {
                    copyAll(zip, zout);
                }
                zout.closeEntry();
                zip.closeEntry();
            }

            for (Map.Entry<String, Path> pendingEntry : pending.entrySet()) {
                String name = pendingEntry.getKey();
                logger.at(Level.INFO).log("Adding new JAR entry: " + name);
                ZipEntry newEntry = new ZipEntry(name);
                zout.putNextEntry(newEntry);
                Files.copy(pendingEntry.getValue(), zout);
                zout.closeEntry();
            }

            zout.finish();
            zout.flush();
        }

        try {
            Files.move(tempJar, jarPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            logger.at(Level.INFO).log("JAR repack successful.");
            success = true;
        } catch (Exception ignored) {
            Files.move(tempJar, jarPath, StandardCopyOption.REPLACE_EXISTING);
            success = true;
        } finally {
            try {
                Files.deleteIfExists(tempJar);
            } catch (Exception ignored) {
            }
        }

        return success;
    }

    private void copyAll(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) >= 0) {
            if (read > 0) {
                output.write(buffer, 0, read);
            }
        }
    }

    public String getIconPath(String kitName) {
        if (kitName == null || kitName.isBlank()) {
            return null;
        }
        String normalized = kitName.toLowerCase().replace(" ", "_");
        String[] extensions = {".png", ".jpg", ".jpeg"};
        for (String ext : extensions) {
            Path candidate = dataFolder.resolve(normalized + ext);
            if (Files.exists(candidate) && Files.isRegularFile(candidate)) {
                return UI_BASE_PATH + normalized + ext;
            }
        }
        return null;
    }

    public String getBackgroundPath(String kitName) {
        if (kitName == null || kitName.isBlank()) {
            return null;
        }
        String normalized = kitName.toLowerCase().replace(" ", "_");
        String[] extensions = {".png", ".jpg", ".jpeg"};
        for (String ext : extensions) {
            Path candidate = dataFolder.resolve(normalized + "_bg" + ext);
            if (Files.exists(candidate) && Files.isRegularFile(candidate)) {
                return UI_BASE_PATH + normalized + "_bg" + ext;
            }
        }
        return null;
    }

    private boolean isRegularFile(Path path) {
        return Files.isRegularFile(path, new LinkOption[0]);
    }

    private boolean isImageFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        boolean ok = name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg");
        if (!ok) {
            logger.at(Level.INFO).log("Skipping non-image file: " + name);
        }
        return ok;
    }

    private void acceptAsset(Map<String, Path> assets, Path path) {
        String fileName = path.getFileName().toString();
        String jarEntry = JAR_BASE_PATH + fileName;
        assets.put(jarEntry, path);
        logger.at(Level.INFO).log("Found asset: " + fileName + " -> JAR path: " + jarEntry);
    }
}

package com.gillodaby.betterkits;

import com.gillodaby.betterkits.commands.KitCommand;
import com.gillodaby.betterkits.commands.KitsMenuCommand;
import com.gillodaby.betterkits.models.KitDefinition;
import com.gillodaby.betterkits.settings.KitGlobalSettings;
import com.gillodaby.betterkits.settings.KitSettings;
import com.gillodaby.betterkits.settings.KitSettingsRepository;
import com.gillodaby.betterkits.util.KitAssetManager;
import com.gillodaby.betterkits.util.JsonUtil;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class BetterKitsPlugin extends JavaPlugin {

    private static BetterKitsPlugin instance;

    private KitRepository repository;
    private KitSettingsRepository settingsRepository;
    private KitAssetManager assetManager;

    private final Map<UUID, Map<String, Long>> kitCooldowns;
    private final Map<UUID, Long> menuCommandCooldowns;

    private Path cooldownsFile;
    private Path menuCommandCooldownsFile;

    private final Map<UUID, KitDefinition> lastKitByPlayer;
    private final Map<UUID, String> editorSelectionByPlayer;

    public BetterKitsPlugin(JavaPluginInit init) {
        super(init);
        this.kitCooldowns = new ConcurrentHashMap<>();
        this.menuCommandCooldowns = new ConcurrentHashMap<>();
        this.lastKitByPlayer = new ConcurrentHashMap<>();
        this.editorSelectionByPlayer = new ConcurrentHashMap<>();
        instance = this;
    }

    public static BetterKitsPlugin get() {
        return instance;
    }

    public KitDefinition getLastKit(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        return lastKitByPlayer.get(playerId);
    }

    public void setLastKit(UUID playerId, KitDefinition kit) {
        if (playerId == null) {
            return;
        }
        if (kit == null) {
            lastKitByPlayer.remove(playerId);
        } else {
            lastKitByPlayer.put(playerId, kit);
        }
    }

    public String getEditorKitName(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        return editorSelectionByPlayer.get(playerId);
    }

    public void setEditorKitName(UUID playerId, String kitName) {
        if (playerId == null) {
            return;
        }
        if (kitName == null || kitName.isBlank()) {
            editorSelectionByPlayer.remove(playerId);
            return;
        }
        editorSelectionByPlayer.put(playerId, kitName);
    }

    public long getRemainingCooldownSeconds(UUID playerId, KitDefinition kit) {
        if (playerId == null || kit == null) {
            return 0;
        }
        long cooldownSeconds = resolveCooldownSeconds(kit);
        if (cooldownSeconds <= 0) {
            return 0;
        }
        String key = KitRepository.normalizeName(kit.getName());
        if (key.isEmpty()) {
            return 0;
        }
        Map<String, Long> playerCooldowns = kitCooldowns.get(playerId);
        if (playerCooldowns == null) {
            return 0;
        }
        Long lastUsed = playerCooldowns.get(key);
        if (lastUsed == null) {
            return 0;
        }
        long remainingMs = (cooldownSeconds * 1000L) - (System.currentTimeMillis() - lastUsed);
        if (remainingMs <= 0) {
            playerCooldowns.remove(key);
            if (playerCooldowns.isEmpty()) {
                kitCooldowns.remove(playerId);
            }
            saveCooldowns();
            return 0;
        }
        return (remainingMs + 999L) / 1000L;
    }

    public void markKitUsed(UUID playerId, KitDefinition kit) {
        if (playerId == null || kit == null) {
            return;
        }
        setLastKit(playerId, kit);
        long cooldownSeconds = resolveCooldownSeconds(kit);
        if (cooldownSeconds <= 0) {
            return;
        }
        String key = KitRepository.normalizeName(kit.getName());
        if (key.isEmpty()) {
            return;
        }
        kitCooldowns.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>())
            .put(key, System.currentTimeMillis());
        saveCooldowns();
    }

    public static String formatDuration(long seconds) {
        if (seconds <= 0) {
            return "0s";
        }
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long secs = seconds % 60L;
        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append("h");
        }
        if (minutes > 0) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(minutes).append("m");
        }
        if (secs > 0 || sb.isEmpty()) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(secs).append("s");
        }
        return sb.toString();
    }

    @Override
    protected void start() {
        repository = new KitRepository(getDataDirectory(), getLogger());
        repository.load();

        settingsRepository = new KitSettingsRepository(getDataDirectory());
        settingsRepository.load();
        settingsRepository.save();

        cooldownsFile = getDataDirectory().resolve("cooldowns.json");
        menuCommandCooldownsFile = getDataDirectory().resolve("menu_command_cooldowns.json");
        loadCooldowns();
        loadMenuCommandCooldowns();

        Path dataFolder = getDataDirectory().resolve("data");
        assetManager = new KitAssetManager(getFile(), dataFolder, getLogger());
        assetManager.packKitAssets();

        getCommandRegistry().registerCommand(new KitCommand(this));
        getCommandRegistry().registerCommand(new KitsMenuCommand(this));
    }

    @Override
    protected void shutdown() {
        saveCooldowns();
        saveMenuCommandCooldowns();
    }

    public long getRemainingMenuCommandCooldownSeconds(UUID playerId) {
        if (playerId == null) {
            return 0;
        }
        KitGlobalSettings global = settingsRepository != null
            ? settingsRepository.getGlobalSettings()
            : new KitGlobalSettings();
        long cooldownSeconds = global != null ? global.getMenuCommandCooldownSeconds() : 0L;
        if (cooldownSeconds <= 0) {
            return 0;
        }
        Long lastUsed = menuCommandCooldowns.get(playerId);
        if (lastUsed == null) {
            return 0;
        }
        long remainingMs = (cooldownSeconds * 1000L) - (System.currentTimeMillis() - lastUsed);
        if (remainingMs <= 0) {
            menuCommandCooldowns.remove(playerId);
            saveMenuCommandCooldowns();
            return 0;
        }
        return (remainingMs + 999L) / 1000L;
    }

    public void markMenuCommandUsed(UUID playerId) {
        if (playerId == null) {
            return;
        }
        KitGlobalSettings global = settingsRepository != null
            ? settingsRepository.getGlobalSettings()
            : new KitGlobalSettings();
        long cooldownSeconds = global != null ? global.getMenuCommandCooldownSeconds() : 0L;
        if (cooldownSeconds <= 0) {
            return;
        }
        menuCommandCooldowns.put(playerId, System.currentTimeMillis());
        saveMenuCommandCooldowns();
    }

    public KitSettings getSettingsForKit(KitDefinition kit) {
        if (kit == null || settingsRepository == null) {
            return new KitSettings();
        }
        KitSettings settings = settingsRepository.get(kit.getName());
        return settings != null ? settings : new KitSettings();
    }

    private long resolveCooldownSeconds(KitDefinition kit) {
        KitSettings settings = getSettingsForKit(kit);
        return Math.max(0L, settings.getCooldownSeconds());
    }

    public KitRepository getRepository() {
        return repository;
    }

    public KitSettingsRepository getSettingsRepository() {
        return settingsRepository;
    }

    public KitAssetManager getAssetManager() {
        return assetManager;
    }

    private void loadCooldowns() {
        kitCooldowns.clear();
        if (cooldownsFile == null || !Files.exists(cooldownsFile)) {
            return;
        }
        try {
            Object parsed = JsonUtil.readFile(cooldownsFile);
            Map<String, Object> raw = JsonUtil.asObjectMap(parsed);
            if (raw.isEmpty()) {
                return;
            }
            for (Map.Entry<String, Object> entry : raw.entrySet()) {
                String key = entry.getKey();
                Map<String, Object> values = JsonUtil.asObjectMap(entry.getValue());
                if (key == null || key.isBlank() || values == null) {
                    continue;
                }
                UUID uuid;
                try {
                    uuid = UUID.fromString(key);
                } catch (IllegalArgumentException ignored) {
                    continue;
                }
                Map<String, Long> parsedValues = new ConcurrentHashMap<>();
                for (Map.Entry<String, Object> kitEntry : values.entrySet()) {
                    String kitKey = kitEntry.getKey();
                    long timestamp = JsonUtil.asLong(kitEntry.getValue(), -1L);
                    if (kitKey == null || kitKey.isBlank() || timestamp <= 0) {
                        continue;
                    }
                    parsedValues.put(kitKey, timestamp);
                }
                if (!parsedValues.isEmpty()) {
                    kitCooldowns.put(uuid, parsedValues);
                }
            }
        } catch (IOException e) {
            getLogger().at(Level.SEVERE).withCause(e).log("Failed to load cooldowns.json");
        }
    }

    private void saveCooldowns() {
        if (cooldownsFile == null) {
            return;
        }
        try {
            Files.createDirectories(cooldownsFile.getParent(), new FileAttribute[0]);
            Map<String, Object> serialized = new TreeMap<>();
            for (Map.Entry<UUID, Map<String, Long>> entry : kitCooldowns.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                serialized.put(entry.getKey().toString(), new TreeMap<>(entry.getValue()));
            }
            JsonUtil.writeFile(cooldownsFile, serialized);
        } catch (IOException e) {
            getLogger().at(Level.SEVERE).withCause(e).log("Failed to save cooldowns.json");
        }
    }

    private void loadMenuCommandCooldowns() {
        menuCommandCooldowns.clear();
        if (menuCommandCooldownsFile == null || !Files.exists(menuCommandCooldownsFile)) {
            return;
        }
        try {
            Object parsed = JsonUtil.readFile(menuCommandCooldownsFile);
            Map<String, Object> raw = JsonUtil.asObjectMap(parsed);
            if (raw.isEmpty()) {
                return;
            }
            for (Map.Entry<String, Object> entry : raw.entrySet()) {
                String key = entry.getKey();
                long value = JsonUtil.asLong(entry.getValue(), -1L);
                if (key == null || key.isBlank() || value <= 0) {
                    continue;
                }
                try {
                    UUID uuid = UUID.fromString(key);
                    menuCommandCooldowns.put(uuid, value);
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (IOException e) {
            getLogger().at(Level.SEVERE).withCause(e).log("Failed to load menu_command_cooldowns.json");
        }
    }

    private void saveMenuCommandCooldowns() {
        if (menuCommandCooldownsFile == null) {
            return;
        }
        try {
            Files.createDirectories(menuCommandCooldownsFile.getParent(), new FileAttribute[0]);
            Map<String, Object> serialized = new TreeMap<>();
            for (Map.Entry<UUID, Long> entry : menuCommandCooldowns.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                serialized.put(entry.getKey().toString(), entry.getValue());
            }
            JsonUtil.writeFile(menuCommandCooldownsFile, serialized);
        } catch (IOException e) {
            getLogger().at(Level.SEVERE).withCause(e).log("Failed to save menu_command_cooldowns.json");
        }
    }
}

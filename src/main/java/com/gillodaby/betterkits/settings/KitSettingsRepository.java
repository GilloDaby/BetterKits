package com.gillodaby.betterkits.settings;

import com.gillodaby.betterkits.BetterKitsPlugin;
import com.gillodaby.betterkits.KitRepository;
import com.gillodaby.betterkits.util.JsonUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class KitSettingsRepository {

    private final Path settingsFile;
    private final Map<String, KitSettings> settings;
    private KitGlobalSettings globalSettings;

    public KitSettingsRepository(Path dataDir) {
        this.settings = new ConcurrentHashMap<>();
        this.globalSettings = new KitGlobalSettings();
        this.settingsFile = dataDir.resolve("settings.json");
    }

    public synchronized void load() {
        settings.clear();
        globalSettings = new KitGlobalSettings();
        if (settingsFile == null || !Files.exists(settingsFile)) {
            return;
        }
        try {
            Object parsed = JsonUtil.readFile(settingsFile);
            Map<String, Object> root = JsonUtil.asObjectMap(parsed);
            if (root.isEmpty()) {
                return;
            }
            if (root.containsKey("kits") || root.containsKey("global")) {
                Map<String, Object> globalMap = JsonUtil.asObjectMap(root.get("global"));
                if (!globalMap.isEmpty()) {
                    globalSettings = toGlobalSettings(globalMap);
                }
                Map<String, Object> kitsMap = JsonUtil.asObjectMap(root.get("kits"));
                kitsMap.forEach((name, value) -> acceptLoaded(name, toKitSettings(value)));
            } else {
                root.forEach((name, value) -> acceptLoaded(name, toKitSettings(value)));
            }
        } catch (IOException e) {
            BetterKitsPlugin.get().getLogger().at(Level.SEVERE).withCause(e).log("Failed to load settings.json");
        }
    }

    public synchronized void save() {
        if (settingsFile == null) {
            return;
        }
        try {
            Files.createDirectories(settingsFile.getParent(), new FileAttribute[0]);
            Map<String, Object> model = new TreeMap<>();
            model.put("global", toMap(globalSettings != null ? globalSettings : new KitGlobalSettings()));
            Map<String, Object> kitsMap = new TreeMap<>();
            for (Map.Entry<String, KitSettings> entry : settings.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                kitsMap.put(entry.getKey(), toMap(entry.getValue()));
            }
            model.put("kits", kitsMap);
            JsonUtil.writeFile(settingsFile, model);
        } catch (IOException e) {
            BetterKitsPlugin.get().getLogger().at(Level.SEVERE).withCause(e).log("Failed to save settings.json");
        }
    }

    public KitSettings get(String name) {
        return settings.get(KitRepository.normalizeName(name));
    }

    public KitSettings getOrDefault(String name) {
        KitSettings value = get(name);
        return value != null ? value : new KitSettings();
    }

    public synchronized void set(String name, KitSettings value) {
        String key = KitRepository.normalizeName(name);
        if (key.isEmpty() || value == null) {
            return;
        }
        settings.put(key, value);
        save();
    }

    public synchronized void putAll(Map<String, KitSettings> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        for (Map.Entry<String, KitSettings> entry : values.entrySet()) {
            String key = KitRepository.normalizeName(entry.getKey());
            if (key.isEmpty() || entry.getValue() == null) {
                continue;
            }
            settings.put(key, entry.getValue());
        }
        save();
    }

    public synchronized void remove(String name) {
        String key = KitRepository.normalizeName(name);
        if (key.isEmpty()) {
            return;
        }
        settings.remove(key);
        save();
    }

    public Map<String, KitSettings> listAll() {
        return Map.copyOf(settings);
    }

    public KitGlobalSettings getGlobalSettings() {
        return globalSettings != null ? globalSettings : new KitGlobalSettings();
    }

    public synchronized void setGlobalSettings(KitGlobalSettings globalSettings) {
        if (globalSettings == null) {
            return;
        }
        this.globalSettings = globalSettings;
        save();
    }

    private void acceptLoaded(String name, KitSettings value) {
        String key = KitRepository.normalizeName(name);
        if (key.isEmpty() || value == null) {
            return;
        }
        settings.put(key, value);
    }

    private static KitSettings toKitSettings(Object value) {
        Map<String, Object> map = JsonUtil.asObjectMap(value);
        KitSettings settings = new KitSettings();
        if (map.isEmpty()) {
            return settings;
        }
        settings.setCooldownSeconds(JsonUtil.asLong(map.get("cooldownSeconds"), settings.getCooldownSeconds()));
        settings.setAllowKitStacking(JsonUtil.asBoolean(map.get("allowKitStacking"), settings.isAllowKitStacking()));
        settings.setOverlapOtherKits(JsonUtil.asBoolean(map.get("overlapOtherKits"), settings.isOverlapOtherKits()));
        settings.setInvDeletion(JsonUtil.asBoolean(map.get("invDeletion"), settings.isInvDeletion()));
        settings.setAllowedWorlds(JsonUtil.asStringList(map.get("allowedWorlds")));
        return settings;
    }

    private static KitGlobalSettings toGlobalSettings(Map<String, Object> map) {
        KitGlobalSettings settings = new KitGlobalSettings();
        if (map == null || map.isEmpty()) {
            return settings;
        }
        settings.setMenuCommandCooldownSeconds(JsonUtil.asLong(map.get("menuCommandCooldownSeconds"),
            settings.getMenuCommandCooldownSeconds()));
        settings.setShowRandomKitButton(JsonUtil.asBoolean(map.get("showRandomKitButton"),
            settings.isShowRandomKitButton()));
        settings.setBackgroundDefaultOpacity(JsonUtil.asDouble(map.get("backgroundDefaultOpacity"),
            settings.getBackgroundDefaultOpacity()));
        return settings;
    }

    private static Map<String, Object> toMap(KitSettings settings) {
        Map<String, Object> map = new TreeMap<>();
        map.put("cooldownSeconds", settings.getCooldownSeconds());
        map.put("allowKitStacking", settings.isAllowKitStacking());
        map.put("overlapOtherKits", settings.isOverlapOtherKits());
        map.put("invDeletion", settings.isInvDeletion());
        map.put("allowedWorlds", settings.getAllowedWorlds() != null ? settings.getAllowedWorlds() : List.of());
        return map;
    }

    private static Map<String, Object> toMap(KitGlobalSettings settings) {
        Map<String, Object> map = new TreeMap<>();
        map.put("menuCommandCooldownSeconds", settings.getMenuCommandCooldownSeconds());
        map.put("showRandomKitButton", settings.isShowRandomKitButton());
        map.put("backgroundDefaultOpacity", settings.getBackgroundDefaultOpacity());
        return map;
    }
}

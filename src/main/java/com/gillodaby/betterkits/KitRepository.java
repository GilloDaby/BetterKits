package com.gillodaby.betterkits;

import com.gillodaby.betterkits.models.KitDefinition;
import com.gillodaby.betterkits.models.KitItem;
import com.gillodaby.betterkits.util.JsonUtil;
import com.hypixel.hytale.logger.HytaleLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class KitRepository {

    private final Path dataFile;
    private final HytaleLogger logger;
    private final Map<String, KitDefinition> kits;

    public KitRepository(Path dataDir, HytaleLogger logger) {
        this.kits = new ConcurrentHashMap<>();
        this.dataFile = dataDir.resolve("kits.json");
        this.logger = logger;
    }

    public synchronized void load() {
        kits.clear();
        if (!Files.exists(dataFile)) {
            return;
        }
        try {
            Object parsed = JsonUtil.readFile(dataFile);
            Map<String, Object> raw = JsonUtil.asObjectMap(parsed);
            if (raw.isEmpty()) {
                return;
            }
            for (Map.Entry<String, Object> entry : raw.entrySet()) {
                String key = entry.getKey();
                KitDefinition kit = toKitDefinition(entry.getValue(), key);
                acceptLoaded(key, kit);
            }
        } catch (IOException e) {
            logger.at(Level.SEVERE).withCause(e).log("Failed to load kits.json");
        }
    }

    public synchronized void save() {
        try {
            Files.createDirectories(dataFile.getParent());
            Path tempFile = dataFile.resolveSibling(dataFile.getFileName().toString() + ".tmp");
            Map<String, Object> output = new TreeMap<>();
            for (Map.Entry<String, KitDefinition> entry : kits.entrySet()) {
                KitDefinition kit = entry.getValue();
                if (kit == null) {
                    continue;
                }
                output.put(entry.getKey(), toSerializable(kit));
            }
            JsonUtil.writeFile(tempFile, output);
            try {
                Files.move(tempFile, dataFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                Files.move(tempFile, dataFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            logger.at(Level.SEVERE).withCause(e).log("Failed to save kits.json");
        }
    }

    public KitDefinition getKit(String name) {
        return kits.get(normalizeName(name));
    }

    public Collection<KitDefinition> list() {
        return kits.values();
    }

    public synchronized boolean setKit(KitDefinition kit) {
        String key = normalizeName(kit != null ? kit.getName() : null);
        if (key.isEmpty()) {
            return false;
        }
        kits.put(key, sanitize(kit, key));
        save();
        return true;
    }

    public synchronized boolean removeKit(String name) {
        String key = normalizeName(name);
        if (key.isEmpty()) {
            return false;
        }
        KitDefinition removed = kits.remove(key);
        if (removed == null) {
            return false;
        }
        save();
        return true;
    }

    public static String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        return name.trim().toLowerCase();
    }

    private void acceptLoaded(String name, KitDefinition kit) {
        String key = normalizeName(name);
        if (key.isEmpty()) {
            return;
        }
        kits.put(key, sanitize(kit, key));
    }

    private KitDefinition sanitize(KitDefinition kit, String key) {
        if (kit == null) {
            return new KitDefinition(key, key, 0L, new ArrayList<>(), new ArrayList<>());
        }
        if (kit.getName() == null || kit.getName().isBlank()) {
            kit.setName(key);
        }
        if (kit.getDisplayName() == null || kit.getDisplayName().isBlank()) {
            kit.setDisplayName(kit.getName());
        } else {
            String cleaned = stripAmpersandFormatting(kit.getDisplayName()).trim();
            kit.setDisplayName(cleaned.isBlank() ? kit.getName() : cleaned);
        }
        if (kit.getCooldownSeconds() < 0) {
            kit.setCooldownSeconds(0L);
        }
        if (kit.getItems() == null) {
            kit.setItems(new ArrayList<>());
        }
        if (kit.getArmor() == null) {
            kit.setArmor(new ArrayList<>());
        }
        return kit;
    }

    private Map<String, Object> toSerializable(KitDefinition kit) {
        Map<String, Object> map = new TreeMap<>();
        map.put("name", kit.getName());
        map.put("displayName", kit.getDisplayName());
        map.put("items", toItemList(kit.getItems()));
        map.put("armor", toItemList(kit.getArmor()));
        return map;
    }

    private List<Map<String, Object>> toItemList(List<KitItem> items) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (items == null) {
            return result;
        }
        for (KitItem item : items) {
            if (item == null) {
                continue;
            }
            Map<String, Object> entry = new TreeMap<>();
            entry.put("id", item.getId());
            entry.put("quantity", item.getQuantity());
            result.add(entry);
        }
        return result;
    }

    private KitDefinition toKitDefinition(Object value, String key) {
        Map<String, Object> map = JsonUtil.asObjectMap(value);
        if (map.isEmpty()) {
            return new KitDefinition(key, key, 0L, new ArrayList<>(), new ArrayList<>());
        }
        String name = JsonUtil.asString(map.get("name"), key);
        String displayName = JsonUtil.asString(map.get("displayName"), name);
        long cooldownSeconds = JsonUtil.asLong(map.get("cooldownSeconds"), 0L);
        boolean allowKitStacking = JsonUtil.asBoolean(map.get("allowKitStacking"), true);
        List<KitItem> items = toItems(JsonUtil.asList(map.get("items")));
        List<KitItem> armor = toItems(JsonUtil.asList(map.get("armor")));
        return new KitDefinition(name, displayName, cooldownSeconds, items, armor, allowKitStacking);
    }

    private List<KitItem> toItems(List<Object> rawList) {
        List<KitItem> items = new ArrayList<>();
        if (rawList == null) {
            return items;
        }
        for (Object entry : rawList) {
            Map<String, Object> map = JsonUtil.asObjectMap(entry);
            if (map.isEmpty()) {
                continue;
            }
            String id = JsonUtil.asString(map.get("id"), "");
            int quantity = JsonUtil.asInt(map.get("quantity"), 1);
            if (id.isBlank()) {
                continue;
            }
            items.add(new KitItem(id, Math.max(1, quantity)));
        }
        return items;
    }

    private static String stripAmpersandFormatting(String raw) {
        if (raw == null || raw.isEmpty()) {
            return raw;
        }
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (ch == '&' && i + 1 < raw.length() && isFormattingCode(raw.charAt(i + 1))) {
                i++;
                continue;
            }
            sb.append(ch);
        }
        return sb.toString();
    }

    private static boolean isFormattingCode(char ch) {
        char lower = Character.toLowerCase(ch);
        if (lower >= '0' && lower <= '9') {
            return true;
        }
        if (lower >= 'a' && lower <= 'f') {
            return true;
        }
        return lower == 'k' || lower == 'l' || lower == 'm' || lower == 'n'
            || lower == 'o' || lower == 'r';
    }
}

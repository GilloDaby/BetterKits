package com.gillodaby.betterkits.pages;

import com.gillodaby.betterkits.BetterKitsPlugin;
import com.gillodaby.betterkits.KitRepository;
import com.gillodaby.betterkits.models.KitDefinition;
import com.gillodaby.betterkits.pages.KitEditorPage;
import com.gillodaby.betterkits.pages.KitPreviewPage;
import com.gillodaby.betterkits.settings.KitSettings;
import com.gillodaby.betterkits.util.KitUtils;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec.Builder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.ui.PatchStyle;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class KitMenuPage extends InteractiveCustomUIPage<KitMenuPage.KitMenuEventData> {

    private static final Random RANDOM = new Random();

    private final List<KitDefinition> allKits;
    private List<KitDefinition> displayedKits;
    private String currentSearchQuery;
    private final boolean editorMode;

    public KitMenuPage(PlayerRef playerRef, List<KitDefinition> kits) {
        this(playerRef, kits, false);
    }

    public KitMenuPage(PlayerRef playerRef, List<KitDefinition> kits, boolean editorMode) {
        super(playerRef, CustomPageLifetime.CanDismiss, KitMenuEventData.CODEC);
        this.currentSearchQuery = "";
        this.editorMode = editorMode;
        List<KitDefinition> resolved = new ArrayList<>();
        if (kits != null) {
            resolved.addAll(kits);
        }
        resolved.sort(Comparator.comparing(KitUtils::safeKitDisplayName, String.CASE_INSENSITIVE_ORDER));
        this.allKits = resolved;
        this.displayedKits = resolved;
    }

    @Override
    public void build(Ref<EntityStore> ref,
                      UICommandBuilder cmd,
                      UIEventBuilder events,
                      Store<EntityStore> store) {
        cmd.append("Pages/KitsPage.ui");
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            showError(cmd, "ERROR: PLAYER NOT FOUND");
            return;
        }

        BetterKitsPlugin plugin = BetterKitsPlugin.get();
        boolean showRandom = !editorMode && plugin.getSettingsRepository().getGlobalSettings().isShowRandomKitButton();
        cmd.set("#RandomKitButton.Visible", showRandom);

        events.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#SearchInput",
            EventData.of("@SearchQuery", "#SearchInput.Value"),
            false
        );
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#RandomKitButton",
            EventData.of("Action", "Random")
        );

        buildKitList(ref, cmd, events, store);
    }

    private void buildKitList(Ref<EntityStore> ref,
                              UICommandBuilder cmd,
                              UIEventBuilder events,
                              Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        String worldName = player.getWorld().getName();
        List<KitDefinition> worldKits = filterByWorld(worldName);
        List<KitDefinition> filtered = filterBySearch(worldKits);
        if (!editorMode) {
            List<KitDefinition> permitted = new ArrayList<>();
            for (KitDefinition kit : filtered) {
                if (hasKitPermission(KitUtils.safeKitName(kit))) {
                    permitted.add(kit);
                }
            }
            filtered = permitted;
        }

        int available = countAvailableKits(filtered);
        cmd.set("#KitCount.Text", available + " / " + filtered.size() + " KITS AVAILABLE");
        cmd.clear("#KitCards");

        if (filtered.isEmpty()) {
            String message = hasSearchQuery() ? "NO KITS FOUND" : "NO KITS AVAILABLE IN THIS WORLD";
            showEmptyMessage(cmd, message);
            displayedKits = filtered;
            return;
        }

        int index = 0;
        for (KitDefinition kit : filtered) {
            String kitName = KitUtils.safeKitName(kit);
            if (kitName.isBlank()) {
                continue;
            }
            buildKitCard(cmd, events, kit, index);
            index++;
        }
        displayedKits = filtered;
    }

    private void buildKitCard(UICommandBuilder cmd, UIEventBuilder events, KitDefinition kit, int index) {
        String kitName = KitUtils.safeKitName(kit);
        String displayName = KitUtils.safeKitDisplayName(kit);
        String selector = "#KitCards[" + index + "]";
        boolean hasPermission = editorMode || hasKitPermission(kitName);
        long cooldown = (!editorMode && hasPermission)
            ? BetterKitsPlugin.get().getRemainingCooldownSeconds(playerRef.getUuid(), kit)
            : 0L;
        boolean available = editorMode || (hasPermission && cooldown <= 0);

        String statusText;
        String statusColor;
        if (editorMode) {
            statusText = "Edit";
            statusColor = "#6b9bc7";
        } else if (!hasPermission) {
            statusText = "Locked";
            statusColor = "#c76b6b";
        } else if (cooldown > 0) {
            statusText = "Cooldown: " + BetterKitsPlugin.formatDuration(cooldown);
            statusColor = "#d4a76b";
        } else {
            statusText = "Available";
            statusColor = "#6bc77a";
        }

        cmd.append("#KitCards", "Pages/KitsEntry.ui");
        cmd.set(selector + " #CardButton #Name.Text", displayName);
        cmd.set(selector + " #CardButton #Status.Text", statusText);
        cmd.set(selector + " #CardButton #Status.Style.TextColor", statusColor);
        applyKitAssets(cmd, selector + " #CardButton", kitName);

        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            selector + " #CardButton",
            EventData.of("Index", String.valueOf(index)).append("Action", editorMode ? "Edit" : "Preview")
        );
    }

    private void applyKitAssets(UICommandBuilder cmd, String selector, String kitName) {
        var assetManager = BetterKitsPlugin.get().getAssetManager();
        if (assetManager == null) {
            cmd.set(selector + " #Icon.Visible", false);
            return;
        }

        String iconPath = assetManager.getIconPath(kitName);
        if (iconPath != null) {
            PatchStyle iconStyle = new PatchStyle(Value.of(iconPath));
            cmd.setObject(selector + " #Icon.Background", iconStyle);
            cmd.set(selector + " #Icon.Visible", true);
        } else {
            cmd.set(selector + " #Icon.Visible", false);
        }

        String backgroundPath = assetManager.getBackgroundPath(kitName);
        if (backgroundPath != null) {
            PatchStyle base = new PatchStyle(Value.of(backgroundPath));
            PatchStyle hovered = new PatchStyle(Value.of(backgroundPath)).setColor(Value.of("#00000040"));
            PatchStyle pressed = new PatchStyle(Value.of(backgroundPath)).setColor(Value.of("#00000066"));

            cmd.setObject(selector + ".Style.Default.Background", base);
            cmd.setObject(selector + ".Style.Hovered.Background", hovered);
            cmd.setObject(selector + ".Style.Pressed.Background", pressed);
            cmd.setObject(selector + " #Background.Background", base);
            cmd.set(selector + " #Background.Visible", true);
        } else {
            PatchStyle base = new PatchStyle().setColor(Value.of("#161e2b"));
            PatchStyle hovered = new PatchStyle().setColor(Value.of("#1e2938"));
            PatchStyle pressed = new PatchStyle().setColor(Value.of("#131a25"));

            cmd.setObject(selector + ".Style.Default.Background", base);
            cmd.setObject(selector + ".Style.Hovered.Background", hovered);
            cmd.setObject(selector + ".Style.Pressed.Background", pressed);
            cmd.setObject(selector + " #Background.Background", base);
            cmd.set(selector + " #Background.Visible", false);
        }
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, KitMenuEventData data) {
        if (data.searchQuery != null) {
            handleSearch(ref, store, data.searchQuery);
            return;
        }
        if ("Random".equals(data.action)) {
            giveRandomKit(ref, store);
            return;
        }
        if ("Edit".equals(data.action)) {
            handleKitSelection(ref, store, data);
            return;
        }
        if ("Claim".equals(data.action)) {
            handleClaim(ref, store, data);
            return;
        }
        if ("Preview".equals(data.action)) {
            handlePreview(ref, store, data);
            return;
        }
        handleKitSelection(ref, store, data);
    }

    private void handleSearch(Ref<EntityStore> ref, Store<EntityStore> store, String query) {
        currentSearchQuery = query.trim().toLowerCase();
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();

        events.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#SearchInput",
            EventData.of("@SearchQuery", "#SearchInput.Value"),
            false
        );
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#RandomKitButton",
            EventData.of("Action", "Random")
        );

        buildKitList(ref, cmd, events, store);
        sendUpdate(cmd, events, false);
    }

    private void handleKitSelection(Ref<EntityStore> ref, Store<EntityStore> store, KitMenuEventData data) {
        int index = parseIndex(data);
        if (index < 0 || index >= displayedKits.size()) {
            playerRef.sendMessage(Message.raw("Kit not found."));
            close();
            return;
        }
        KitDefinition kit = displayedKits.get(index);
        if (kit == null) {
            playerRef.sendMessage(Message.raw("Kit not found."));
            close();
            return;
        }
        if (editorMode) {
            String kitName = KitUtils.safeKitName(kit);
            BetterKitsPlugin.get().setEditorKitName(playerRef.getUuid(), kitName);
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null || player.getPageManager() == null) {
                playerRef.sendMessage(Message.raw("Player not available."));
                close();
                return;
            }
            player.getPageManager().openCustomPage(ref, store, new KitEditorPage(playerRef, kitName, BetterKitsPlugin.get().getRepository()));
            return;
        }
        giveKit(ref, store, kit);
    }

    private void handlePreview(Ref<EntityStore> ref, Store<EntityStore> store, KitMenuEventData data) {
        int index = parseIndex(data);
        if (index < 0 || index >= displayedKits.size()) {
            playerRef.sendMessage(Message.raw("Kit not found."));
            return;
        }
        KitDefinition kit = displayedKits.get(index);
        if (kit == null) {
            playerRef.sendMessage(Message.raw("Kit not found."));
            return;
        }
        if (editorMode) {
            handleKitSelection(ref, store, data);
            return;
        }
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null || player.getPageManager() == null) {
            playerRef.sendMessage(Message.raw("Player not available."));
            return;
        }
        player.getPageManager().openCustomPage(ref, store,
            new KitPreviewPage(playerRef, kit, BetterKitsPlugin.get().getRepository()));
    }

    private void handleClaim(Ref<EntityStore> ref, Store<EntityStore> store, KitMenuEventData data) {
        int index = parseIndex(data);
        if (index < 0 || index >= displayedKits.size()) {
            playerRef.sendMessage(Message.raw("Kit not found."));
            return;
        }
        KitDefinition kit = displayedKits.get(index);
        if (kit == null) {
            playerRef.sendMessage(Message.raw("Kit not found."));
            return;
        }
        giveKit(ref, store, kit);
    }

    private void giveRandomKit(Ref<EntityStore> ref, Store<EntityStore> store) {
        List<KitDefinition> available = new ArrayList<>();
        for (KitDefinition kit : displayedKits) {
            String kitName = KitUtils.safeKitName(kit);
            if (kitName.isBlank()) {
                continue;
            }
            if (!hasKitPermission(kitName)) {
                continue;
            }
            long remaining = BetterKitsPlugin.get().getRemainingCooldownSeconds(playerRef.getUuid(), kit);
            if (remaining <= 0) {
                available.add(kit);
            }
        }

        if (available.isEmpty()) {
            playerRef.sendMessage(Message.raw("No available kits to select randomly."));
            close();
            return;
        }

        KitDefinition kit = available.get(RANDOM.nextInt(available.size()));
        giveKit(ref, store, kit);
    }

    private void giveKit(Ref<EntityStore> ref, Store<EntityStore> store, KitDefinition kit) {
        String kitName = KitUtils.safeKitName(kit);
        String displayName = KitUtils.safeKitDisplayName(kit);
        if (kitName.isBlank()) {
            playerRef.sendMessage(Message.raw("Kit not found."));
            close();
            return;
        }
        if (!hasKitPermission(kitName)) {
            playerRef.sendMessage(Message.raw("You don't have permission to use the '" + displayName + "' kit."));
            close();
            return;
        }
        long remaining = BetterKitsPlugin.get().getRemainingCooldownSeconds(playerRef.getUuid(), kit);
        if (remaining > 0) {
            playerRef.sendMessage(Message.raw(
                "Kit '" + displayName + "' is available in " + BetterKitsPlugin.formatDuration(remaining) + "."
            ));
            close();
            return;
        }
        if (!KitUtils.giveKitToPlayer(store, ref, kit)) {
            close();
            return;
        }
        BetterKitsPlugin.get().markKitUsed(playerRef.getUuid(), kit);
        playerRef.sendMessage(Message.raw("Kit '" + displayName + "' received successfully!"));
        close();
    }

    private List<KitDefinition> filterByWorld(String worldName) {
        if (editorMode) {
            return new ArrayList<>(allKits);
        }
        List<KitDefinition> resolved = new ArrayList<>();
        for (KitDefinition kit : allKits) {
            KitSettings settings = BetterKitsPlugin.get().getSettingsForKit(kit);
            if (isWorldAllowed(settings, worldName)) {
                resolved.add(kit);
            }
        }
        return resolved;
    }

    private List<KitDefinition> filterBySearch(List<KitDefinition> kits) {
        if (!hasSearchQuery()) {
            return new ArrayList<>(kits);
        }
        List<KitDefinition> resolved = new ArrayList<>();
        for (KitDefinition kit : kits) {
            String display = KitUtils.safeKitDisplayName(kit).toLowerCase();
            String name = KitUtils.safeKitName(kit).toLowerCase();
            if (display.contains(currentSearchQuery) || name.contains(currentSearchQuery)) {
                resolved.add(kit);
            }
        }
        return resolved;
    }

    private int countAvailableKits(List<KitDefinition> kits) {
        if (editorMode) {
            return kits.size();
        }
        int count = 0;
        for (KitDefinition kit : kits) {
            if (hasKitPermission(KitUtils.safeKitName(kit))) {
                count++;
            }
        }
        return count;
    }

    private boolean hasSearchQuery() {
        return currentSearchQuery != null && !currentSearchQuery.trim().isEmpty();
    }

    private boolean hasKitPermission(String kitName) {
        String key = KitRepository.normalizeName(kitName);
        if (PermissionsModule.get().hasPermission(playerRef.getUuid(), com.gillodaby.betterkits.settings.KitPermissions.KITS_MANAGE)
            || PermissionsModule.get().hasPermission(playerRef.getUuid(), com.gillodaby.betterkits.settings.KitPermissions.ADMIN)) {
            return true;
        }
        if (PermissionsModule.get().hasPermission(playerRef.getUuid(), com.gillodaby.betterkits.settings.KitPermissions.KITS_GET_ANY)
            || PermissionsModule.get().hasPermission(playerRef.getUuid(), com.gillodaby.betterkits.settings.KitPermissions.KITS_GET)) {
            return true;
        }
        return PermissionsModule.get().hasPermission(playerRef.getUuid(), com.gillodaby.betterkits.settings.KitPermissions.KITS_GET_PREFIX + key);
    }

    private boolean isWorldAllowed(KitSettings settings, String worldName) {
        if (settings == null || settings.getAllowedWorlds() == null || settings.getAllowedWorlds().isEmpty()) {
            return true;
        }
        String world = worldName.toLowerCase();
        for (String allowed : settings.getAllowedWorlds()) {
            if ("all".equalsIgnoreCase(allowed) || allowed.equalsIgnoreCase(world)) {
                return true;
            }
        }
        return false;
    }

    private static int parseIndex(KitMenuEventData data) {
        if (data == null || data.index == null || data.index.isBlank()) {
            return -1;
        }
        try {
            return Integer.parseInt(data.index);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private void showError(UICommandBuilder cmd, String message) {
        String ui = "Label #EmptyLabel { Text: \"" + escapeInline(message)
            + "\"; Style: (FontSize: 16, TextColor: #c76b6b, HorizontalAlignment: Center, RenderUppercase: true);"
            + " Anchor: (Top: 50); }";
        cmd.appendInline("#KitCards", ui);
    }

    private void showEmptyMessage(UICommandBuilder cmd, String message) {
        String ui = "Label #EmptyLabel { Text: \"" + escapeInline(message)
            + "\"; Style: (FontSize: 14, TextColor: #7a8fa8, HorizontalAlignment: Center, RenderUppercase: true);"
            + " Anchor: (Top: 50); }";
        cmd.appendInline("#KitCards", ui);
    }

    private String escapeInline(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("\"", "\\\"");
    }

    public static final class KitMenuEventData {
        public static final BuilderCodec<KitMenuEventData> CODEC = BuilderCodec.builder(
            KitMenuEventData.class,
            KitMenuEventData::new
        )
            .append(new KeyedCodec<>("Index", Codec.STRING),
                (data, value) -> data.index = value,
                data -> data.index).add()
            .append(new KeyedCodec<>("Action", Codec.STRING),
                (data, value) -> data.action = value,
                data -> data.action).add()
            .append(new KeyedCodec<>("@SearchQuery", Codec.STRING),
                (data, value) -> data.searchQuery = value,
                data -> data.searchQuery).add()
            .build();

        public String index;
        public String action;
        public String searchQuery;

        public KitMenuEventData() {
        }
    }
}

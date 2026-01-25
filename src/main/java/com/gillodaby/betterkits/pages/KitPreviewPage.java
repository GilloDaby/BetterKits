package com.gillodaby.betterkits.pages;

import com.gillodaby.betterkits.BetterKitsPlugin;
import com.gillodaby.betterkits.KitRepository;
import com.gillodaby.betterkits.models.KitDefinition;
import com.gillodaby.betterkits.models.KitItem;
import com.gillodaby.betterkits.util.KitUtils;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.List;

public class KitPreviewPage extends InteractiveCustomUIPage<KitPreviewPage.KitPreviewEventData> {

    private final KitDefinition kit;
    private final KitRepository repository;

    public KitPreviewPage(PlayerRef playerRef, KitDefinition kit, KitRepository repository) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, KitPreviewEventData.CODEC);
        this.kit = kit;
        this.repository = repository;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd, UIEventBuilder events, Store<EntityStore> store) {
        cmd.append("Pages/BetterKitsPreview.ui");
        String displayName = KitUtils.safeKitDisplayName(kit);
        cmd.set("#KitName.Text", "Kit: " + displayName);

        boolean available = isAvailable();
        long remaining = BetterKitsPlugin.get().getRemainingCooldownSeconds(playerRef.getUuid(), kit);
        if (available) {
            cmd.set("#StatusLine.Text", "Available");
        } else if (remaining > 0) {
            cmd.set("#StatusLine.Text", "Cooldown: " + BetterKitsPlugin.formatDuration(remaining));
        } else {
            cmd.set("#StatusLine.Text", "Unavailable");
        }
        cmd.set("#ClaimLabel.Text", available ? "CLAIM" : "UNAVAILABLE");
        cmd.set("#ClaimLabel.Style.TextColor", available ? "#b4c4d3" : "#c76b6b");

        EventData claim = EventData.of("Action", "claim");
        EventData close = EventData.of("Action", "close");
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ClaimButton", claim, false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", close, false);

        buildItems(cmd);
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, KitPreviewEventData data) {
        if (data == null || data.action == null) {
            return;
        }
        switch (data.action) {
            case "close" -> close();
            case "claim" -> handleClaim(ref, store);
            default -> {
            }
        }
    }

    private void handleClaim(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (!isAvailable()) {
            playerRef.sendMessage(Message.raw("Kit not available."));
            return;
        }
        if (!KitUtils.giveKitToPlayer(store, ref, kit)) {
            playerRef.sendMessage(Message.raw("Failed to give kit."));
            return;
        }
        BetterKitsPlugin.get().markKitUsed(playerRef.getUuid(), kit);
        playerRef.sendMessage(Message.raw("Kit '" + KitUtils.safeKitDisplayName(kit) + "' received successfully!"));
        close();
    }

    private void buildItems(UICommandBuilder cmd) {
        cmd.clear("#ItemsList");
        List<KitItem> items = kit != null ? kit.getItems() : new ArrayList<>();
        if (items == null || items.isEmpty()) {
            cmd.appendInline("#ItemsList", "Label { Text: \"No items in kit.\"; Style: (FontSize: 12, TextColor: #7a90a8); Anchor: (Height: 20); }");
            return;
        }
        int index = 0;
        for (KitItem item : items) {
            if (item == null) {
                continue;
            }
            cmd.append("#ItemsList", "Pages/BetterKitsPreviewItem.ui");
            String selector = "#ItemsList[" + index + "]";
            if (item.getId() == null || item.getId().isBlank()) {
                cmd.setNull(selector + " #Icon.ItemId");
                cmd.set(selector + " #Icon.Visible", false);
                cmd.set(selector + " #ItemId.Text", "");
            } else {
                cmd.set(selector + " #Icon.ItemId", item.getId());
                cmd.set(selector + " #Icon.Visible", true);
                cmd.set(selector + " #ItemId.Text", item.getId());
            }
             cmd.set(selector + " #ItemQty.Text", "x" + Math.max(1, item.getQuantity()));
             index++;
         }
     }

    private boolean isAvailable() {
        if (kit == null) {
            return false;
        }
        String kitName = KitUtils.safeKitName(kit);
        if (kitName.isBlank()) {
            return false;
        }
        if (!hasKitPermission(kitName)) {
            return false;
        }
        long remaining = BetterKitsPlugin.get().getRemainingCooldownSeconds(playerRef.getUuid(), kit);
        return remaining <= 0;
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

    public static final class KitPreviewEventData {
        public static final BuilderCodec<KitPreviewEventData> CODEC = BuilderCodec.builder(
            KitPreviewEventData.class,
            KitPreviewEventData::new
        )
            .append(new KeyedCodec<>("Action", Codec.STRING),
                (data, value) -> data.action = value,
                data -> data.action).add()
            .build();

        public String action;

        public KitPreviewEventData() {
        }
    }
}

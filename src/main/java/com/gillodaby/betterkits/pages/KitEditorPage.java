package com.gillodaby.betterkits.pages;

import com.gillodaby.betterkits.BetterKitsPlugin;
import com.gillodaby.betterkits.KitRepository;
import com.gillodaby.betterkits.models.KitDefinition;
import com.gillodaby.betterkits.models.KitItem;
import com.gillodaby.betterkits.settings.KitSettings;
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
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.List;

public class KitEditorPage extends InteractiveCustomUIPage<KitEditorPage.KitEditorEventData> {

    private final String kitName;
    private final KitRepository repository;

    public KitEditorPage(PlayerRef playerRef, String kitName, KitRepository repository) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, KitEditorEventData.CODEC);
        this.kitName = kitName != null ? kitName : "";
        this.repository = repository;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd, UIEventBuilder events, Store<EntityStore> store) {
        cmd.append("Pages/BetterKitsEditor.ui");
        cmd.set("#KitName.Text", "Selected: " + kitName);

        KitSettings settings = BetterKitsPlugin.get().getSettingsRepository().getOrDefault(kitName);
        cmd.set("#CooldownInput.Value", String.valueOf(settings.getCooldownSeconds()));
        cmd.set("#WorldsInput.Value", String.join(", ", settings.getAllowedWorlds() != null
            ? settings.getAllowedWorlds() : List.of("all")));
        updateFlagLabels(cmd, settings);

        EventData addHand = EventData.of("Action", "addHand");
        EventData addId = new EventData().append("Action", "addId")
            .append("@ItemId", "#AddItemIdInput.Value")
            .append("@ItemQty", "#AddQtyInput.Value");
        EventData cooldown = new EventData().append("Action", "cooldown")
            .append("@Cooldown", "#CooldownInput.Value");
        EventData worlds = new EventData().append("Action", "worlds")
            .append("@Worlds", "#WorldsInput.Value");
        EventData flagStacking = EventData.of("Action", "flag").append("@Flag", "stacking");
        EventData flagOverlap = EventData.of("Action", "flag").append("@Flag", "overlap");
        EventData flagInv = EventData.of("Action", "flag").append("@Flag", "invDeletion");
        EventData close = EventData.of("Action", "close");

        events.addEventBinding(CustomUIEventBindingType.Activating, "#AddFromHandButton", addHand, false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#AddButton", addId, false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CooldownButton", cooldown, false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#WorldsButton", worlds, false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#FlagStackingButton", flagStacking, false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#FlagOverlapButton", flagOverlap, false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#FlagInvDelButton", flagInv, false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", close, false);

        buildItems(cmd, events);
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, KitEditorEventData data) {
        if (data == null || data.action == null) {
            return;
        }
        switch (data.action) {
            case "close" -> close();
            case "addHand" -> handleAddFromHand(ref, store);
            case "addId" -> handleAddById(data);
            case "cooldown" -> handleCooldownUpdate(data);
            case "worlds" -> handleWorldsUpdate(data);
            case "flag" -> handleFlagToggle(data);
            case "remove" -> handleRemoveIndex(data);
            default -> {
            }
        }
    }

    private void handleAddFromHand(Ref<EntityStore> ref, Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null || player.getInventory() == null) {
            playerRef.sendMessage(Message.raw("Player not available."));
            return;
        }
        ItemStack stack = player.getInventory().getActiveHotbarItem();
        if (stack == null || ItemStack.isEmpty(stack)) {
            playerRef.sendMessage(Message.raw("Hold an item in your hotbar."));
            return;
        }
        String itemId = stack.getItemId();
        int qty = stack.getQuantity();
        if (itemId == null || itemId.isBlank() || qty <= 0) {
            playerRef.sendMessage(Message.raw("Invalid held item."));
            return;
        }
        KitDefinition kit = getKit();
        if (kit == null) {
            playerRef.sendMessage(Message.raw("Kit not found."));
            close();
            return;
        }
        addOrMergeItem(kit, itemId, qty);
        repository.setKit(kit);
        refresh();
    }

    private void handleAddById(KitEditorEventData data) {
        String itemId = data.itemId != null ? data.itemId.trim() : "";
        if (itemId.isEmpty()) {
            playerRef.sendMessage(Message.raw("ItemId cannot be empty."));
            return;
        }
        int qty = parseQuantity(data.itemQty);
        if (qty <= 0) {
            playerRef.sendMessage(Message.raw("Quantity must be >= 1."));
            return;
        }
        KitDefinition kit = getKit();
        if (kit == null) {
            playerRef.sendMessage(Message.raw("Kit not found."));
            close();
            return;
        }
        addOrMergeItem(kit, itemId, qty);
        repository.setKit(kit);
        refresh();
    }

    private void handleRemoveIndex(KitEditorEventData data) {
        int index = parseIndex(data);
        if (index < 0) {
            return;
        }
        KitDefinition kit = getKit();
        if (kit == null) {
            playerRef.sendMessage(Message.raw("Kit not found."));
            close();
            return;
        }
        List<KitItem> items = kit.getItems() != null ? new ArrayList<>(kit.getItems()) : new ArrayList<>();
        if (index >= items.size()) {
            return;
        }
        items.remove(index);
        kit.setItems(items);
        repository.setKit(kit);
        refresh();
    }

    private void handleCooldownUpdate(KitEditorEventData data) {
        long cooldown = parseLong(data.cooldown);
        if (cooldown < 0) {
            playerRef.sendMessage(Message.raw("Cooldown must be >= 0 seconds."));
            return;
        }
        KitSettings settings = BetterKitsPlugin.get().getSettingsRepository().getOrDefault(kitName);
        settings.setCooldownSeconds(cooldown);
        BetterKitsPlugin.get().getSettingsRepository().set(kitName, settings);
        playerRef.sendMessage(Message.raw("Cooldown updated to " + cooldown + "s."));
        refresh();
    }

    private void handleWorldsUpdate(KitEditorEventData data) {
        String raw = data.worlds != null ? data.worlds.trim() : "";
        List<String> worlds = new ArrayList<>();
        if (!raw.isBlank()) {
            for (String part : raw.split(",")) {
                String world = part.trim();
                if (!world.isBlank()) {
                    worlds.add(world.toLowerCase());
                }
            }
        }
        if (worlds.isEmpty() || worlds.stream().anyMatch(w -> w.equalsIgnoreCase("all"))) {
            worlds = List.of("all");
        }
        KitSettings settings = BetterKitsPlugin.get().getSettingsRepository().getOrDefault(kitName);
        settings.setAllowedWorlds(worlds);
        BetterKitsPlugin.get().getSettingsRepository().set(kitName, settings);
        refresh();
    }

    private void handleFlagToggle(KitEditorEventData data) {
        if (data.flag == null || data.flag.isBlank()) {
            return;
        }
        KitSettings settings = BetterKitsPlugin.get().getSettingsRepository().getOrDefault(kitName);
        switch (data.flag) {
            case "stacking" -> settings.setAllowKitStacking(!settings.isAllowKitStacking());
            case "overlap" -> settings.setOverlapOtherKits(!settings.isOverlapOtherKits());
            case "invDeletion" -> settings.setInvDeletion(!settings.isInvDeletion());
            default -> {
                return;
            }
        }
        BetterKitsPlugin.get().getSettingsRepository().set(kitName, settings);
        refresh();
    }

    private void refresh() {
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();

        EventData addHand = EventData.of("Action", "addHand");
        EventData addId = new EventData().append("Action", "addId")
            .append("@ItemId", "#AddItemIdInput.Value")
            .append("@ItemQty", "#AddQtyInput.Value");
        EventData cooldown = new EventData().append("Action", "cooldown")
            .append("@Cooldown", "#CooldownInput.Value");
        EventData worlds = new EventData().append("Action", "worlds")
            .append("@Worlds", "#WorldsInput.Value");
        EventData flagStacking = EventData.of("Action", "flag").append("@Flag", "stacking");
        EventData flagOverlap = EventData.of("Action", "flag").append("@Flag", "overlap");
        EventData flagInv = EventData.of("Action", "flag").append("@Flag", "invDeletion");
        EventData close = EventData.of("Action", "close");

        events.addEventBinding(CustomUIEventBindingType.Activating, "#AddFromHandButton", addHand, false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#AddButton", addId, false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CooldownButton", cooldown, false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#WorldsButton", worlds, false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#FlagStackingButton", flagStacking, false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#FlagOverlapButton", flagOverlap, false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#FlagInvDelButton", flagInv, false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", close, false);

        KitSettings settings = BetterKitsPlugin.get().getSettingsRepository().getOrDefault(kitName);
        cmd.set("#CooldownInput.Value", String.valueOf(settings.getCooldownSeconds()));
        cmd.set("#WorldsInput.Value", String.join(", ", settings.getAllowedWorlds() != null
            ? settings.getAllowedWorlds() : List.of("all")));
        updateFlagLabels(cmd, settings);

        buildItems(cmd, events);
        sendUpdate(cmd, events, false);
    }

    private void buildItems(UICommandBuilder cmd, UIEventBuilder events) {
        cmd.clear("#ItemsList");
        KitDefinition kit = getKit();
        if (kit == null || kit.getItems() == null || kit.getItems().isEmpty()) {
            cmd.appendInline("#ItemsList", "Label { Text: \"No items in kit.\"; Style: (FontSize: 12, TextColor: #7a90a8); Anchor: (Height: 20); }");
            return;
        }
        int index = 0;
        for (KitItem item : kit.getItems()) {
            if (item == null) {
                continue;
            }
            cmd.append("#ItemsList", "Pages/BetterKitsEditorItem.ui");
            String selector = "#ItemsList[" + index + "]";
            if (item.getId() == null || item.getId().isBlank()) {
                cmd.setNull(selector + " #Icon.ItemId");
                cmd.set(selector + " #Icon.Visible", false);
            } else {
                cmd.set(selector + " #Icon.ItemId", item.getId());
                cmd.set(selector + " #Icon.Visible", true);
            }
            cmd.set(selector + " #ItemId.Text", item.getId() == null ? "" : item.getId());
            cmd.set(selector + " #ItemQty.Text", "x" + Math.max(1, item.getQuantity()));

            EventData remove = EventData.of("Action", "remove").append("Index", String.valueOf(index));
            events.addEventBinding(CustomUIEventBindingType.Activating, selector + " #RemoveButton", remove, false);
            index++;
        }
    }

    private KitDefinition getKit() {
        if (repository == null) {
            return null;
        }
        return repository.getKit(kitName);
    }

    private static void addOrMergeItem(KitDefinition kit, String itemId, int qty) {
        List<KitItem> items = kit.getItems() != null ? new ArrayList<>(kit.getItems()) : new ArrayList<>();
        for (KitItem item : items) {
            if (item != null && itemId.equalsIgnoreCase(item.getId())) {
                item.setQuantity(item.getQuantity() + qty);
                kit.setItems(items);
                return;
            }
        }
        items.add(new KitItem(itemId, qty));
        kit.setItems(items);
    }

    private static int parseQuantity(String raw) {
        if (raw == null || raw.isBlank()) {
            return 1;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static int parseIndex(KitEditorEventData data) {
        if (data == null || data.index == null || data.index.isBlank()) {
            return -1;
        }
        try {
            return Integer.parseInt(data.index);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static long parseLong(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ignored) {
            return -1L;
        }
    }

    private void updateFlagLabels(UICommandBuilder cmd, KitSettings settings) {
        if (cmd == null || settings == null) {
            return;
        }
        cmd.set("#FlagStackingLabel.Text", "STACKING: " + (settings.isAllowKitStacking() ? "ON" : "OFF"));
        cmd.set("#FlagOverlapLabel.Text", "OVERLAP: " + (settings.isOverlapOtherKits() ? "ON" : "OFF"));
        cmd.set("#FlagInvDelLabel.Text", "INV DEL: " + (settings.isInvDeletion() ? "ON" : "OFF"));
    }

    public static final class KitEditorEventData {
        public static final BuilderCodec<KitEditorEventData> CODEC = BuilderCodec.builder(
            KitEditorEventData.class,
            KitEditorEventData::new
        )
            .append(new KeyedCodec<>("Index", Codec.STRING),
                (data, value) -> data.index = value,
                data -> data.index).add()
            .append(new KeyedCodec<>("Action", Codec.STRING),
                (data, value) -> data.action = value,
                data -> data.action).add()
            .append(new KeyedCodec<>("@ItemId", Codec.STRING),
                (data, value) -> data.itemId = value,
                data -> data.itemId).add()
            .append(new KeyedCodec<>("@ItemQty", Codec.STRING),
                (data, value) -> data.itemQty = value,
                data -> data.itemQty).add()
            .append(new KeyedCodec<>("@Cooldown", Codec.STRING),
                (data, value) -> data.cooldown = value,
                data -> data.cooldown).add()
            .append(new KeyedCodec<>("@Worlds", Codec.STRING),
                (data, value) -> data.worlds = value,
                data -> data.worlds).add()
            .append(new KeyedCodec<>("@Flag", Codec.STRING),
                (data, value) -> data.flag = value,
                data -> data.flag).add()
            .build();

        public String index;
        public String action;
        public String itemId;
        public String itemQty;
        public String cooldown;
        public String worlds;
        public String flag;

        public KitEditorEventData() {
        }
    }
}

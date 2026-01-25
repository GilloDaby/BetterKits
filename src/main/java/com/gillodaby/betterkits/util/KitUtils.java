package com.gillodaby.betterkits.util;

import com.gillodaby.betterkits.BetterKitsPlugin;
import com.gillodaby.betterkits.KitRepository;
import com.gillodaby.betterkits.models.KitDefinition;
import com.gillodaby.betterkits.models.KitItem;
import com.gillodaby.betterkits.settings.KitSettings;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public final class KitUtils {

    private KitUtils() {
    }

    public static boolean hasKitPermission(CommandSender sender, String kitName) {
        String key = KitRepository.normalizeName(kitName);
        if (key.isEmpty()) {
            return false;
        }
        if (sender.hasPermission(com.gillodaby.betterkits.settings.KitPermissions.KITS_MANAGE)
            || sender.hasPermission(com.gillodaby.betterkits.settings.KitPermissions.ADMIN)) {
            return true;
        }
        if (sender.hasPermission(com.gillodaby.betterkits.settings.KitPermissions.KITS_GET_ANY)
            || sender.hasPermission(com.gillodaby.betterkits.settings.KitPermissions.KITS_GET)) {
            return true;
        }
        return sender.hasPermission(com.gillodaby.betterkits.settings.KitPermissions.KITS_GET_PREFIX + key);
    }

    public static boolean hasPermission(CommandSender sender, String permission) {
        if (sender == null || permission == null) {
            return false;
        }
        return sender.hasPermission(permission);
    }

    public static boolean giveKitToPlayer(Store<EntityStore> store, Ref<EntityStore> ref, KitDefinition kit) {
        if (kit == null) {
            return false;
        }
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return false;
        }
        ItemContainer combinedHotbar = player.getInventory().getCombinedHotbarFirst();
        ItemContainer combinedAll = player.getInventory().getCombinedEverything();
        ItemContainer armor = player.getInventory().getArmor();

        List<ItemStack> items = toItemStacks(kit.getItems());
        List<ItemStack> armorItems = toItemStacks(kit.getArmor());

        BetterKitsPlugin plugin = BetterKitsPlugin.get();
        KitSettings settings = plugin.getSettingsForKit(kit);
        UUID playerId = player.getUuid();

        if (settings != null && settings.getAllowedWorlds() != null
            && !settings.getAllowedWorlds().contains("all")) {
            String worldName = player.getWorld().getName().toLowerCase();
            boolean allowed = settings.getAllowedWorlds().stream()
                .map(String::toLowerCase)
                .anyMatch(worldName::equals);
            if (!allowed) {
                player.sendMessage(Message.raw("Kit disabled in this world!"));
                return false;
            }
        }

        if (settings != null && settings.isOverlapOtherKits()) {
            KitDefinition lastKit = plugin.getLastKit(playerId);
            if (lastKit != null) {
                String lastName = KitRepository.normalizeName(lastKit.getName());
                String currentName = KitRepository.normalizeName(kit.getName());
                if (!lastName.equals(currentName)) {
                    removeStacks(combinedAll, toItemStacks(lastKit.getItems()));
                    removeStacks(armor, toItemStacks(lastKit.getArmor()));
                    player.invalidateEquipmentNetwork();
                }
            }
        }

        if (settings != null && !settings.isAllowKitStacking()) {
            KitDefinition lastKit = plugin.getLastKit(playerId);
            if (lastKit != null) {
                String lastName = KitRepository.normalizeName(lastKit.getName());
                String currentName = KitRepository.normalizeName(kit.getName());
                if (lastName.equals(currentName)) {
                    removeStacks(combinedAll, items);
                    removeStacks(armor, armorItems);
                    player.invalidateEquipmentNetwork();
                }
            }
        }

        if (settings != null && settings.isInvDeletion()) {
            player.getInventory().clear();
            player.invalidateEquipmentNetwork();
        }

        if (!items.isEmpty() && combinedHotbar != null) {
            combinedHotbar.addItemStacks(items);
        }
        if (!armorItems.isEmpty() && armor != null) {
            armor.addItemStacks(armorItems);
        }

        plugin.setLastKit(playerId, kit);
        return true;
    }

    private static void removeStacks(ItemContainer container, List<ItemStack> items) {
        if (container == null || items == null || items.isEmpty()) {
            return;
        }
        container.removeItemStacks(new ArrayList<>(items));
    }

    public static List<ItemStack> toItemStacks(List<KitItem> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        return items.stream()
            .filter(item -> item != null
                && item.getId() != null
                && !item.getId().isBlank()
                && item.getQuantity() > 0)
            .map(item -> new ItemStack(item.getId(), item.getQuantity()))
            .collect(Collectors.toList());
    }

    public static String safeKitName(KitDefinition kit) {
        if (kit == null || kit.getName() == null) {
            return "";
        }
        return kit.getName();
    }

    public static String safeKitDisplayName(KitDefinition kit) {
        if (kit == null) {
            return "";
        }
        String displayName = kit.getDisplayName();
        if (displayName == null || displayName.isBlank()) {
            return safeKitName(kit);
        }
        return displayName;
    }
}

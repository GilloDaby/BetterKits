package com.gillodaby.betterkits.commands;

import com.gillodaby.betterkits.BetterKitsPlugin;
import com.gillodaby.betterkits.KitRepository;
import com.gillodaby.betterkits.models.KitDefinition;
import com.gillodaby.betterkits.models.KitItem;
import com.gillodaby.betterkits.settings.KitPermissions;
import com.gillodaby.betterkits.settings.KitSettings;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.function.consumer.ShortObjectConsumer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.List;

public class KitUpdateCommand extends AbstractPlayerCommand {

    private final RequiredArg<String> nameArg;
    private final KitRepository repository;
    private final BetterKitsPlugin plugin;

    public KitUpdateCommand(BetterKitsPlugin plugin) {
        super("update", "kits.command.update.desc");
        this.nameArg = withRequiredArg("name", "kits.command.update.name.desc", ArgTypes.STRING);
        requirePermission(KitPermissions.KITS_UPDATE);
        this.repository = plugin.getRepository();
        this.plugin = plugin;
    }

    @Override
    protected void execute(CommandContext ctx,
                           Store<EntityStore> store,
                           Ref<EntityStore> ref,
                           PlayerRef playerRef,
                           World world) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            ctx.sendMessage(Message.raw("Player is not in world."));
            return;
        }

        String name = nameArg.get(ctx);
        String key = KitRepository.normalizeName(name);
        if (key.isEmpty()) {
            ctx.sendMessage(Message.raw("Kit name cannot be empty."));
            return;
        }

        KitDefinition kit = repository.getKit(key);
        if (kit == null) {
            ctx.sendMessage(Message.raw("Kit '" + key + "' not found."));
            return;
        }

        List<KitItem> items = readItems(player.getInventory().getCombinedHotbarFirst());
        List<KitItem> armor = readItems(player.getInventory().getArmor());

        kit.setItems(items);
        kit.setArmor(armor);
        if (kit.getDisplayName() == null || kit.getDisplayName().isBlank()) {
            kit.setDisplayName(name != null ? name.trim() : key);
        }

        if (!repository.setKit(kit)) {
            ctx.sendMessage(Message.raw("Failed to update kit."));
            return;
        }

        KitSettings existing = plugin.getSettingsRepository().get(key);
        if (existing == null) {
            KitSettings settings = new KitSettings(0L, kit.isAllowKitStacking(), true, false, List.of("all"));
            plugin.getSettingsRepository().set(key, settings);
        }

        ctx.sendMessage(Message.raw("Updated kit: " + key));
    }

    private static List<KitItem> readItems(ItemContainer container) {
        List<KitItem> items = new ArrayList<>();
        if (container == null) {
            return items;
        }
        ShortObjectConsumer<ItemStack> consumer = (slot, item) -> {
            if (ItemStack.isEmpty(item)) {
                return;
            }
            items.add(new KitItem(item.getItemId(), item.getQuantity()));
        };
        container.forEach(consumer);
        return items;
    }
}

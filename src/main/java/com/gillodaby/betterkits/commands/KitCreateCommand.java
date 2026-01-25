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
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
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

public class KitCreateCommand extends AbstractPlayerCommand {

    private final RequiredArg<String> nameArg;
    private final OptionalArg<Integer> cooldownArg;
    private final OptionalArg<Boolean> stackingArg;
    private final KitRepository repository;
    private final BetterKitsPlugin plugin;

    public KitCreateCommand(BetterKitsPlugin plugin) {
        super("create", "kits.command.create.desc");
        this.nameArg = withRequiredArg("name", "kits.command.create.name.desc", ArgTypes.STRING);
        this.cooldownArg = withOptionalArg("cooldown", "kits.command.create.cooldown.desc", ArgTypes.INTEGER);
        this.stackingArg = withOptionalArg("stacking", "kits.command.create.stacking.desc", ArgTypes.BOOLEAN);
        requirePermission(KitPermissions.KITS_CREATE);
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
        if (repository.getKit(key) != null) {
            ctx.sendMessage(Message.raw("Kit '" + key + "' already exists."));
            return;
        }

        String displayName = name != null ? name.trim() : key;
        List<KitItem> items = readItems(player.getInventory().getCombinedHotbarFirst());
        List<KitItem> armor = readItems(player.getInventory().getArmor());

        long cooldownSeconds = 0L;
        if (cooldownArg.provided(ctx)) {
            Integer value = cooldownArg.get(ctx);
            if (value != null && value >= 0) {
                cooldownSeconds = value.longValue();
            } else {
                ctx.sendMessage(Message.raw("Cooldown must be a positive number of seconds."));
                return;
            }
        }

        boolean allowStacking = true;
        if (stackingArg.provided(ctx)) {
            Boolean value = stackingArg.get(ctx);
            allowStacking = value != null && value;
        }

        KitDefinition kit = new KitDefinition(key, displayName, 0L, items, armor, allowStacking);
        if (!repository.setKit(kit)) {
            ctx.sendMessage(Message.raw("Failed to create kit."));
            return;
        }

        KitSettings settings = new KitSettings(cooldownSeconds, allowStacking, true, false, List.of("all"));
        plugin.getSettingsRepository().set(key, settings);
        ctx.sendMessage(Message.raw("Created kit: " + key));
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

package com.gillodaby.betterkits.commands;

import com.gillodaby.betterkits.BetterKitsPlugin;
import com.gillodaby.betterkits.KitRepository;
import com.gillodaby.betterkits.models.KitDefinition;
import com.gillodaby.betterkits.pages.KitMenuPage;
import com.gillodaby.betterkits.settings.KitPermissions;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.List;

public class KitsMenuCommand extends AbstractPlayerCommand {

    private final BetterKitsPlugin plugin;
    private final KitRepository repository;

    public KitsMenuCommand(BetterKitsPlugin plugin) {
        super("kits", "kits.command.menu.desc");
        requirePermission(KitPermissions.KITS_MENU);
        this.plugin = plugin;
        this.repository = plugin.getRepository();
    }

    @Override
    protected void execute(CommandContext ctx,
                           Store<EntityStore> store,
                           Ref<EntityStore> ref,
                           PlayerRef playerRef,
                           World world) {
        long remaining = plugin.getRemainingMenuCommandCooldownSeconds(playerRef.getUuid());
        if (remaining > 0) {
            ctx.sendMessage(Message.raw(
                "You can open the kit menu in " + BetterKitsPlugin.formatDuration(remaining) + "."
            ));
            return;
        }

        List<KitDefinition> kits = new ArrayList<>(repository.list());
        if (kits.isEmpty()) {
            ctx.sendMessage(Message.raw("No kits available."));
            return;
        }

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null || player.getPageManager() == null) {
            ctx.sendMessage(Message.raw("Player not available."));
            return;
        }

        PageManager pageManager = player.getPageManager();
        pageManager.openCustomPage(ref, store, new KitMenuPage(playerRef, kits, false));
        plugin.markMenuCommandUsed(playerRef.getUuid());
    }
}

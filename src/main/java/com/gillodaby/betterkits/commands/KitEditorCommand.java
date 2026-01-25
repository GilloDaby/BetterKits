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
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.List;

public class KitEditorCommand extends AbstractPlayerCommand {

    private final OptionalArg<String> actionArg;
    private final BetterKitsPlugin plugin;
    private final KitRepository repository;

    public KitEditorCommand(BetterKitsPlugin plugin) {
        super("editor", "kits.command.editor.desc");
        requirePermission(KitPermissions.KITS_MANAGE);
        this.plugin = plugin;
        this.repository = plugin.getRepository();
        this.actionArg = withOptionalArg("action", "kits.command.editor.action.desc", ArgTypes.STRING);
    }

    @Override
    protected void execute(CommandContext ctx,
                           Store<EntityStore> store,
                           Ref<EntityStore> ref,
                           PlayerRef playerRef,
                           World world) {
        String action = actionArg.provided(ctx) ? actionArg.get(ctx) : null;
        if (action != null && !action.isBlank()) {
            ctx.sendMessage(Message.raw("Use /kit editor to open the menu editor."));
        }
        openEditorMenu(ctx, store, ref, playerRef);
    }

    private void openEditorMenu(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef) {
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
        pageManager.openCustomPage(ref, store, new KitMenuPage(playerRef, kits, true));
    }

}

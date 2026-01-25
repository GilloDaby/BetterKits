package com.gillodaby.betterkits.commands;

import com.gillodaby.betterkits.BetterKitsPlugin;
import com.gillodaby.betterkits.KitRepository;
import com.gillodaby.betterkits.models.KitDefinition;
import com.gillodaby.betterkits.settings.KitPermissions;
import com.gillodaby.betterkits.util.KitUtils;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class KitCommand extends AbstractPlayerCommand {

    private final BetterKitsPlugin plugin;
    private final KitRepository repository;

    public KitCommand(BetterKitsPlugin plugin) {
        super("kit", "kits.command.desc");
        requirePermission(KitPermissions.KITS_USE);

        this.plugin = plugin;
        this.repository = plugin.getRepository();

        addSubCommand(new KitGetSubCommand(plugin));
        addSubCommand(new KitListSubCommand(plugin));
        addSubCommand(new KitHelpSubCommand());
        addSubCommand(new KitCreateCommand(plugin));
        addSubCommand(new KitUpdateCommand(plugin));
        addSubCommand(new KitCooldownCommand(plugin));
        addSubCommand(new KitWorldsCommand(plugin));
        addSubCommand(new KitFlagsCommand(plugin));
        addSubCommand(new KitEditorCommand(plugin));
        addSubCommand(new KitDeleteCommand(plugin));
        addSubCommand(new KitReloadCommand(plugin));
    }

    @Override
    protected void execute(CommandContext ctx,
                           Store<EntityStore> store,
                           Ref<EntityStore> ref,
                           PlayerRef playerRef,
                           World world) {
        String input = normalizeInput(ctx);
        if (input.isBlank()) {
            openMenu(ctx, store, ref, playerRef);
            return;
        }
        String[] parts = input.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isBlank()) {
            openMenu(ctx, store, ref, playerRef);
            return;
        }
        String first = parts[0].toLowerCase();
        if ("editor".equals(first)) {
            if (!ctx.sender().hasPermission(KitPermissions.KITS_MANAGE)
                && !ctx.sender().hasPermission(KitPermissions.ADMIN)) {
                ctx.sendMessage(Message.raw("You do not have permission to use /kit editor."));
                return;
            }
            openEditorMenu(ctx, store, ref, playerRef);
            return;
        }
        if ("get".equals(first)) {
            if (parts.length < 2) {
                ctx.sendMessage(Message.raw("Usage: /kit get <name>"));
                return;
            }
            handleKitGet(ctx, store, ref, playerRef, parts[1]);
            return;
        }
        handleKitGet(ctx, store, ref, playerRef, parts[0]);
    }

    private String normalizeInput(CommandContext ctx) {
        String input = ctx.getInputString();
        if (input == null) {
            return "";
        }
        String trimmed = input.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1).trim();
        }
        String cmdName = ctx.getCalledCommand() != null ? ctx.getCalledCommand().getName() : "kit";
        if (trimmed.equalsIgnoreCase(cmdName)) {
            return "";
        }
        String prefix = cmdName.toLowerCase() + " ";
        if (trimmed.toLowerCase().startsWith(prefix)) {
            return trimmed.substring(prefix.length()).trim();
        }
        return trimmed;
    }

    private void openMenu(CommandContext ctx,
                          Store<EntityStore> store,
                          Ref<EntityStore> ref,
                          PlayerRef playerRef) {
        if (!ctx.sender().hasPermission(KitPermissions.KITS_MENU)) {
            ctx.sendMessage(Message.raw("You do not have permission to use /kits."));
            return;
        }
        List<KitDefinition> kits = new java.util.ArrayList<>(repository.list());
        if (kits.isEmpty()) {
            ctx.sendMessage(Message.raw("No kits available."));
            return;
        }
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null || player.getPageManager() == null) {
            ctx.sendMessage(Message.raw("Player not available."));
            return;
        }
        player.getPageManager().openCustomPage(ref, store, new com.gillodaby.betterkits.pages.KitMenuPage(playerRef, kits));
        plugin.markMenuCommandUsed(playerRef.getUuid());
    }

    private void openEditorMenu(CommandContext ctx,
                                Store<EntityStore> store,
                                Ref<EntityStore> ref,
                                PlayerRef playerRef) {
        List<KitDefinition> kits = new java.util.ArrayList<>(repository.list());
        if (kits.isEmpty()) {
            ctx.sendMessage(Message.raw("No kits available."));
            return;
        }
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null || player.getPageManager() == null) {
            ctx.sendMessage(Message.raw("Player not available."));
            return;
        }
        player.getPageManager().openCustomPage(ref, store, new com.gillodaby.betterkits.pages.KitMenuPage(playerRef, kits, true));
    }

    private static void handleKitGet(CommandContext ctx,
                                     Store<EntityStore> store,
                                     Ref<EntityStore> ref,
                                     PlayerRef playerRef,
                                     String name) {
        String key = KitRepository.normalizeName(name);
        if (key.isEmpty()) {
            ctx.sendMessage(Message.raw("Kit name cannot be empty."));
            return;
        }
        if (!KitUtils.hasKitPermission(ctx.sender(), key)) {
            ctx.sendMessage(Message.raw("You do not have permission to use this kit."));
            return;
        }
        KitDefinition kit = BetterKitsPlugin.get().getRepository().getKit(key);
        if (kit == null) {
            ctx.sendMessage(Message.raw("Kit '" + key + "' not found."));
            return;
        }
        long remaining = BetterKitsPlugin.get().getRemainingCooldownSeconds(playerRef.getUuid(), kit);
        if (remaining > 0) {
            String display = KitUtils.safeKitDisplayName(kit);
            ctx.sendMessage(Message.raw(
                "Kit '" + display + "' is available in " + BetterKitsPlugin.formatDuration(remaining) + "."
            ));
            return;
        }
        if (!KitUtils.giveKitToPlayer(store, ref, kit)) {
            ctx.sendMessage(Message.raw("Failed to give kit."));
            return;
        }
        BetterKitsPlugin.get().markKitUsed(playerRef.getUuid(), kit);
        String display = KitUtils.safeKitDisplayName(kit);
        ctx.sendMessage(Message.raw("Kit '" + display + "' received successfully!"));
    }

    private static final class KitGetSubCommand extends AbstractPlayerCommand {

        private final BetterKitsPlugin plugin;
        private final KitRepository repository;
        private final RequiredArg<String> kitNameArg;

        private KitGetSubCommand(BetterKitsPlugin plugin) {
            super("get", "kits.command.get.desc");
            this.plugin = plugin;
            this.repository = plugin.getRepository();
            this.kitNameArg = withRequiredArg("name", "kits.command.get.name.desc", ArgTypes.STRING);
        }

        @Override
        protected void execute(CommandContext ctx,
                               Store<EntityStore> store,
                               Ref<EntityStore> ref,
                               PlayerRef playerRef,
                               World world) {
            String name = kitNameArg.get(ctx);
            handleKitGet(ctx, store, ref, playerRef, name);
        }
    }

    private static final class KitListSubCommand extends AbstractPlayerCommand {

        private final KitRepository repository;

        private KitListSubCommand(BetterKitsPlugin plugin) {
            super("list", "kits.command.list.desc");
            requirePermission(KitPermissions.KITS_LIST);
            this.repository = plugin.getRepository();
        }

        @Override
        protected void execute(CommandContext ctx,
                               Store<EntityStore> store,
                               Ref<EntityStore> ref,
                               PlayerRef playerRef,
                               World world) {
            List<String> names = repository.list().stream()
                .map(KitUtils::safeKitName)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
            if (names.isEmpty()) {
                ctx.sendMessage(Message.raw("No kits found."));
                return;
            }
            ctx.sendMessage(Message.raw("Available kits: " + String.join(", ", names)));
        }
    }

    private static final class KitHelpSubCommand extends CommandBase {

        private KitHelpSubCommand() {
            super("help", "kits.command.help.desc");
        }

        @Override
        protected void executeSync(CommandContext ctx) {
            ctx.sendMessage(Message.raw("=== Kit Commands ==="));
            ctx.sendMessage(Message.raw("/kit <name> - Get a specific kit"));
            ctx.sendMessage(Message.raw("/kits - Open kit selection menu"));
            ctx.sendMessage(Message.raw("/kit get <name> - Get a specific kit"));
            ctx.sendMessage(Message.raw("/kit list - List all available kits"));
            ctx.sendMessage(Message.raw("/kit create <name> [cooldown] [stacking] - Create kit from inventory"));
            ctx.sendMessage(Message.raw("/kit update <name> - Update kit from inventory"));
            ctx.sendMessage(Message.raw("/kit cooldown <name> [seconds] - View or set cooldown"));
            ctx.sendMessage(Message.raw("/kit worlds <name> [worlds] - View or set allowed worlds"));
            ctx.sendMessage(Message.raw("/kit flags <name> [flag] [value] - View or set flags"));
            ctx.sendMessage(Message.raw("/kit editor - Open kit editor menu"));
            ctx.sendMessage(Message.raw("/kit delete <name> - Delete a kit"));
            ctx.sendMessage(Message.raw("/kit reload - Reload kits from disk"));
            ctx.sendMessage(Message.raw("/kit help - Show this help"));
        }
    }
}

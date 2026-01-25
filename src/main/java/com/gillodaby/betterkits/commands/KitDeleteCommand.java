package com.gillodaby.betterkits.commands;

import com.gillodaby.betterkits.BetterKitsPlugin;
import com.gillodaby.betterkits.KitRepository;
import com.gillodaby.betterkits.settings.KitPermissions;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

public class KitDeleteCommand extends CommandBase {

    private final RequiredArg<String> nameArg;
    private final KitRepository repository;
    private final BetterKitsPlugin plugin;

    public KitDeleteCommand(BetterKitsPlugin plugin) {
        super("delete", "kits.command.delete.desc");
        this.nameArg = withRequiredArg("name", "kits.command.delete.name.desc", ArgTypes.STRING);
        requirePermission(KitPermissions.KITS_DELETE);
        this.repository = plugin.getRepository();
        this.plugin = plugin;
    }

    @Override
    protected void executeSync(CommandContext ctx) {
        String name = nameArg.get(ctx);
        String key = KitRepository.normalizeName(name);
        if (key.isEmpty()) {
            ctx.sendMessage(Message.raw("Kit name cannot be empty."));
            return;
        }
        if (!repository.removeKit(key)) {
            ctx.sendMessage(Message.raw("Kit '" + key + "' not found."));
            return;
        }
        plugin.getSettingsRepository().remove(key);
        ctx.sendMessage(Message.raw("Deleted kit: " + key));
    }
}

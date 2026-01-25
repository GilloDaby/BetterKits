package com.gillodaby.betterkits.commands;

import com.gillodaby.betterkits.BetterKitsPlugin;
import com.gillodaby.betterkits.KitRepository;
import com.gillodaby.betterkits.settings.KitPermissions;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

public class KitReloadCommand extends CommandBase {

    private final KitRepository repository;
    private final BetterKitsPlugin plugin;

    public KitReloadCommand(BetterKitsPlugin plugin) {
        super("reload", "kits.command.reload.desc");
        requirePermission(KitPermissions.KITS_RELOAD);
        this.plugin = plugin;
        this.repository = plugin.getRepository();
    }

    @Override
    protected void executeSync(CommandContext ctx) {
        repository.load();
        if (plugin.getSettingsRepository() != null) {
            plugin.getSettingsRepository().load();
        }
        ctx.sendMessage(Message.raw("Kits and settings reloaded."));
    }
}

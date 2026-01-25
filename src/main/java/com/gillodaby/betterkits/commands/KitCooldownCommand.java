package com.gillodaby.betterkits.commands;

import com.gillodaby.betterkits.BetterKitsPlugin;
import com.gillodaby.betterkits.KitRepository;
import com.gillodaby.betterkits.settings.KitPermissions;
import com.gillodaby.betterkits.settings.KitSettings;
import com.gillodaby.betterkits.settings.KitSettingsRepository;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

public class KitCooldownCommand extends CommandBase {

    private final RequiredArg<String> nameArg;
    private final KitRepository repository;
    private final KitSettingsRepository settingsRepository;

    public KitCooldownCommand(BetterKitsPlugin plugin) {
        super("cooldown", "kits.command.cooldown.desc");
        this.nameArg = withRequiredArg("name", "kits.command.cooldown.name.desc", ArgTypes.STRING);
        setAllowsExtraArguments(true);
        requirePermission(KitPermissions.KITS_COOLDOWN);
        this.repository = plugin.getRepository();
        this.settingsRepository = plugin.getSettingsRepository();
    }

    @Override
    protected void executeSync(CommandContext ctx) {
        String name = nameArg.get(ctx);
        String key = KitRepository.normalizeName(name);
        if (key.isEmpty()) {
            ctx.sendMessage(Message.raw("Kit name cannot be empty."));
            return;
        }
        if (repository.getKit(key) == null) {
            ctx.sendMessage(Message.raw("Kit '" + key + "' not found."));
            return;
        }

        KitSettings settings = settingsRepository.getOrDefault(key);
        Integer value = parseSecondArg(ctx.getInputString());
        if (value != null) {
            if (value < 0) {
                ctx.sendMessage(Message.raw("Cooldown must be a positive number of seconds."));
                return;
            }
            settings.setCooldownSeconds(value.longValue());
            settingsRepository.set(key, settings);
            ctx.sendMessage(Message.raw("Cooldown for kit '" + key + "' set to " + value + "s."));
            return;
        }

        long cooldown = settings.getCooldownSeconds();
        ctx.sendMessage(Message.raw("Cooldown for kit '" + key + "' is " + cooldown + "s."));
    }

    private Integer parseSecondArg(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String[] parts = input.trim().split("\\s+");
        if (parts.length < 2) {
            return null;
        }
        try {
            return Integer.parseInt(parts[1]);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}

package com.gillodaby.betterkits.commands;

import com.gillodaby.betterkits.BetterKitsPlugin;
import com.gillodaby.betterkits.KitRepository;
import com.gillodaby.betterkits.settings.KitPermissions;
import com.gillodaby.betterkits.settings.KitSettings;
import com.gillodaby.betterkits.settings.KitSettingsRepository;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class KitWorldsCommand extends CommandBase {

    private final RequiredArg<String> nameArg;
    private final OptionalArg<String> worldsArg;
    private final KitRepository repository;
    private final KitSettingsRepository settingsRepository;

    public KitWorldsCommand(BetterKitsPlugin plugin) {
        super("worlds", "kits.command.worlds.desc");
        this.nameArg = withRequiredArg("name", "kits.command.worlds.name.desc", ArgTypes.STRING);
        this.worldsArg = withOptionalArg("worlds", "kits.command.worlds.list.desc", ArgTypes.STRING);
        requirePermission(KitPermissions.KITS_WORLDS);
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
        if (worldsArg.provided(ctx)) {
            String raw = worldsArg.get(ctx);
            if (raw == null || raw.isBlank()) {
                ctx.sendMessage(Message.raw("World list cannot be empty."));
                return;
            }
            List<String> worlds = Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(String::toLowerCase)
                .collect(Collectors.toCollection(ArrayList::new));

            if (worlds.isEmpty() || worlds.stream().anyMatch(s -> s.equalsIgnoreCase("all"))) {
                worlds = List.of("all");
            }

            settings.setAllowedWorlds(worlds);
            settingsRepository.set(key, settings);
            ctx.sendMessage(Message.raw("Allowed worlds for kit '" + key + "' set to: " + String.join(", ", worlds)));
            return;
        }

        List<String> worlds = settings.getAllowedWorlds();
        if (worlds == null || worlds.isEmpty()) {
            ctx.sendMessage(Message.raw("Allowed worlds for kit '" + key + "' are: all"));
            return;
        }
        ctx.sendMessage(Message.raw("Allowed worlds for kit '" + key + "' are: " + String.join(", ", worlds)));
    }
}

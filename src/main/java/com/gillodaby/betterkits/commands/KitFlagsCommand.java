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

public class KitFlagsCommand extends CommandBase {

    private final RequiredArg<String> nameArg;
    private final OptionalArg<String> flagArg;
    private final OptionalArg<Boolean> valueArg;
    private final KitRepository repository;
    private final KitSettingsRepository settingsRepository;

    public KitFlagsCommand(BetterKitsPlugin plugin) {
        super("flags", "kits.command.flags.desc");
        this.nameArg = withRequiredArg("name", "kits.command.flags.name.desc", ArgTypes.STRING);
        this.flagArg = withOptionalArg("flag", "kits.command.flags.flag.desc", ArgTypes.STRING);
        this.valueArg = withOptionalArg("value", "kits.command.flags.value.desc", ArgTypes.BOOLEAN);
            requirePermission(KitPermissions.KITS_FLAGS);
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
        if (!flagArg.provided(ctx)) {
            ctx.sendMessage(Message.raw("Flags for kit '" + key + "':"));
            ctx.sendMessage(Message.raw("- stacking: " + settings.isAllowKitStacking()));
            ctx.sendMessage(Message.raw("- overlap: " + settings.isOverlapOtherKits()));
            ctx.sendMessage(Message.raw("- invDeletion: " + settings.isInvDeletion()));
            return;
        }

        String flag = flagArg.get(ctx);
        if (flag == null || flag.isBlank()) {
            ctx.sendMessage(Message.raw("Flag name cannot be empty."));
            return;
        }

        String normalized = flag.trim().toLowerCase();
        if (!valueArg.provided(ctx)) {
            ctx.sendMessage(Message.raw(formatFlagValue(normalized, settings)));
            return;
        }

        Boolean value = valueArg.get(ctx);
        if (value == null) {
            ctx.sendMessage(Message.raw("Flag value must be true or false."));
            return;
        }

        if (applyFlag(normalized, value, settings)) {
            settingsRepository.set(key, settings);
            ctx.sendMessage(Message.raw("Flag '" + normalized + "' for kit '" + key + "' set to " + value + "."));
            return;
        }

        ctx.sendMessage(Message.raw("Unknown flag: " + flag + ". Use stacking, overlap, or invDeletion."));
    }

    private static boolean applyFlag(String normalized, boolean value, KitSettings settings) {
        switch (normalized) {
            case "stacking":
            case "allowstacking":
                settings.setAllowKitStacking(value);
                return true;
            case "overlap":
            case "overlapotherkits":
                settings.setOverlapOtherKits(value);
                return true;
            case "invdeletion":
            case "inventorydeletion":
            case "deleteinventory":
                settings.setInvDeletion(value);
                return true;
            default:
                return false;
        }
    }

    private static String formatFlagValue(String normalized, KitSettings settings) {
        switch (normalized) {
            case "stacking":
            case "allowstacking":
                return "Flag 'stacking' is " + settings.isAllowKitStacking();
            case "overlap":
            case "overlapotherkits":
                return "Flag 'overlap' is " + settings.isOverlapOtherKits();
            case "invdeletion":
            case "inventorydeletion":
            case "deleteinventory":
                return "Flag 'invDeletion' is " + settings.isInvDeletion();
            default:
                return "Unknown flag: " + normalized + ". Use stacking, overlap, or invDeletion.";
        }
    }
}

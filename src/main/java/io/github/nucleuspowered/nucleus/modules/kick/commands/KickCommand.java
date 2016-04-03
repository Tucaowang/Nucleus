/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kick.commands;

import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.internal.annotations.*;
import io.github.nucleuspowered.nucleus.internal.command.OldCommandBase;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.HashMap;
import java.util.Map;

/**
 * Kicks a player
 *
 * Permission: quickstart.kick.base
 */
@Permissions(suggestedLevel = SuggestedLevel.MOD)
@NoWarmup
@NoCooldown
@NoCost
@RegisterCommand("kick")
public class KickCommand extends OldCommandBase<CommandSource> {

    private final String player = "player";
    private final String reason = "reason";

    @Override
    public CommandSpec createSpec() {
        return getSpecBuilderBase().description(Text.of("Kicks a player."))
                .arguments(GenericArguments.onlyOne(GenericArguments.player(Text.of(player))),
                        GenericArguments.optional(GenericArguments.onlyOne(GenericArguments.remainingJoinedStrings(Text.of(reason)))))
                .build();
    }

    @Override
    public Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> m = new HashMap<>();
        m.put("notify", new PermissionInformation(Util.getMessageWithFormat("permission.kick.notify"), SuggestedLevel.MOD));
        return m;
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        Player pl = args.<Player>getOne(player).get();
        String r = args.<String>getOne(reason).orElse(Util.getMessageWithFormat("command.kick.defaultreason"));
        pl.kick(TextSerializers.FORMATTING_CODE.deserialize(r));

        MessageChannel mc = MessageChannel.permission(permissions.getPermissionWithSuffix("notify"));
        mc.send(Util.getTextMessageWithFormat("command.kick.message", pl.getName(), src.getName()));
        mc.send(Util.getTextMessageWithFormat("command.reason", r));
        return CommandResult.success();
    }
}

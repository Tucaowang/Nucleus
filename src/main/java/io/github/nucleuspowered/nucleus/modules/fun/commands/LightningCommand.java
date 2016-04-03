/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.fun.commands;

import io.github.nucleuspowered.nucleus.NameUtil;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.OldCommandBase;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.util.blockray.BlockRayHit;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Permissions
@RegisterCommand({"lightning", "smite", "thor"})
public class LightningCommand extends OldCommandBase<CommandSource> {

    private final String player = "player";

    @Override
    public Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> m = new HashMap<>();
        m.put("others", new PermissionInformation(Util.getMessageWithFormat("permission.others", this.getAliases()[0]), SuggestedLevel.ADMIN));
        return m;
    }

    @Override
    public CommandSpec createSpec() {
        return getSpecBuilderBase().arguments(GenericArguments.optional(GenericArguments.requiringPermission(
                GenericArguments.onlyOne(GenericArguments.player(Text.of(player))), permissions.getPermissionWithSuffix("others")))).build();
    }

    @Override
    public CommandResult executeCommand(final CommandSource src, CommandContext args) throws Exception {
        Optional<Player> opl = this.getUser(Player.class, src, player, args);
        if (!opl.isPresent()) {
            return CommandResult.empty();
        }

        Player pl = opl.get();

        // No argument, let's not smite the player.
        if (!args.getOne(player).isPresent()) {
            // 100 is a good limit here.
            BlockRay<World> playerBlockRay = BlockRay.from(pl).blockLimit(100).filter(BlockRay.continueAfterFilter(BlockRay.onlyAirFilter(), 1)).build();
            Optional<BlockRayHit<World>> obh = playerBlockRay.end();
            Location<World> lightningLocation;
            if (obh.isPresent()) {
                lightningLocation = obh.get().getLocation();
            } else {
                // Smite above, but not on.
                lightningLocation = pl.getLocation().add(0, 3, 0);
            }

            return this.spawnLightning(lightningLocation, src, "command.lightning.success.normal");
        }

        return this.spawnLightning(pl.getLocation(), src, "command.lightning.success.other", NameUtil.getSerialisedName(pl));
    }

    private CommandResult spawnLightning(Location<World> location, CommandSource src, String successKey, String... replacements) {
        World world = location.getExtent();
        Optional<Entity> bolt = world.createEntity(EntityTypes.LIGHTNING, location.getPosition());

        if (bolt.isPresent() && world.spawnEntity(bolt.get(), Cause.of(NamedCause.source(src)))) {
            src.sendMessage(Util.getTextMessageWithFormat(successKey, replacements));
            return CommandResult.success();
        }

        src.sendMessage(Util.getTextMessageWithFormat("command.lightning.error"));
        return CommandResult.empty();
    }
}

/*
 * This file is part of Essence, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.essencepowered.essence.commands.home;

import io.github.essencepowered.essence.Util;
import io.github.essencepowered.essence.api.PluginModule;
import io.github.essencepowered.essence.api.data.WarpLocation;
import io.github.essencepowered.essence.internal.CommandBase;
import io.github.essencepowered.essence.internal.annotations.Modules;
import io.github.essencepowered.essence.internal.annotations.Permissions;
import io.github.essencepowered.essence.internal.annotations.RegisterCommand;
import io.github.essencepowered.essence.internal.annotations.RunAsync;
import io.github.essencepowered.essence.internal.interfaces.InternalEssenceUser;
import io.github.essencepowered.essence.internal.permissions.PermissionInformation;
import io.github.essencepowered.essence.internal.permissions.SuggestedLevel;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.permission.option.OptionSubject;
import org.spongepowered.api.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Permissions(root = "home", alias = "set", suggestedLevel = SuggestedLevel.USER)
@Modules(PluginModule.HOMES)
@RunAsync
@RegisterCommand({"homeset", "sethome"})
public class SetHomeCommand extends CommandBase<Player> {

    private final String homeKey = "home";
    private final Pattern warpName = Pattern.compile("^[a-zA-Z][a-zA-Z0-9]{1,15}$");

    @Override
    public CommandSpec createSpec() {
        return CommandSpec.builder().arguments(GenericArguments.onlyOne(GenericArguments.optional(GenericArguments.string(Text.of(homeKey)))))
                .executor(this).build();
    }

    @Override
    public Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> m = new HashMap<>();
        m.put("unlimited", new PermissionInformation(Util.getMessageWithFormat("permission.homes.unlimited"), SuggestedLevel.ADMIN));
        return m;
    }

    @Override
    public CommandResult executeCommand(Player src, CommandContext args) throws Exception {
        // Get the home key.
        String home = args.<String>getOne(homeKey).orElse("home").toLowerCase();

        // Get the homes.
        InternalEssenceUser iqsu = plugin.getUserLoader().getUser(src);
        Map<String, WarpLocation> msw = iqsu.getHomes();

        if (!warpName.matcher(home).matches()) {
            src.sendMessage(Util.getTextMessageWithFormat("command.sethome.name"));
            return CommandResult.empty();
        }

        int c = getCount(src);
        if (msw.size() >= c) {
            src.sendMessage(Util.getTextMessageWithFormat("command.sethome.limit", String.valueOf(c)));
            return CommandResult.empty();
        }

        // Does the home exist?
        if (msw.containsKey(home) || !iqsu.setHome(home, src.getLocation(), src.getRotation())) {
            src.sendMessage(Util.getTextMessageWithFormat("command.sethome.seterror", home));
            return CommandResult.empty();
        }

        src.sendMessage(Util.getTextMessageWithFormat("command.sethome.set", home));
        return CommandResult.success();
    }

    private int getCount(Player src) {
        if (permissions.testSuffix(src, "unlimited")) {
            return Integer.MAX_VALUE;
        }

        // If we have too many, then
        String homesAllowed = ((OptionSubject) src).getOption("home-count").orElse("1");
        int i = Integer.getInteger(homesAllowed, 1);
        if (i < 1) {
            i = 1;
        }

        return i;
    }
}

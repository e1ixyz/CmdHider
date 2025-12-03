package com.elimcgehee.cmdhider.proxy;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.util.Tristate;
import com.velocitypowered.api.proxy.Player;

import java.util.Locale;

public class ProxyPermissionChecker {

    private final LuckPerms luckPerms;

    public ProxyPermissionChecker(LuckPerms luckPerms) {
        this.luckPerms = luckPerms;
    }

    public boolean hasPermission(Player player, String permission) {
        if (permission == null || permission.isEmpty()) {
            return true;
        }

        if (luckPerms != null) {
            User user = luckPerms.getPlayerAdapter(Player.class).getUser(player);
            if (user != null) {
                Tristate result = user.getCachedData().getPermissionData().checkPermission(permission);
                if (result != Tristate.UNDEFINED) {
                    return result.asBoolean();
                }
            }
        }

        return player.hasPermission(permission);
    }

    public String getPrimaryGroup(Player player) {
        if (luckPerms != null) {
            User user = luckPerms.getPlayerAdapter(Player.class).getUser(player);
            if (user != null && user.getPrimaryGroup() != null) {
                return user.getPrimaryGroup().toLowerCase(Locale.ROOT);
            }
        }
        return "default";
    }
}

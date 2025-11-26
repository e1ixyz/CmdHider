package com.elimcgehee.cmdhider;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.util.Tristate;
import org.bukkit.entity.Player;

import java.util.Optional;

public class PermissionChecker {

    private final LuckPerms luckPerms;

    public PermissionChecker(LuckPerms luckPerms) {
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
}

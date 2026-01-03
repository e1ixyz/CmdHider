package com.elimcgehee.cmdhider.proxy;

import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.command.CommandExecuteEvent.CommandResult;
import com.velocitypowered.api.event.command.PlayerAvailableCommandsEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import java.util.Locale;
import java.util.function.Predicate;
import java.util.logging.Logger;

public class ProxyCommandFilter {

    private final ProxyCmdHiderPlugin plugin;
    private final ProxyServer server;
    private final ProxyPermissionChecker permissionChecker;
    private final Logger logger;

    public ProxyCommandFilter(ProxyCmdHiderPlugin plugin,
                              ProxyServer server,
                              ProxyPermissionChecker permissionChecker,
                              Logger logger) {
        this.plugin = plugin;
        this.server = server;
        this.permissionChecker = permissionChecker;
        this.logger = logger;
    }

    @Subscribe
    public void onAvailableCommands(PlayerAvailableCommandsEvent event) {
        ProxyHiderSettings settings = plugin.getSettings();
        if (settings == null) {
            return;
        }
        Player player = event.getPlayer();
        String group = permissionChecker.getPrimaryGroup(player);

        RootCommandNode<?> root = event.getRootNode();
        root.getChildren().removeIf(node -> shouldHideNode(player, node, settings, group));
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onCommandExecute(CommandExecuteEvent event) {
        ProxyHiderSettings settings = plugin.getSettings();
        if (settings == null) {
            return;
        }
        if (!(event.getCommandSource() instanceof Player player)) {
            return;
        }

        String raw = event.getCommand();
        if (raw == null || raw.isEmpty()) {
            return;
        }

        String trimmed = raw.startsWith("/") ? raw.substring(1) : raw;
        String[] parts = trimmed.split(" ");
        if (parts.length == 0) {
            return;
        }

        String label = parts[0].toLowerCase(Locale.ROOT);
        String group = permissionChecker.getPrimaryGroup(player);

        if (settings.isAlwaysHide(label, group)) {
            if (settings.replaceNoPermission() && settings.hasNoPermissionMessage()) {
                player.sendMessage(plugin.format(settings.noPermissionMessage()));
            }
            event.setResult(CommandResult.denied());
            return;
        }

        boolean proxyCommand = server.getCommandManager().hasCommand(label);
        if (!proxyCommand) {
            // Not a Velocity command; let the backend server handle permissions/unknown command replies.
            return;
        }

        if (settings.filterByPermission() || settings.replaceNoPermission()) {
            if (!canUseCommand(player, label, settings, group)) {
                if (settings.replaceNoPermission() && settings.hasNoPermissionMessage()) {
                    player.sendMessage(plugin.format(settings.noPermissionMessage()));
                }
                event.setResult(CommandResult.denied());
            }
        }
    }

    private boolean canUseCommand(Player player, String commandLabel, ProxyHiderSettings settings, String group) {
        if (settings.isAlwaysShow(commandLabel, group)) {
            return true;
        }
        if (settings.isAlwaysHide(commandLabel, group)) {
            return false;
        }

        // Use Velocity's command manager permission predicate to respect LuckPerms requirements.
        return server.getCommandManager().hasCommand(commandLabel, player);
    }

    private boolean shouldHideNode(Player player, CommandNode<?> node, ProxyHiderSettings settings, String group) {
        String name = node.getName().toLowerCase(Locale.ROOT);
        if (settings.isAlwaysShow(name, group)) {
            return false;
        }
        if (settings.isAlwaysHide(name, group)) {
            logIfDebug(settings, "Hiding " + name + " because it is in always-hide.");
            return true;
        }
        if (settings.hideNamespaced() && name.contains(":")) {
            logIfDebug(settings, "Hiding " + name + " because it is namespaced.");
            return true;
        }
        Predicate<CommandSource> requirement = castPredicate(node.getRequirement());
        if (settings.filterByPermission() && requirement != null && !requirement.test(player)) {
            logIfDebug(settings, "Hiding " + name + " because player lacks permission predicate.");
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private Predicate<CommandSource> castPredicate(Predicate<?> predicate) {
        try {
            return (Predicate<CommandSource>) predicate;
        } catch (ClassCastException ignored) {
            return null;
        }
    }

    private void logIfDebug(ProxyHiderSettings settings, String message) {
        if (settings.debug()) {
            logger.info(message);
        }
    }
}

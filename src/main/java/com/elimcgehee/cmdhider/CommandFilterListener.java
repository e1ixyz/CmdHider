package com.elimcgehee.cmdhider;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.server.TabCompleteEvent;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

public class CommandFilterListener implements Listener {

    private final CmdHiderPlugin plugin;
    private final CommandResolver commandResolver;
    private final PermissionChecker permissionChecker;
    private final Logger logger;

    public CommandFilterListener(CmdHiderPlugin plugin,
                                 CommandResolver commandResolver,
                                 PermissionChecker permissionChecker,
                                 Logger logger) {
        this.plugin = plugin;
        this.commandResolver = commandResolver;
        this.permissionChecker = permissionChecker;
        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCommandSend(PlayerCommandSendEvent event) {
        HiderSettings settings = plugin.getSettings();
        if (settings == null) {
            return;
        }
        Player player = event.getPlayer();
        String group = permissionChecker.getPrimaryGroup(player);
        Collection<String> commands = event.getCommands();

        commands.removeIf(cmd -> shouldHideCommand(player, cmd, settings, group));
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onTabComplete(TabCompleteEvent event) {
        if (!event.isCommand()) {
            return;
        }
        HiderSettings settings = plugin.getSettings();
        if (settings == null) {
            return;
        }

        final String group = (event.getSender() instanceof Player player)
                ? permissionChecker.getPrimaryGroup(player)
                : null;

        // Hide subcommand suggestions only if the base command is already hidden.
        if (settings.hideSubcommandSuggestions() && event.getBuffer().contains(" ")) {
            if (event.getSender() instanceof Player player) {
                String base = extractBaseLabel(event.getBuffer());
                if (shouldHideCommand(player, base, settings, group)) {
                    event.setCompletions(Collections.emptyList());
                    return;
                }
            }
        }

        if (settings.hideNamespaced()) {
            List<String> filtered = event.getCompletions().stream()
                    .filter(completion -> shouldKeepCompletion(completion, settings, group))
                    .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
            event.setCompletions(filtered);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        HiderSettings settings = plugin.getSettings();
        if (settings == null) {
            return;
        }
        String message = event.getMessage();
        if (message.isEmpty() || message.charAt(0) != '/') {
            return;
        }

        String withoutSlash = message.substring(1);
        String[] args = withoutSlash.split(" ");
        if (args.length == 0) {
            return;
        }

        String label = args[0].toLowerCase(Locale.ROOT);
        Player player = event.getPlayer();
        String group = permissionChecker.getPrimaryGroup(player);

        // Block always-hide commands outright.
        if (settings.isAlwaysHide(label, group)) {
            if (settings.replaceNoPermission()) {
                player.sendMessage(settings.noPermissionMessage());
            }
            event.setCancelled(true);
            return;
        }

        Command command = commandResolver.findCommand(label).orElse(null);
        if (command == null) {
            if (settings.replaceUnknownCommand()) {
                player.sendMessage(settings.unknownCommandMessage());
                event.setCancelled(true);
            }
            return;
        }

        // Standardize permission denial messaging.
        if (settings.filterByPermission() || settings.replaceNoPermission()) {
            if (!canUseCommand(player, label, settings, group)) {
                if (settings.replaceNoPermission()) {
                    player.sendMessage(settings.noPermissionMessage());
                }
                event.setCancelled(true);
            }
        }
    }

    private boolean shouldKeepCompletion(String completion, HiderSettings settings, String group) {
        String normalized = completion.toLowerCase(Locale.ROOT);
        if (settings.isAlwaysShow(normalized, group)) {
            return true;
        }
        if (settings.isAlwaysHide(normalized, group)) {
            return false;
        }
        return !settings.hideNamespaced() || !normalized.contains(":");
    }

    private boolean shouldHideCommand(Player player, String commandLabel, HiderSettings settings, String group) {
        String normalized = commandLabel.toLowerCase(Locale.ROOT);
        if (settings.isAlwaysShow(normalized, group)) {
            return false;
        }
        if (settings.isAlwaysHide(normalized, group)) {
            logIfDebug(settings, "Hiding " + normalized + " because it is in always-hide.");
            return true;
        }

        if (settings.hideNamespaced() && normalized.contains(":")) {
            logIfDebug(settings, "Hiding " + normalized + " because it is namespaced.");
            return true;
        }

        if (settings.filterByPermission() && !canUseCommand(player, normalized, settings, group)) {
            logIfDebug(settings, "Hiding " + normalized + " because player lacks permission.");
            return true;
        }

        return false;
    }

    private boolean canUseCommand(Player player, String commandLabel, HiderSettings settings, String group) {
        if (settings.isAlwaysShow(commandLabel, group)) {
            return true;
        }
        if (settings.isAlwaysHide(commandLabel, group)) {
            return false;
        }

        // If the command exists and declares a permission, check against LuckPerms.
        Command command = commandResolver.findCommand(commandLabel).orElse(null);
        if (command != null) {
            boolean allowedByPermission = commandResolver.findPermission(commandLabel)
                    .map(permission -> permissionChecker.hasPermission(player, permission))
                    .orElse(true);
            if (!allowedByPermission) {
                return false;
            }
            if (!command.testPermissionSilent(player)) {
                return false;
            }
        }

        // Fall back to help topics; they respect per-command permissions too.
        return commandResolver.findHelpTopic(commandLabel)
                .map(topic -> topic.canSee(player))
                .orElse(true);
    }

    private String extractBaseLabel(String buffer) {
        String trimmed = buffer.startsWith("/") ? buffer.substring(1) : buffer;
        int spaceIndex = trimmed.indexOf(' ');
        if (spaceIndex >= 0) {
            trimmed = trimmed.substring(0, spaceIndex);
        }
        return trimmed;
    }

    private void logIfDebug(HiderSettings settings, String message) {
        if (settings.debug()) {
            logger.info(ChatColor.stripColor(message));
        }
    }
}

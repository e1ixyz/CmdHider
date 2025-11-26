package com.elimcgehee.cmdhider;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.help.HelpTopic;
import org.bukkit.plugin.PluginManager;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CommandResolver {

    private final Logger logger;
    private CommandMap commandMap;

    public CommandResolver(Logger logger) {
        this.logger = logger;
    }

    public Optional<Command> findCommand(String label) {
        if (label == null || label.isEmpty()) {
            return Optional.empty();
        }
        String normalized = label.toLowerCase(Locale.ROOT);
        Command direct = getCommandMap().getCommand(normalized);
        if (direct != null) {
            return Optional.of(direct);
        }
        // Try without namespace
        if (normalized.contains(":")) {
            String[] parts = normalized.split(":", 2);
            Command fallback = getCommandMap().getCommand(parts[1]);
            if (fallback != null) {
                return Optional.of(fallback);
            }
        }
        PluginCommand pluginCommand = Bukkit.getPluginCommand(normalized);
        if (pluginCommand != null) {
            return Optional.of(pluginCommand);
        }
        return Optional.empty();
    }

    public Optional<String> findPermission(String label) {
        return findCommand(label).map(Command::getPermission).filter(p -> p != null && !p.isEmpty());
    }

    public Optional<HelpTopic> findHelpTopic(String label) {
        if (label == null || label.isEmpty()) {
            return Optional.empty();
        }
        String normalized = label.toLowerCase(Locale.ROOT);
        HelpTopic topic = Bukkit.getHelpMap().getHelpTopic("/" + normalized);
        if (topic == null) {
            topic = Bukkit.getHelpMap().getHelpTopic(normalized);
        }
        if (topic == null && normalized.contains(":")) {
            String[] parts = normalized.split(":", 2);
            topic = Bukkit.getHelpMap().getHelpTopic("/" + parts[1]);
            if (topic == null) {
                topic = Bukkit.getHelpMap().getHelpTopic(parts[1]);
            }
        }
        return Optional.ofNullable(topic);
    }

    private CommandMap getCommandMap() {
        if (commandMap != null) {
            return commandMap;
        }
        PluginManager pluginManager = Bukkit.getPluginManager();
        if (pluginManager != null) {
            try {
                Field commandMapField = pluginManager.getClass().getDeclaredField("commandMap");
                commandMapField.setAccessible(true);
                CommandMap map = (CommandMap) commandMapField.get(pluginManager);
                if (map != null) {
                    commandMap = map;
                    return map;
                }
            } catch (ReflectiveOperationException ignored) {
                // fall through to logger below
            }
        }

        // Fallback to reflection on the server instance.
        try {
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap map = (CommandMap) commandMapField.get(Bukkit.getServer());
            if (map != null) {
                commandMap = map;
                return map;
            }
        } catch (ReflectiveOperationException e) {
            logger.log(Level.WARNING, "Unable to access command map; some checks may be skipped.", e);
        }

        commandMap = new SimpleCommandMap(Bukkit.getServer(), java.util.Collections.emptyMap());
        return commandMap;
    }
}

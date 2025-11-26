package com.elimcgehee.cmdhider;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class HiderSettings {

    private final boolean hideNamespaced;
    private final boolean hideSubcommandSuggestions;
    private final boolean filterByPermission;
    private final boolean replaceUnknownCommand;
    private final boolean replaceNoPermission;
    private final boolean debug;
    private final Set<String> alwaysShow;
    private final Set<String> alwaysHide;
    private final String unknownCommandMessage;
    private final String noPermissionMessage;

    public HiderSettings(boolean hideNamespaced,
                         boolean hideSubcommandSuggestions,
                         boolean filterByPermission,
                         boolean replaceUnknownCommand,
                         boolean replaceNoPermission,
                         boolean debug,
                         Set<String> alwaysShow,
                         Set<String> alwaysHide,
                         String unknownCommandMessage,
                         String noPermissionMessage) {
        this.hideNamespaced = hideNamespaced;
        this.hideSubcommandSuggestions = hideSubcommandSuggestions;
        this.filterByPermission = filterByPermission;
        this.replaceUnknownCommand = replaceUnknownCommand;
        this.replaceNoPermission = replaceNoPermission;
        this.debug = debug;
        this.alwaysShow = alwaysShow;
        this.alwaysHide = alwaysHide;
        this.unknownCommandMessage = unknownCommandMessage;
        this.noPermissionMessage = noPermissionMessage;
    }

    public static HiderSettings fromConfig(FileConfiguration config) {
        boolean hideNamespaced = config.getBoolean("options.hide-namespaced", true);
        boolean hideSubcommandSuggestions = config.getBoolean("options.hide-subcommand-suggestions", true);
        boolean filterByPermission = config.getBoolean("options.filter-by-permission", true);
        boolean replaceUnknownCommand = config.getBoolean("options.replace-unknown-command", true);
        boolean replaceNoPermission = config.getBoolean("options.replace-no-permission", true);
        boolean debug = config.getBoolean("options.debug", false);

        Set<String> alwaysShow = toLowerSet(config.getStringList("exceptions.always-show"));
        Set<String> alwaysHide = toLowerSet(config.getStringList("exceptions.always-hide"));

        String unknown = colorize(config.getString("messages.unknown-command", "&cThis command does not exist."));
        String noPerm = colorize(config.getString("messages.no-permission", "&cYou don't have permission."));

        return new HiderSettings(
                hideNamespaced,
                hideSubcommandSuggestions,
                filterByPermission,
                replaceUnknownCommand,
                replaceNoPermission,
                debug,
                alwaysShow,
                alwaysHide,
                unknown,
                noPerm
        );
    }

    private static Set<String> toLowerSet(Iterable<String> raw) {
        Set<String> set = new HashSet<>();
        for (String value : raw) {
            String normalized = value == null ? "" : value;
            if (normalized.startsWith("/")) {
                normalized = normalized.substring(1);
            }
            set.add(normalized.toLowerCase(Locale.ROOT));
        }
        return Collections.unmodifiableSet(set);
    }

    private static String colorize(String raw) {
        return ChatColor.translateAlternateColorCodes('&', raw == null ? "" : raw);
    }

    public boolean hideNamespaced() {
        return hideNamespaced;
    }

    public boolean hideSubcommandSuggestions() {
        return hideSubcommandSuggestions;
    }

    public boolean filterByPermission() {
        return filterByPermission;
    }

    public boolean replaceUnknownCommand() {
        return replaceUnknownCommand;
    }

    public boolean replaceNoPermission() {
        return replaceNoPermission;
    }

    public boolean debug() {
        return debug;
    }

    public Set<String> alwaysShow() {
        return alwaysShow;
    }

    public Set<String> alwaysHide() {
        return alwaysHide;
    }

    public String unknownCommandMessage() {
        return unknownCommandMessage;
    }

    public String noPermissionMessage() {
        return noPermissionMessage;
    }

    public boolean isAlwaysShow(String commandLabel) {
        String normalized = normalize(commandLabel);
        if (alwaysShow.contains(normalized)) {
            return true;
        }
        return checkBase(normalized, alwaysShow);
    }

    public boolean isAlwaysHide(String commandLabel) {
        String normalized = normalize(commandLabel);
        if (alwaysHide.contains(normalized)) {
            return true;
        }
        return checkBase(normalized, alwaysHide);
    }

    private String normalize(String command) {
        if (command == null) {
            return "";
        }
        String normalized = command.startsWith("/") ? command.substring(1) : command;
        return normalized.toLowerCase(Locale.ROOT);
    }

    private boolean checkBase(String normalized, Set<String> set) {
        int colonIndex = normalized.indexOf(':');
        if (colonIndex >= 0) {
            String base = normalized.substring(colonIndex + 1);
            return set.contains(base);
        }
        return false;
    }
}

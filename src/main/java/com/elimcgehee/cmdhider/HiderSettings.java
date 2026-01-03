package com.elimcgehee.cmdhider;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
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
    private final Map<String, Set<String>> groupAlwaysShow;
    private final Map<String, Set<String>> groupAlwaysHide;
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
                         Map<String, Set<String>> groupAlwaysShow,
                         Map<String, Set<String>> groupAlwaysHide,
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
        this.groupAlwaysShow = groupAlwaysShow;
        this.groupAlwaysHide = groupAlwaysHide;
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
        Map<String, Set<String>> groupAlwaysShow = readGroupLists(config, "exceptions.per-group", "always-show");
        Map<String, Set<String>> groupAlwaysHide = readGroupLists(config, "exceptions.per-group", "always-hide");

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
                groupAlwaysShow,
                groupAlwaysHide,
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

    public Map<String, Set<String>> groupAlwaysShow() {
        return groupAlwaysShow;
    }

    public Map<String, Set<String>> groupAlwaysHide() {
        return groupAlwaysHide;
    }

    public String unknownCommandMessage() {
        return unknownCommandMessage;
    }

    public String noPermissionMessage() {
        return noPermissionMessage;
    }

    public boolean hasUnknownCommandMessage() {
        return !unknownCommandMessage.isBlank();
    }

    public boolean hasNoPermissionMessage() {
        return !noPermissionMessage.isBlank();
    }

    public boolean isAlwaysShow(String commandLabel) {
        return isAlwaysShow(commandLabel, null);
    }

    public boolean isAlwaysShow(String commandLabel, String group) {
        String normalized = normalize(commandLabel);
        Set<String> groupSet = groupAlwaysShow.get(normalizeGroup(group));
        if (matches(normalized, groupSet)) {
            return true;
        }
        return matches(normalized, alwaysShow);
    }

    public boolean isAlwaysHide(String commandLabel) {
        return isAlwaysHide(commandLabel, null);
    }

    public boolean isAlwaysHide(String commandLabel, String group) {
        String normalized = normalize(commandLabel);
        Set<String> groupSet = groupAlwaysHide.get(normalizeGroup(group));
        if (matches(normalized, groupSet)) {
            return true;
        }
        return matches(normalized, alwaysHide);
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

    private boolean matches(String normalized, Set<String> set) {
        if (set == null || set.isEmpty()) {
            return false;
        }
        if (set.contains(normalized)) {
            return true;
        }
        return checkBase(normalized, set);
    }

    private static Map<String, Set<String>> readGroupLists(FileConfiguration config, String basePath, String listName) {
        Map<String, Set<String>> map = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection(basePath);
        if (section == null) {
            return Collections.emptyMap();
        }
        for (String key : section.getKeys(false)) {
            String path = basePath + "." + key + "." + listName;
            Set<String> values = toLowerSet(config.getStringList(path));
            map.put(normalizeGroup(key), values);
        }
        return Collections.unmodifiableMap(map);
    }

    private static String normalizeGroup(String group) {
        if (group == null || group.isEmpty()) {
            return "default";
        }
        return group.toLowerCase(Locale.ROOT);
    }
}

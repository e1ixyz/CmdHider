package com.elimcgehee.cmdhider.proxy;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class ProxyHiderSettings {

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

    public ProxyHiderSettings(boolean hideNamespaced,
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

    public static ProxyHiderSettings load(Path path, Logger logger) {
        CommentedFileConfig config = CommentedFileConfig.builder(path).autosave().build();
        config.load();

        boolean hideNamespaced = config.getOrElse("options.hide-namespaced", true);
        boolean hideSubcommandSuggestions = config.getOrElse("options.hide-subcommand-suggestions", true);
        boolean filterByPermission = config.getOrElse("options.filter-by-permission", true);
        boolean replaceUnknownCommand = config.getOrElse("options.replace-unknown-command", true);
        boolean replaceNoPermission = config.getOrElse("options.replace-no-permission", true);
        boolean debug = config.getOrElse("options.debug", false);

        Set<String> alwaysShow = toLowerSet(config.getOrElse("exceptions.always-show", Collections.emptyList()));
        Set<String> alwaysHide = toLowerSet(config.getOrElse("exceptions.always-hide", Collections.emptyList()));
        Map<String, Set<String>> groupAlwaysShow = readGroupLists(config, "exceptions.per-group", "always-show");
        Map<String, Set<String>> groupAlwaysHide = readGroupLists(config, "exceptions.per-group", "always-hide");

        String unknown = config.getOrElse("messages.unknown-command", "This command does not exist.");
        String noPerm = config.getOrElse("messages.no-permission", "You don't have permission.");

        return new ProxyHiderSettings(
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

    public String unknownCommandMessage() {
        return unknownCommandMessage;
    }

    public String noPermissionMessage() {
        return noPermissionMessage;
    }

    public boolean isAlwaysShow(String commandLabel, String group) {
        String normalized = normalize(commandLabel);
        Set<String> groupSet = groupAlwaysShow.get(normalizeGroup(group));
        if (matches(normalized, groupSet)) {
            return true;
        }
        return matches(normalized, alwaysShow);
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

    private static Map<String, Set<String>> readGroupLists(Config config, String basePath, String listName) {
        Map<String, Set<String>> map = new HashMap<>();
        Object maybeSection = config.get(basePath);
        if (!(maybeSection instanceof Config section)) {
            return Collections.emptyMap();
        }
        for (String key : section.valueMap().keySet()) {
            String path = basePath + "." + key + "." + listName;
            Set<String> values = toLowerSet(config.getOrElse(path, Collections.emptyList()));
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

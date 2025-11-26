package com.elimcgehee.cmdhider;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.logging.Level;

public class CmdHiderPlugin extends JavaPlugin {

    private HiderSettings settings;
    private LuckPerms luckPerms;
    private CommandResolver commandResolver;
    private PermissionChecker permissionChecker;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadSettings();
        hookLuckPerms();

        this.commandResolver = new CommandResolver(getLogger());
        this.permissionChecker = new PermissionChecker(luckPerms);

        getServer().getPluginManager().registerEvents(
                new CommandFilterListener(this, commandResolver, permissionChecker, getLogger()), this
        );

        Optional.ofNullable(getCommand("cmdhider")).ifPresent(cmd -> cmd.setExecutor(this::onCommand));

        getLogger().info("CmdHider enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("CmdHider disabled.");
    }

    public HiderSettings getSettings() {
        return settings;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || !"reload".equalsIgnoreCase(args[0])) {
            sender.sendMessage("Usage: /cmdhider reload");
            return true;
        }

        if (!sender.hasPermission("cmdhider.admin")) {
            sender.sendMessage(getSettings().noPermissionMessage());
            return true;
        }

        reloadSettings();
        sender.sendMessage("CmdHider configuration reloaded.");
        return true;
    }

    private void reloadSettings() {
        reloadConfig();
        this.settings = HiderSettings.fromConfig(getConfig());
    }

    private void hookLuckPerms() {
        try {
            this.luckPerms = LuckPermsProvider.get();
            getLogger().info("Hooked into LuckPerms for permission checks.");
        } catch (IllegalStateException ex) {
            this.luckPerms = null;
            getLogger().log(Level.WARNING, "LuckPerms not found; falling back to Bukkit permissions.");
        }
    }
}

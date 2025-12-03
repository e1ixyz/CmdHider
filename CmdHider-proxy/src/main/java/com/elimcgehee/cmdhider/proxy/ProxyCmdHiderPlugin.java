package com.elimcgehee.cmdhider.proxy;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

@Plugin(id = "cmdhider", name = "CmdHider", version = "1.0.0", authors = {"Eli"},
        dependencies = { @Dependency(id = "luckperms", optional = false) })
public class ProxyCmdHiderPlugin {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private ProxyHiderSettings settings;
    private ProxyPermissionChecker permissionChecker;
    private LuckPerms luckPerms;

    @Inject
    public ProxyCmdHiderPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onInitialize(ProxyInitializeEvent event) {
        ensureConfig();
        hookLuckPerms();
        reloadSettings();
        this.permissionChecker = new ProxyPermissionChecker(luckPerms);

        server.getEventManager().register(this, new ProxyCommandFilter(this, server, permissionChecker, logger));

        logger.info("CmdHider Proxy enabled.");
    }

    public ProxyHiderSettings getSettings() {
        return settings;
    }

    public void reloadSettings() {
        try {
            this.settings = ProxyHiderSettings.load(dataDirectory.resolve("config.toml"), logger);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Failed to load config.toml", ex);
        }
    }

    public net.kyori.adventure.text.Component format(String message) {
        return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(message);
    }

    private void ensureConfig() {
        try {
            Files.createDirectories(dataDirectory);
            Path configPath = dataDirectory.resolve("config.toml");
            if (Files.notExists(configPath)) {
                try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.toml")) {
                    if (in != null) {
                        Files.copy(in, configPath);
                    }
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to create config.toml", e);
        }
    }

    private void hookLuckPerms() {
        try {
            this.luckPerms = LuckPermsProvider.get();
            logger.info("Hooked into LuckPerms.");
        } catch (IllegalStateException ex) {
            logger.log(Level.SEVERE, "LuckPerms not found; permission checks may be inaccurate.", ex);
        }
    }
}

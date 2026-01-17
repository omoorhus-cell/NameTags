package org.tekkabyte.nameTags;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.tekkabyte.nameTags.listeners.NametagListener;
import org.tekkabyte.nameTags.managers.NametagManager;

import java.util.logging.Logger;

public class NameTags extends JavaPlugin {

    private NametagManager nametagManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.nametagManager = new NametagManager(this);

        Bukkit.getPluginManager().registerEvents(new NametagListener(this, nametagManager), this);

        for (Player p : Bukkit.getOnlinePlayers()) {
            nametagManager.ensureTag(p);
        }

        long refreshTicks = Math.max(5L, getConfig().getLong("settings.text-refresh-ticks", 20L));
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                nametagManager.refreshText(p);
            }
        }, 1L, refreshTicks);

        reg("nametags", new ReloadTagsCommand(this));

        getLogger().info("BetterNametags enabled.");
    }

    @Override
    public void onDisable() {
        if (nametagManager != null) nametagManager.clearAll();
        getLogger().info("BetterNametags disabled.");
    }

    private void reg(String name, CommandExecutor exec) {
        PluginCommand cmd = getCommand(name);
        if (cmd == null) {
            Logger logger = getLogger();
            logger.severe("Command not found in plugin.yml: " + name);
            return;
        }
        cmd.setExecutor(exec);
    }

    public NametagManager getNametagManager() {
        return nametagManager;
    }
}
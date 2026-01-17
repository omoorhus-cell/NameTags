package org.tekkabyte.nameTags;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.tekkabyte.nameTags.managers.NametagManager;

public class ReloadTagsCommand implements CommandExecutor {

    private final NameTags plugin;

    public ReloadTagsCommand(NameTags plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("betternametags.admin")) {
            sender.sendMessage("§cNo permission.");
            return true;
        }

        if (args.length == 0 || !args[0].equalsIgnoreCase("rebuild")) {
            sender.sendMessage("§eUsage: §f/nametags rebuild");
            return true;
        }

        NametagManager manager = plugin.getNametagManager();
        if (manager == null) {
            sender.sendMessage("§cNametag manager not initialized.");
            return true;
        }

        manager.clearAll();
        for (Player p : Bukkit.getOnlinePlayers()) {
            manager.ensureTag(p);
        }

        sender.sendMessage("§aRebuilt nametags for §f" + Bukkit.getOnlinePlayers().size() + "§a online players.");
        return true;
    }
}
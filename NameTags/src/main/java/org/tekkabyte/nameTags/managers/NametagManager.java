package org.tekkabyte.nameTags.managers;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;
import org.tekkabyte.nameTags.NameTags;

import java.lang.reflect.Method;
import java.util.*;

public class NametagManager {

    private final NameTags plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    private final NamespacedKey KEY_NAMETAG;
    private final NamespacedKey KEY_OWNER;

    private final Map<UUID, TagPair> tags = new HashMap<>();

    public NametagManager(NameTags plugin) {
        this.plugin = plugin;
        this.KEY_NAMETAG = new NamespacedKey(plugin, "is_nametag");
        this.KEY_OWNER = new NamespacedKey(plugin, "owner_uuid");
    }

    public boolean hasTag(Player player) {
        return player != null && tags.containsKey(player.getUniqueId());
    }

    public void ensureTag(Player player) {
        if (player == null || !player.isOnline()) return;

        if (player.getGameMode() == GameMode.SPECTATOR) {
            deleteTag(player);
            return;
        }
        if (player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
            deleteTag(player);
            return;
        }

        TagPair pair = tags.get(player.getUniqueId());
        if (pair != null && pair.isValid()) {

            if (pair.info.getWorld() != player.getWorld() || pair.name.getWorld() != player.getWorld()) {
                deleteTag(player);
            } else {
                try {
                    if (pair.info.getVehicle() != player) {
                        player.addPassenger(pair.info);
                    }
                } catch (Throwable ignored) {}

                try {
                    if (pair.name.getVehicle() != pair.info) {
                        pair.info.addPassenger(pair.name);
                    }
                } catch (Throwable ignored) {}

                return;
            }
        }

        deleteTag(player);

        Location loc = player.getLocation();
        if (loc.getWorld() == null) return;

        TextDisplay info = loc.getWorld().spawn(loc, TextDisplay.class, td -> {
            td.setBillboard(Display.Billboard.CENTER);
            td.setSeeThrough(false);
            td.setShadowed(true);
            td.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            td.setAlignment(TextDisplay.TextAlignment.CENTER);
            td.setDefaultBackground(false);
            td.setLineWidth(200);
            td.setPersistent(false);
            td.setInvulnerable(true);
            td.setGravity(false);

            PersistentDataContainer pdc = td.getPersistentDataContainer();
            pdc.set(KEY_NAMETAG, PersistentDataType.BYTE, (byte) 1);
            pdc.set(KEY_OWNER, PersistentDataType.STRING, player.getUniqueId().toString());

            setDisplayTranslation(td, 0f, 0.25f, 0f);
        });

        TextDisplay name = loc.getWorld().spawn(loc, TextDisplay.class, td -> {
            td.setBillboard(Display.Billboard.CENTER);
            td.setSeeThrough(false);
            td.setShadowed(true);
            td.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            td.setAlignment(TextDisplay.TextAlignment.CENTER);
            td.setDefaultBackground(false);
            td.setLineWidth(200);
            td.setPersistent(false);
            td.setInvulnerable(true);
            td.setGravity(false);

            PersistentDataContainer pdc = td.getPersistentDataContainer();
            pdc.set(KEY_NAMETAG, PersistentDataType.BYTE, (byte) 1);
            pdc.set(KEY_OWNER, PersistentDataType.STRING, player.getUniqueId().toString());

            setDisplayTranslation(td, 0f, 0.55f, 0f);
        });

        player.addPassenger(info);
        info.addPassenger(name);

        tags.put(player.getUniqueId(), new TagPair(info, name));
        refreshText(player);
    }

    public void refreshText(Player player) {
        if (player == null) return;

        TagPair pair = tags.get(player.getUniqueId());
        if (pair == null || !pair.isValid()) return;

        if (pair.info.getWorld() != player.getWorld() || pair.name.getWorld() != player.getWorld()) {
            deleteTag(player);
            return;
        }

        int hp = (int) Math.ceil(player.getHealth());
        int ping = player.getPing();
        String pingColor = getPingColor(ping);

        String nameColor = "<#aaaaaa>";

        if (hasPAPI()) {
            String prefix = PlaceholderAPI.setPlaceholders(player, "%luckperms_prefix%");
            if (prefix != null && !prefix.isBlank()) {
                String hex = lastHex(prefix);
                if (hex != null) nameColor = "<#" + hex + ">";
            }
        }

        Component infoComp = mm.deserialize(
                "<#E64B4B>" + hp + "❤" + pingColor + " <dark_gray><bold>|</bold></dark_gray> " + ping + "ms" + "📶 ");

        String teamsTag = getTeamsTag(player);
        String teamPart = "";
        if (teamsTag != null && !teamsTag.isBlank()) {
            teamPart = "<dark_gray>[</dark_gray><gray>" + toMiniSafe(teamsTag) + "</gray><dark_gray>]</dark_gray> ";
        }

        Component nameComp = mm.deserialize(teamPart + nameColor + toMiniSafe(player.getName()));

        pair.info.text(infoComp);
        pair.name.text(nameComp);
    }

    public void deleteTag(Player player) {
        if (player == null) return;

        TagPair pair = tags.remove(player.getUniqueId());
        if (pair == null) return;

        try {
            if (pair.name != null && pair.name.isValid()) pair.name.remove();
        } catch (Throwable ignored) {}

        try {
            if (pair.info != null && pair.info.isValid()) pair.info.remove();
        } catch (Throwable ignored) {}
    }

    public void clearAll() {
        List<UUID> keys = new ArrayList<>(tags.keySet());
        for (UUID id : keys) {
            TagPair pair = tags.remove(id);
            if (pair == null) continue;

            try {
                if (pair.name != null && pair.name.isValid()) pair.name.remove();
            } catch (Throwable ignored) {}

            try {
                if (pair.info != null && pair.info.isValid()) pair.info.remove();
            } catch (Throwable ignored) {}
        }
    }

    private String getTeamsTag(Player player) {
        try {
            Plugin teamsPlugin = plugin.getServer().getPluginManager().getPlugin("Teams");
            if (teamsPlugin == null || !teamsPlugin.isEnabled()) return null;

            Method getTeamManager = teamsPlugin.getClass().getMethod("getTeamManager");
            Object teamManager = getTeamManager.invoke(teamsPlugin);
            if (teamManager == null) return null;

            Method getPlayerTeamIdSync = teamManager.getClass().getMethod("getPlayerTeamIdSync", UUID.class);
            Object teamIdObj = getPlayerTeamIdSync.invoke(teamManager, player.getUniqueId());
            if (!(teamIdObj instanceof UUID teamId)) return null;

            Method getTeamByIdSync = teamManager.getClass().getMethod("getTeamByIdSync", UUID.class);
            Object teamObj = getTeamByIdSync.invoke(teamManager, teamId);
            if (teamObj == null) return null;

            Method getTag = teamObj.getClass().getMethod("getTag");
            Object tagObj = getTag.invoke(teamObj);
            return tagObj == null ? null : String.valueOf(tagObj);

        } catch (Throwable ignored) {
            return null;
        }
    }

    private boolean hasPAPI() {
        Plugin papi = plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI");
        return papi != null && papi.isEnabled();
    }

    private String getPingColor(int ping) {
        if (ping <= 50) return "<#98F421>";
        if (ping <= 100) return "<#C6E921>";
        if (ping <= 150) return "<#F4DE21>";
        if (ping <= 200) return "<#F4A821>";
        if (ping <= 250) return "<#F47321>";
        return "<#F43D21>";
    }

    private String toMiniSafe(String s) {
        if (s == null) return "";
        return s.replace("<", "\\<").replace(">", "\\>");
    }

    private String lastHex(String s) {
        if (s == null) return null;
        for (int i = s.length() - 1; i >= 0; i--) {
            if (s.charAt(i) == '#') {
                if (i + 6 < s.length()) {
                    String hex = s.substring(i + 1, i + 7);
                    if (hex.matches("[0-9a-fA-F]{6}")) return hex;
                }
            }
        }
        return null;
    }

    private static void setDisplayTranslation(TextDisplay display, float x, float y, float z) {
        Transformation t = display.getTransformation();
        display.setTransformation(new Transformation(
                new Vector3f(x, y, z),
                t.getLeftRotation(),
                t.getScale(),
                t.getRightRotation()
        ));
    }

    public static final class TagPair {
        public final TextDisplay info;
        public final TextDisplay name;

        public TagPair(TextDisplay info, TextDisplay name) {
            this.info = info;
            this.name = name;
        }

        public boolean isValid() {
            return info != null && name != null && info.isValid() && name.isValid();
        }
    }
}
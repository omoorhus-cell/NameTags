package org.tekkabyte.nameTags.listeners;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.player.*;
import org.bukkit.potion.PotionEffectType;
import org.tekkabyte.nameTags.NameTags;
import org.tekkabyte.nameTags.managers.NametagManager;
import org.tekkabyte.nameTags.utils.TeleportUtil;

public class NametagListener implements Listener {

    private final NameTags plugin;
    private final NametagManager nametags;

    private final ThreadLocal<Boolean> internalTeleport = ThreadLocal.withInitial(() -> Boolean.FALSE);

    public NametagListener(NameTags plugin, NametagManager nametags) {
        this.plugin = plugin;
        this.nametags = nametags;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Bukkit.getScheduler().runTask(plugin, () -> nametags.ensureTag(e.getPlayer()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        nametags.deleteTag(e.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Bukkit.getScheduler().runTask(plugin, () -> nametags.ensureTag(e.getPlayer()));
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        Bukkit.getScheduler().runTask(plugin, () -> nametags.ensureTag(e.getPlayer()));
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        if (Boolean.TRUE.equals(internalTeleport.get())) return;

        Player p = e.getPlayer();
        Location to = e.getTo();
        if (to == null || to.getWorld() == null) return;

        if (p.getGameMode() == GameMode.SPECTATOR || p.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
            nametags.deleteTag(p);
            return;
        }

        e.setCancelled(true);

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                internalTeleport.set(Boolean.TRUE);

                nametags.deleteTag(p);

                TeleportUtil.teleportRetainingRideStack(plugin, p, to);

            } finally {
                internalTeleport.set(Boolean.FALSE);
            }

            Bukkit.getScheduler().runTask(plugin, () -> nametags.ensureTag(p));
        });
    }

    @EventHandler
    public void onGamemode(PlayerGameModeChangeEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (e.getNewGameMode() == GameMode.SPECTATOR) nametags.deleteTag(p);
            else nametags.ensureTag(p);
        });
    }

    @EventHandler
    public void onDismount(EntityDismountEvent e) {
        Entity ent = e.getEntity();
        if (!(ent instanceof Player p)) return;
        Bukkit.getScheduler().runTask(plugin, () -> nametags.ensureTag(p));
    }

    @EventHandler
    public void onPotion(EntityPotionEffectEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (e.getModifiedType() != PotionEffectType.INVISIBILITY) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (p.hasPotionEffect(PotionEffectType.INVISIBILITY)) nametags.deleteTag(p);
            else nametags.ensureTag(p);
        });
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        Bukkit.getScheduler().runTask(plugin, () -> nametags.refreshText(p));
    }

    @EventHandler
    public void onHeal(EntityRegainHealthEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        Bukkit.getScheduler().runTask(plugin, () -> nametags.refreshText(p));
    }
}
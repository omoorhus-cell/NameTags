package org.tekkabyte.nameTags.utils;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class TeleportUtil {

    private TeleportUtil() {}

    public static void teleportRetainingRideStack(JavaPlugin plugin, Player player, Location to) {
        if (plugin == null || player == null || to == null || to.getWorld() == null) return;

        Entity top = getTopVehicle(player);
        RideNode root = captureRideTree(top);

        detachRideTree(root);

        top.teleport(to);

        Location base = top.getLocation();
        teleportChildrenTo(root, base);

        Server server = plugin.getServer();
        server.getScheduler().runTask(plugin, () -> reattachRideTree(root));
    }

    private static void teleportChildrenTo(RideNode node, Location base) {
        for (RideNode child : node.passengers) {
            try {
                if (child.entity != null && child.entity.isValid()) {
                    child.entity.teleport(base);
                }
            } catch (Throwable ignored) {}
            teleportChildrenTo(child, base);
        }
    }

    private static Entity getTopVehicle(Entity entity) {
        Entity cur = entity;
        while (cur.getVehicle() != null) {
            cur = cur.getVehicle();
        }
        return cur;
    }

    private static RideNode captureRideTree(Entity entity) {
        RideNode node = new RideNode(entity);
        for (Entity passenger : new ArrayList<>(entity.getPassengers())) {
            node.passengers.add(captureRideTree(passenger));
        }
        return node;
    }

    private static void detachRideTree(RideNode node) {
        for (RideNode child : node.passengers) detachRideTree(child);
        try {
            node.entity.eject();
        } catch (Throwable ignored) {}
    }

    private static void reattachRideTree(RideNode node) {
        for (RideNode child : node.passengers) {
            if (!node.entity.isValid()) continue;
            if (!child.entity.isValid()) continue;

            if (node.entity.getWorld() != child.entity.getWorld()) continue;

            try {
                node.entity.addPassenger(child.entity);
            } catch (Throwable ignored) {}

            reattachRideTree(child);
        }
    }

    static final class RideNode {
        private final Entity entity;
        private final List<RideNode> passengers = new ArrayList<>();

        private RideNode(Entity entity) {
            this.entity = entity;
        }
    }
}
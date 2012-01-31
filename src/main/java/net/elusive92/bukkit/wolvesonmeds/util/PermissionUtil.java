package net.elusive92.bukkit.wolvesonmeds.util;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;

public class PermissionUtil {

    /**
     * Tests whether the owner of a wolf has a permission.
     *
     * @param tameable
     * @param name
     * @return whether the owner has permission
     */
    public static boolean hasPermission(Tameable tameable, String name) {
        return hasPermission(tameable.getOwner(), name);
    }

    /**
     * Tests whether an animal tamer has a permission.
     *
     * @param tamer
     * @param name
     * @return whether he has permission
     */
    public static boolean hasPermission(AnimalTamer tamer, String name) {
        // Try to find a valid player.
        Player owner = null;

        if (tamer instanceof Player) {
            owner = (Player) tamer;
        } else if (tamer instanceof OfflinePlayer) {
            owner = ((OfflinePlayer) tamer).getPlayer();
        }

        // Only online players have permissions.
        if (owner == null) return false;
        return hasPermission(owner, name);
    }

    /**
     * Tests whether a player has a permission.
     *
     * @param player
     * @param name
     * @return whether he has permission
     */
    public static boolean hasPermission(Player player, String name) {
        return player.hasPermission("wolvesonmeds." + name);
    }
}

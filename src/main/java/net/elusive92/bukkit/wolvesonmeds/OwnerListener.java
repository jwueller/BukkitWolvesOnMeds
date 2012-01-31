package net.elusive92.bukkit.wolvesonmeds;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Listens for players joining the server.
 *
 * @author johannes
 */
public class OwnerListener implements Listener {

    private final WolvesOnMeds plugin; // reference to the main plugin class

    public OwnerListener(WolvesOnMeds plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.dispatchAll();
    }
}

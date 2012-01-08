package net.elusive92.bukkit.wolvesonmeds;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Wolf;
import org.bukkit.event.entity.*;

/**
 * Listens for all wolves that might be tamed or spawned.
 */
public class WolfListener extends EntityListener {

    private final WolvesOnMeds plugin; // reference to the main plugin class

    /**
     * Creates an entity listener.
     *
     * @param plugin main plugin class
     */
    public WolfListener(WolvesOnMeds plugin) {
        this.plugin = plugin;
    }

    /**
     * Notifies the plugin when wolves are spawned.
     *
     * @param event
     */
    @Override
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.isCancelled()) return;
        Entity entity = event.getEntity();
        if (entity instanceof Wolf) plugin.dispatch((Wolf) entity);
    }

    /**
     * Notifies the plugin when wolves are tamed.
     *
     * @param event
     */
    @Override
    public void onEntityTame(EntityTameEvent event) {
        if (event.isCancelled()) return;
        Entity entity = event.getEntity();
        if (entity instanceof Wolf) plugin.dispatch((Wolf) entity);
    }

    /**
     * Notifies a plugin if a wolf gets wounded.
     *
     * @param event
     */
    @Override
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.isCancelled()) return;
        Entity entity = event.getEntity();

        if (entity instanceof Wolf) {
            Wolf wolf = (Wolf) entity;

            // We need to override the health manually, since the damage is not
            // yet included in the health property of the wolf.
            plugin.dispatch(wolf, wolf.getHealth() - event.getDamage());
            plugin.resetDelay(wolf);
        }
    }

    /**
     * Removes dead wolves from the managed list.
     *
     * @param event
     */
    @Override
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();

        if (entity instanceof Wolf) {
            plugin.dispatch((Wolf) entity);
        }
    }
}

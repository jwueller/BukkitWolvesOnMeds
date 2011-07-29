package net.elusive92.bukkit.wolvesonmeds;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Wolf;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.entity.EntityTameEvent;

/**
 * Listens for all wolves that might be tamed or spawned.
 */
public class WOMEntityListener extends EntityListener {

    /**
     * Stores a reference to the main plugin class.
     */
    private final WolvesOnMeds plugin;

    /**
     * Creates an entity listener.
     * 
     * @param plugin main plugin class
     */
    public WOMEntityListener(WolvesOnMeds plugin) {
        this.plugin = plugin;
    }

    /**
     * Notifies the plugin when wolves are spawned.
     * 
     * @param event 
     */
    @Override
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.isCancelled()) {
            return;
        }
        
        Entity entity = event.getEntity();
        
        if (entity instanceof Wolf) {
            plugin.manage((Wolf) entity);
        }
    }
    
    /**
     * Notifies the plugin when wolves are tamed.
     * 
     * @param event 
     */
    @Override
    public void onEntityTame(EntityTameEvent event) {
        if (event.isCancelled()) {
            return;
        }
        
        Entity entity = event.getEntity();
        
        if (entity instanceof Wolf) {
            plugin.manage((Wolf) entity);
        }
    }

    /**
     * Notifies a plugin if a wolf gets wounded.
     * 
     * @param event 
     */
    @Override
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.isCancelled()) {
            return;
        }
        
        Entity entity = event.getEntity();
        
        if (entity instanceof Wolf) {
            Wolf wolf = (Wolf) entity;
            
            // We need to override the health manually, since the damage is not
            // yet included in the health property of the wolf.
            plugin.manage(wolf, wolf.getHealth() - event.getDamage());
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
        
        // Ensure that dead wolves are removed from the managed list.
        if (entity instanceof Wolf) {
            plugin.getRecoveringWolves().remove((Wolf) entity);
        }
    }
}

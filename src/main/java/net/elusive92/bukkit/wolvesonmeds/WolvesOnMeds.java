package net.elusive92.bukkit.wolvesonmeds;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Wolf;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class WolvesOnMeds extends JavaPlugin {
    
    /**
     * Stores the configuration handler.
     */
    private WOMConfiguration config = new WOMConfiguration(this);
    
    /**
     * Stores the entity listener.
     */
    private WOMEntityListener entityListener = new WOMEntityListener(this);
    
    /**
     * Caches all tamed wolves on the server to allow for very fast access when
     * being used by the scheduler. The set needs to be synchronized to ensure
     * that nothing is modified while the scheduler is using it.
     */
    private Set<Wolf> recoveringWolves = Collections.synchronizedSet(new HashSet<Wolf>());
    
    /**
     * Stores the time (in ticks) until a wolf starts to recover. Attacking a
     * wolf will reset the delay to the configured value to avoid instant
     * healing after combat.
     */
    private Map<Wolf, Long> recoveryDelays = Collections.synchronizedMap(new HashMap<Wolf, Long>());

    /**
     * Stores the maximum health a wolf can be healed to.
     */
    private static int maxHealth;
    
    /**
     * Stores the minimum health a wolf needs for automatic recovery.
     */
    private static int minHealth;
    
    /**
     * Stores the amount of ticks required to heal a wolf from 0-100% health.
     */
    private long recoveryDurationTicks;
    
    /**
     * Stores the amount of ticks to wait before starting to heal a wolf after
     * combat.
     */
    private long recoveryDelayTicks;
    
    /**
     * Stores the interval (in ticks) at which the wolves get healed.
     */
    private long recoveryIntervalTicks;

    /**
     * Initializes the plugin.
     */
    public void onEnable() {
        // Load the configuration every time the plugin is enabled. This needs
        // to be done first to prevent missing properties from eating our souls.
        config.load();
        
        // Determine the minimum and maximum health. A health value needs to be
        // in the range from 1 to 20.
        maxHealth = getHealthUnits(config.getInt("heal.max-health"));
        minHealth = getHealthUnits(config.getInt("heal.min-health"));
        debug("max health: " + maxHealth + "; min health: " + minHealth);

        // Convert seconds to ticks.
        recoveryDurationTicks = Math.max(config.getInt("heal.duration"), 1) * 20;
        recoveryDelayTicks = config.getInt("heal.delay") * 20;
        
        // Calculate the interval at which the wolves should be healed.
        recoveryIntervalTicks = recoveryDurationTicks / maxHealth;

        // Register event listeners.
        registerEvent(Type.CREATURE_SPAWN, entityListener);
        registerEvent(Type.ENTITY_DAMAGE, entityListener);
        registerEvent(Type.ENTITY_DEATH, entityListener);
        registerEvent(Type.ENTITY_TAME, entityListener);

        // Find all wounded tamed wolves that are currently on the server.
        for (World world : getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Wolf) {
                    dispatch((Wolf) entity, null);
                }
            }
        }

        // Try to schedule our healing task.
        if (getServer().getScheduler().scheduleSyncRepeatingTask(this, new WOMHealTask(this), 0L, recoveryIntervalTicks) == -1) {
            log("Failed to schedule wolf healing task.");
        }

        System.out.println(this + " is now enabled!");
    }

    /**
     * Shuts down the plugin.
     */
    public void onDisable() {
        System.out.println(this + " is now disabled!");
    }
    
    /**
     * Decides what to do with the wolf. If he can recover, he is either healed
     * instantly or queued for timed healing.
     * 
     * @param wolf
     * @param health explicit health value to check against to determine if the
     *        wolf is wounded (it is automatically inserted if it is null)
     */
    /*package*/ void dispatch(Wolf wolf, Integer health) {
        // Insert the current health of the entity if no explicit health is
        // specified.
        if (health == null) {
            health = wolf.getHealth();
        }
        
        // Tamed wounded wolves get added to the set of recovering wolves if
        // their maximum recovery health is not reached.
        if (wolf.isTamed() && health < maxHealth && health >= minHealth) {
            recoveringWolves.add(wolf);
            debug("Wolf " + wolf + " is recovering.");
        } else {
            // Remove all references to wolves that have recovered successfully.
            recoveringWolves.remove(wolf);
            recoveryDelays.remove(wolf);
        }
    }
    
    /**
     * Resets the recovery delay for a wolf.
     * 
     * @param wolf 
     */
    /*package*/ void resetDelay(Wolf wolf) {
        if (recoveryDelayTicks > 0 && recoveringWolves.contains(wolf)) {
            recoveryDelays.put(wolf, recoveryDelayTicks);
        }
    }
    
    /**
     * Actually heals the wounded wolves. This method is thread safe.
     */
    /*package*/ void heal() {
        // Do not do anything if there are no wolves to be healed.
        if (recoveringWolves.isEmpty()) {
            return;
        }

        // We need to use an iterator to allow for removing elements while
        // iterating.
        Iterator<Wolf> itr = recoveringWolves.iterator();

        while (itr.hasNext()) {
            Wolf wolf = itr.next();
            
            // Process the recovery delay.
            if (recoveryDelayTicks > 0 && recoveryDelays.containsKey(wolf)) {
                long remainingDelay = recoveryDelays.get(wolf) - recoveryIntervalTicks;
                
                // Decrease the remaining delay.
                if (remainingDelay > 0) {
                    recoveryDelays.put(wolf, remainingDelay);
                    debug("Decreased the delay of wolf " + wolf + " to " + ((double) remainingDelay) / 20.0 + " seconds.");
                    
                    // Do not heal this wolf, since the remaining delay is not
                    // zero yet.
                    continue;
                } else {
                    recoveryDelays.remove(wolf);
                }
            }
            
            // Heal the wolf.
            EntityRegainHealthEvent event = new EntityRegainHealthEvent(wolf, 1, EntityRegainHealthEvent.RegainReason.REGEN);
            getServer().getPluginManager().callEvent(event);
            
            if (!event.isCancelled()) {
                wolf.setHealth(Math.min(wolf.getHealth() + event.getAmount(), 20));
                debug("Wolf " + wolf + " was healed to " + wolf.getHealth() + "/" + maxHealth + ".");

                // We need to make sure that the wolf is removed from the set of
                // wounded tamed wolves after it has been completely healed.
                if (wolf.getHealth() >= maxHealth) {
                    itr.remove();
                    debug("Wolf " + wolf + " has recovered.");
                }
            }
        }
    }
    
    /**
     * Converts a health configuration value (range from 0 to 100) to a real
     * health unit and ensures that it is in the valid range (1 to 20).
     * 
     * @param 
     * @return health unit
     */
    private int getHealthUnits(int health) {
        return Math.min(Math.max((int) Math.ceil(((double) health) / 5.0), 1), 20);
    }

    /**
     * Shortcut to regitering event listeners.
     * 
     * @param type
     * @param listener
     */
    private void registerEvent(Type type, Listener listener) {
        getServer().getPluginManager().registerEvent(type, listener, Priority.High, this);
    }

    /**
     * Reports an arbitrary string to the server console and automatically
     * prepend the plugin name to ease message source identification.
     * 
     * @param message
     */
    /*package*/ void log(String message) {
        System.out.println(getDescription().getName() + ": " + message);
    }
    
    /**
     * Sends a debug message to the server console.
     * 
     * @param message 
     */
    /*package*/ void debug(String message) {
        if (config.getBoolean("debug")) {
            log("DEBUG: " + message);
        }
    }

    /**
     * Provides additional information to the full plugin name.
     * 
     * @return enhanced description
     */
    @Override
    public String toString() {
        return super.toString() + " by Elusive92";
    }
}

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
    private Map<Wolf, Integer> recoveryDelays = Collections.synchronizedMap(new HashMap<Wolf, Integer>());

    /**
     * Stores the maximum health a wolf can be healed to.
     */
    private static int maxHealth;
    
    /**
     * Stores the minimum health a wolf needs for automatic recovery.
     */
    private static int minHealth;
    
    /**
     * Stores the number of ticks needed to heal a wolf from 0-100% health.
     */
    private long recoveryDurationTicks;
    
    /**
     * Stores wether to dispatch wolf health instantly.
     */
    private boolean recoverInstantly = false;
    
    /**
     * Stores the interval (in ticks) at which the wolves get healed.
     */
    private long healIntervalTicks;

    /**
     * Initializes the plugin.
     */
    public void onEnable() {
        // Load the configuration every time the plugin is enabled. This needs
        // to be done first to prevent missing properties from eating our souls.
        config.load();
        
        // Determine the minimum and maximum health. A health value needs to be
        // in the range from 1 to 20.
        maxHealth = Math.min(Math.max(config.getInt("heal.max-health"), 1), 20);
        minHealth = Math.min(Math.max(config.getInt("heal.min-health"), 1), 20);

        // Convert seconds to ticks.
        recoveryDurationTicks = config.getInt("heal.duration") * 20;

        // Very low values will be almost equal to instant healing, so we use
        // that to save CPU cycles.
        recoverInstantly = recoveryDurationTicks <= 20; // 1 second

        // Register event listeners.
        registerEvent(Type.CREATURE_SPAWN, entityListener);
        registerEvent(Type.ENTITY_DAMAGE, entityListener);
        registerEvent(Type.ENTITY_DEATH, entityListener);
        registerEvent(Type.ENTITY_TAME, entityListener);

        // Find all wounded tamed wolves that are currently on the server.
        for (World world : getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Wolf) {
                    dispatch((Wolf) entity);
                }
            }
        }

        // We need to do some additional setup for timed recovery.
        if (!recoverInstantly) {
            healIntervalTicks = recoveryDurationTicks / maxHealth;

            // Try to schedule our healing task.
            int taskId = getServer().getScheduler().scheduleSyncRepeatingTask(this, new WOMHealTask(this), 0L, healIntervalTicks);

            // Use instant recovery if the task could not be scheduled. It is
            // better than nothing.
            if (taskId == -1) {
                recoverInstantly = true;
                log("Failed to schedule wolf healing task. Falling back to instant recovery.");
            }
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
     * @param health to check against to determine if the wolf is wounded
     */
    /* package */ void dispatch(Wolf wolf, int health) {
        if (wolf.isTamed() && health < maxHealth) {
            if (!recoverInstantly) {
                recoveringWolves.add(wolf);
                debug("Wolf " + wolf.getUniqueId() + " was scheduled for timed recovery.");
            } else {
                wolf.setHealth(maxHealth);
                recoveringWolves.remove(wolf);
                debug("Wolf " + wolf.getUniqueId() + " was healed instantly.");
            }
        } else {
            recoveringWolves.remove(wolf);
        }
    }
    
    /**
     * Shortcut that automatically inserts the living entity health to check
     * against.
     * 
     * @param wolf
     */
    /* package */ void dispatch(Wolf wolf) {
        dispatch(wolf, wolf.getHealth());
    }
    
    /**
     * Actually heals the wounded wolves. This method is thread safe.
     */
    /* package */ void heal() {
        // Do not do anything if there are no wolves to be healed.
        if (recoveringWolves.isEmpty()) {
            return;
        }

        // We need to use an iterator to allow for removing elements while
        // iterating.
        Iterator<Wolf> itr = recoveringWolves.iterator();

        while (itr.hasNext()) {
            Wolf wolf = itr.next();
            
            // Calculate the target health for the wolf.
            int newHealth = wolf.getHealth() + 1;

            // Did we reach the maximum health?
            if (newHealth < maxHealth) {
                wolf.setHealth(newHealth);
            } else {
                wolf.setHealth(maxHealth);

                // We need to make sure that the wolf is removed from the list
                // of wounded tamed wolves after it has been healed.
                itr.remove();
            }
            
            debug("Wolf " + wolf.getUniqueId() + " was healed to " + wolf.getHealth() + "/" + maxHealth + ".");
        }
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

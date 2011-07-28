package net.elusive92.bukkit.wolvesonmeds;

import java.util.Collections;
import java.util.HashSet;
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
     * Stores the maximum health a wolf can have. This is probably not going to
     * change anytime soon, but having a label for it makes the code a lot
     * easier to read and maintain.
     */
    private static final int maxHealth = 20;
    
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
     * Stores wether to manage wolf health instantly.
     */
    private boolean recoverInstantly = false;

    /**
     * Shuts down the plugin.
     */
    public void onDisable() {
        System.out.println(this + " is now disabled!");
    }

    /**
     * Initializes the plugin.
     */
    public void onEnable() {
        // Load the configuration every time the plugin is enabled.
        config.load();

        // Convert seconds to ticks.
        long recoveryTicks = config.getInt("recover.duration", 0) * 20;

        // Very low values will be almost equal to instant healing, so we use
        // that to save CPU cycles.
        recoverInstantly = recoveryTicks <= 20; // 1 second

        // Find all wounded tamed wolves that are currently on the server.
        for (World world : getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Wolf) {
                    manage((Wolf) entity);
                }
            }
        }

        // Register event listeners.
        registerEvent(Type.CREATURE_SPAWN, entityListener);
        registerEvent(Type.ENTITY_DAMAGE, entityListener);
        registerEvent(Type.ENTITY_DEATH, entityListener);
        registerEvent(Type.ENTITY_TAME, entityListener);

        // We need to do some additional setup for timed recovery.
        if (!recoverInstantly) {
            long healInterval = recoveryTicks / maxHealth;

            // Try to schedule our healing task.
            int taskId = getServer().getScheduler().scheduleSyncRepeatingTask(this, new WOMHealTask(this), 0L, healInterval);

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
     * Decides what to do with the wolf. If he can recover, he is either healed
     * instantly or queued for timed healing.
     * 
     * @param wolf 
     * @param health to check against to determine if the wolf is wounded
     */
    public void manage(Wolf wolf, int health) {
        if (wolf.isTamed() && health < maxHealth) {
            if (recoverInstantly) {
                debug("Wolf " + wolf.getUniqueId() + " was healed instantly");
                wolf.setHealth(maxHealth);
                recoveringWolves.remove(wolf);
            } else {
                debug("Wolf " + wolf.getUniqueId() + " was scheduled for timed recovery");
                recoveringWolves.add(wolf);
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
    public void manage(Wolf wolf) {
        manage(wolf, wolf.getHealth());
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
     * Returns the maximum health for wolves.
     * 
     * @return maximum wolf health
     */
    public int getMaxHealth() {
        return maxHealth;
    }

    /**
     * Returns wether wounded tamed wolves are present on the server.
     * 
     * @return existance
     */
    public boolean hasRecoveringWolves() {
        return !recoveringWolves.isEmpty();
    }

    /**
     * Returns a set of wounded tamed wolves.
     * 
     * @return set of wolves
     */
    public Set<Wolf> getRecoveringWolves() {
        return recoveringWolves;
    }

    /**
     * Reports an arbitrary string to the server console and automatically
     * prepend the plugin name to ease message source identification.
     * 
     * @param message
     */
    public void log(String message) {
        System.out.println(getDescription().getName() + ": " + message);
    }
    
    /**
     * Sends a debug message to the server console.
     * 
     * @param message 
     */
    public void debug(String message) {
        if (config.getBoolean("debug", false)) {
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

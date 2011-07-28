package net.elusive92.bukkit.wolvesonmeds;

import java.util.Iterator;
import org.bukkit.entity.Wolf;

/**
 * Thread that is used to heal wounded tamed wolves over time.
 */
public class WOMHealTask implements Runnable {

    /**
     * Stores a reference to the main plugin class.
     */
    private final WolvesOnMeds plugin;

    /**
     * Creates a heal task.
     * 
     * @param plugin main plugin class
     */
    public WOMHealTask(WolvesOnMeds plugin) {
        this.plugin = plugin;
    }

    /**
     * Executes a healing step.
     */
    public void run() {
        // Do not do anything if there are no wolves to be healed.
        if (!plugin.hasRecoveringWolves()) {
            return;
        }

        // We need to use an iterator to allow for removing elements
        // while iterating.
        Iterator<Wolf> itr = plugin.getRecoveringWolves().iterator();

        while (itr.hasNext()) {
            Wolf wolf = itr.next();

            // Calculate the target health for the wolf.
            int newHealth = wolf.getHealth() + 1;

            // Did we reach the maximum health?
            if (newHealth < plugin.getMaxHealth()) {
                wolf.setHealth(newHealth);
            } else {
                wolf.setHealth(plugin.getMaxHealth());

                // We need to make sure that the wolf is removed from the list
                // of wounded tamed wolves after it has been healed.
                itr.remove();
            }
            
            plugin.debug("Wolf " + wolf.getUniqueId() + " was healed to " + wolf.getHealth() + "/" + plugin.getMaxHealth());
        }
    }
}

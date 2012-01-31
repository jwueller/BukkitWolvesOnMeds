package net.elusive92.bukkit.wolvesonmeds;

import java.util.*;
import net.elusive92.bukkit.wolvesonmeds.util.MathUtil;
import net.elusive92.bukkit.wolvesonmeds.util.PermissionUtil;
import net.elusive92.bukkit.wolvesonmeds.util.Scheduler;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Wolf;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class WolvesOnMeds extends JavaPlugin {

    private WolfListener wolfListener; // listener for anything that happens to the wolves
    private OwnerListener ownerListener; // listener for anythign that relates to the owners of a wolf
    private Scheduler healTaskScheduler; // scheduler for the heal task

    private Set<Wolf> recoveringWolves; // wolves that need to be considered by the heal task
    private Map<Wolf, Long> recoveryDelays; // time (in ticks) until a wolf starts to recover

    private int maxHealth; // maximum health a wolf can be healed to
    private int minHealth; // minimum health a wolf needs for automatic recovery
    private long recoveryDurationTicks; // amount of ticks required to heal a wolf from 0-100% health
    private long recoveryDelayTicks; // amount of ticks to wait before starting to heal a wolf after combat
    private long recoveryIntervalTicks; // interval (in ticks) at which the wolves get healed

    /**
     * Initializes the plugin.
     */
    public void onEnable() {
        // Initialize members (we do this here instead of in the constructor to
        // make sure that the /reload command does not cause tons of memory
        // leaks).
        wolfListener = new WolfListener(this);
        ownerListener = new OwnerListener(this);
        healTaskScheduler = new Scheduler(this, new HealTask(this), false);
        recoveringWolves = new HashSet<Wolf>();
        recoveryDelays = new HashMap<Wolf, Long>();

        // Load and apply the configuration. Also, make sure that the default
        // config is copied to the plugin data directory if it is missing.
        getConfig().options().copyDefaults(true);
        saveConfig();

        // Register event listeners.
        getServer().getPluginManager().registerEvents(wolfListener, this);
        getServer().getPluginManager().registerEvents(ownerListener, this);

        // Notify the console.
        System.out.println(this + " enabled.");
    }

    /**
     * Shuts the plugin down.
     */
    public void onDisable() {
        // Cancel the heal task to ensure that it does not leak on /reload.
        cancelHealTask();

        // Nullify all object members to minimize the memory-leaks caused by the
        // bukkit /reload command.
        wolfListener = null;
        healTaskScheduler = null;
        recoveringWolves = null;
        recoveryDelays = null;

        // Notify the console.
        System.out.println(this + " disabled.");
    }

    /**
     * Hooks into the config reload mechanism to ensure that all cached values
     * are always up-to-date.
     */
    @Override
    public void reloadConfig() {
        super.reloadConfig();
        dlog("config loaded.");

        // Determine the minimum and maximum health. A health value needs to be
        // in the range from 1 to 20.
        maxHealth = getHealthUnits(getConfig().getInt("heal.max-health"));
        minHealth = getHealthUnits(getConfig().getInt("heal.min-health"));
        dlog("max health: " + maxHealth + "; min health: " + minHealth);

        // Convert seconds to ticks.
        recoveryDurationTicks = Math.max(getConfig().getInt("heal.duration"), 1) * 20;
        recoveryDelayTicks = getConfig().getInt("heal.delay") * 20;
        dlog("duration: " + recoveryDurationTicks + "; delay: " + recoveryDelayTicks);

        // Calculate the interval at which the wolves should be healed.
        recoveryIntervalTicks = recoveryDurationTicks / maxHealth;

        // Find all wounded tamed wolves that are currently on the server.
        dispatchAll();
    }

    /**
     * Makes sure that all wolves are tracked. This needs to be done when the
     * configuration is loaded of if a player joins or leaves.
     */
    public void dispatchAll() {
        // Find all wounded tamed wolves that are currently on the server.
        for (World world : getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Wolf) {
                    dispatch((Wolf) entity);
                }
            }
        }
    }

    /**
     * Decides what to do with the wolf. If he can recover, he is either healed
     * instantly or queued for timed healing.
     *
     * @param wolf
     */
    public void dispatch(Wolf wolf) {
        // Insert the current health of the entity if no explicit health is
        // specified.
        dispatch(wolf, wolf.getHealth());
    }

    /**
     * Decides what to do with the wolf. If he can recover, he is either healed
     * instantly or queued for timed healing.
     *
     * @param wolf
     * @param health explicit health value to check against to determine if the
     *               wolf is wounded
     */
    public void dispatch(Wolf wolf, int health) {
        dlog("Dispatching wolf " + wolf.getUniqueId() + ".");

        // Tamed wounded wolves get added to the set of recovering wolves if
        // their maximum recovery health is not reached.
        if (!wolf.isDead() && isHealable(wolf) && health < maxHealth && health >= minHealth) {
            recoveringWolves.add(wolf);
            dlog("Wolf " + wolf.getUniqueId() + " is recovering.");

            // Make sure that the heal task is scheduled, since there is
            // something to be healed.
            scheduleHealTask();
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
    public void resetDelay(Wolf wolf) {
        dlog("Reset delay for wolf " + wolf.getUniqueId() + " to " + recoveryDelayTicks + ".");
        if (recoveryDelayTicks > 0 && hasDelayedHealing(wolf) && recoveringWolves.contains(wolf)) {
            recoveryDelays.put(wolf, recoveryDelayTicks);
        }
    }

    /**
     * Actually heals all wounded wolves.
     */
    public void heal() {
        // Do not do anything if there are no wolves to be healed.
        if (recoveringWolves.isEmpty()) return;

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
                    dlog("Decreased the delay of wolf " + wolf.getUniqueId() + " to " + ((double) remainingDelay) / 20.0 + " seconds.");

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
                dlog("Wolf " + wolf.getUniqueId() + " was healed to " + wolf.getHealth() + "/" + maxHealth + ".");

                // We need to make sure that the wolf is removed from the set of
                // wounded tamed wolves after it has been completely healed.
                if (wolf.getHealth() >= maxHealth) {
                    itr.remove();
                    dlog("Wolf " + wolf.getUniqueId() + " has recovered.");
                }
            }
        }

        // Cancel the heal task if there is nothing left to do, so that it does
        // not run when not needed.
        if (recoveringWolves.isEmpty()) {
            cancelHealTask();
        }
    }

    /**
     * Returns whether a wolf can be healed.
     *
     * @param tameable
     * @return whether it can be healed
     */
    private boolean isHealable(Tameable tameable) {
        if (!tameable.isTamed()) return false;
        return PermissionUtil.hasPermission(tameable, "heal");
    }

    /**
     * Returns whether a wolf has a healing delay.
     *
     * @param tameable
     * @return whether it has a delay
     */
    private boolean hasDelayedHealing(Tameable tameable) {
        return !PermissionUtil.hasPermission(tameable, "no-delay");
    }

    /**
     * Schedules the heal task.
     */
    private void scheduleHealTask() {
        dlog("Scheduling heal task.");
        if (!healTaskScheduler.schedule(recoveryIntervalTicks)) {
            throw new RuntimeException("Failed to schedule wolf healing task.");
        }
    }

    /**
     * Cancels the heal task.
     */
    private void cancelHealTask() {
        dlog("Stopping heal task.");
        healTaskScheduler.cancel();
    }

    /**
     * Converts a health configuration value (range from 0 to 100) to a real
     * health unit and ensures that it is in the valid range (1 to 20).
     *
     * @param health percentage
     * @return health unit
     */
    private int getHealthUnits(int health) {
        return MathUtil.clamp((int) Math.ceil(((double) health) / 5.0), 1, 20);
    }

    /**
     * Sends a message to the server console.
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
    public void dlog(String message) {
        if (getConfig().getBoolean("debug")) {
            log("DEBUG: " + message);
        }
    }
}

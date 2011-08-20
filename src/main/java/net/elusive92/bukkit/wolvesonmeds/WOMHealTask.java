package net.elusive92.bukkit.wolvesonmeds;

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
     * Issues healing a bunch of wolves.
     */
    public void run() {
        plugin.heal();
    }
}

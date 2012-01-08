package net.elusive92.bukkit.wolvesonmeds;

/**
 * Thread that is used to heal wounded tamed wolves over time.
 */
public class HealTask implements Runnable {

    private final WolvesOnMeds plugin; // reference to the main plugin class

    /**
     * Creates a heal task.
     *
     * @param plugin main plugin class
     */
    public HealTask(WolvesOnMeds plugin) {
        this.plugin = plugin;
    }

    /**
     * Issues healing a bunch of wolves.
     */
    public void run() {
        // This does not explicitly need to be synchronized, as it should be
        // scheduled synchronously. This is in place for the case that this
        // changes and somebody forgets to synchronize this. It could lead to
        // really weird behaviour.
        synchronized (plugin) {
            plugin.heal();
        }
    }
}

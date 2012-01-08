package net.elusive92.bukkit.wolvesonmeds.util;

import net.elusive92.bukkit.wolvesonmeds.WolvesOnMeds;
import org.bukkit.scheduler.BukkitScheduler;

/**
 * Scheduler that is responsible for managing a single task and ensures that it
 * does not run several times.
 */
public class Scheduler {

    private static final int INVALID = -1; // represents the value that indicates an invalid task ID

    private final WolvesOnMeds plugin; // reference to the main plugin class
    private final Runnable task; // task that is scheduled
    private final boolean async; // whether to schedule the task asynchronously
    private int taskId = INVALID; // ID that the scheduler assigns to the scheduled task

    /**
     * Creates a heal task.
     *
     * @param plugin
     * @param task
     * @param async whether to schedule the task asynchronously
     */
    public Scheduler(WolvesOnMeds plugin, Runnable task, boolean async) {
        this.plugin = plugin;
        this.task = task;
        this.async = async;
    }

    /**
     * (Re-)schedules the heal task.
     *
     * @param interval
     * @return success
     */
    public boolean schedule(long interval) {
        // Shut the task down if it is already running.
        cancel();

        // Try to schedule the task.
        BukkitScheduler s = plugin.getServer().getScheduler();

        if (async) {
            taskId = s.scheduleAsyncRepeatingTask(plugin, task, 0L, interval);
        } else {
            taskId = s.scheduleSyncRepeatingTask(plugin, task, 0L, interval);
        }

        return isRunning();
    }

    /**
     * Cancels the task (if it is scheduled).
     */
    public void cancel() {
        // Do not do anything if there is nothing to cancel.
        if (!isRunning()) return;

        // Cancel the task.
        plugin.getServer().getScheduler().cancelTask(taskId);
        taskId = INVALID;
    }

    /**
     * Returns wether the task is currently running.
     */
    private boolean isRunning() {
        return taskId != INVALID;
    }
}
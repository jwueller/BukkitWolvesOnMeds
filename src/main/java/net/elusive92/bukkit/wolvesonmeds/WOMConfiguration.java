package net.elusive92.bukkit.wolvesonmeds;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.util.config.Configuration;

/**
 * Handles the plugin configuration.
 */
public class WOMConfiguration extends Configuration {
    
    /**
     * Stores the configuration file name.
     */
    private static final String filename = "plugins/WolvesOnMeds/config.yml";
    
    /**
     * Stores the default configuration values.
     */
    private static final Map<String, Object> defaults = new HashMap<String, Object>();

    // Defines the default configuration values.
    static {
        defaults.put("recover.duration", new Integer(60));
        defaults.put("recover.while-sleeping", true);
        defaults.put("debug", false);
    }
    
    /**
     * Stores a reference to the main plugin class.
     */
    private final WolvesOnMeds plugin;

    /**
     * Creates a configuration.
     * 
     * @param plugin main plugin class
     */
    public WOMConfiguration(WolvesOnMeds plugin) {
        super(new File(filename));
        this.plugin = plugin;
    }
    
    /**
     * Loads the configuration and ensures that the configuration file contains
     * all possible configuration options.
     */
    @Override
    public void load() {
        super.load();

        // Set defaults.
        setHeader("# " + plugin + "\n# " + new Date());

        for (Map.Entry<String, Object> entry : defaults.entrySet()) {
            Object currentValue = getProperty(entry.getKey());

            // Set the default if the property is missing.
            if (currentValue == null) {
                setProperty(entry.getKey(), entry.getValue());
            }
        }

        // Write to disc.
        if (!save()) {
            plugin.log("Could not save configuration to " + filename + "! Please check the file permissions.");
        }
    }
}

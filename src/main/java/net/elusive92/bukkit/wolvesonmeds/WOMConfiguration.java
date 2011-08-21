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
     * Stores a reference to the main plugin class.
     */
    private final WolvesOnMeds plugin;
    
    /**
     * Stores the default values of all properties.
     */
    private static final Map<String, Object> defaults = new HashMap<String, Object>();
    
    static {
        defaults.put("heal.duration",   new Integer(60));
        defaults.put("heal.delay",      new Integer(10));
        
        // Dead wolves (with 0 health) cannot be healed, but this will look
        // better in the configuration and the validation will convert it to the
        // lowest valid value.
        defaults.put("heal.min-health", new Integer(0));
        defaults.put("heal.max-health", new Integer(100));
        
        defaults.put("debug",           false);
    }
    
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
     * (Re)Loads the configuration and ensures that the configuration file does
     * not contain any invalid configuration options.
     */
    public void load() {
        super.load();
        
        // Set file header.
        setHeader("# " + plugin + "\n# " + new Date());
        
        // Normalize the configuration. We ignore unknown properties.
        for (Map.Entry<String, Object> item : defaults.entrySet()) {
            String property = item.getKey();
            
            // Use the defaults if the property is not present in the
            // configuration file.
            if (getProperty(property) == null) {
                setProperty(property, item.getValue());
            }
        }

        // Persist the validated configuration.
        if (!save()) {
            plugin.log("Could not save configuration to " + filename + "! Please check the file permissions.");
        }
    }
    
    /**
     * Returns a value for a integer property.
     * 
     * @param property
     * @return value
     */
    public int getInt(String property) {
        return super.getInt(property, (Integer) defaults.get(property));
    }
    
    /**
     * Returns a value for a boolean property.
     * 
     * @param property
     * @return value
     */
    public boolean getBoolean(String property) {
        return super.getBoolean(property, (Boolean) defaults.get(property));
    }
}

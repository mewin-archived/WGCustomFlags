/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mewin.WGCustomFlags;

import com.mewin.WGCustomFlags.data.FlagSaveHandler;
import com.mewin.WGCustomFlags.data.JDBCSaveHandler;
import com.mewin.WGCustomFlags.data.YAMLSaveHandler;
import com.mewin.WGCustomFlags.util.ClassHacker;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.Flag;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The WGCustomFlagsPlugin that allows you to hook into WorldGuard and setup custom flags
 * @author mewin
 */
public class WGCustomFlagsPlugin extends JavaPlugin {
    
    private JDBCSaveHandler jdbcConnector;
    private WGCustomFlagsListener listener;
    
    public static HashMap<String, Flag> customFlags;
    public static WorldGuardPlugin wgPlugin;
    
    /**
     * Constructor for WGCustomFlagsPlugin
     * should not be called manually
     */
    public WGCustomFlagsPlugin()
    {
        super();
        customFlags = new HashMap<>();
        listener = new WGCustomFlagsListener(this);
    }
    
    /**
     * sets up the plugin and creates connection to the WorldGuard plugin
     */
    private void setupWgPlugin()
    {
        Plugin plug = getServer().getPluginManager().getPlugin("WorldGuard");
        if(plug == null || !(plug instanceof WorldGuardPlugin))
        {
            System.out.println("WorldGuard plugin not found, disabling.");
            
            getServer().getPluginManager().disablePlugin(this);
        }
        else
        {
            wgPlugin = (WorldGuardPlugin) plug;
        }
    }
    
    @Override
    public void onEnable()
    {
        setupWgPlugin();
        if (wgPlugin.getGlobalStateManager().useSqlDatabase)
        {
            jdbcConnector = new JDBCSaveHandler(wgPlugin.getGlobalStateManager().sqlDsn,
                    wgPlugin.getGlobalStateManager().sqlUsername,
                    wgPlugin.getGlobalStateManager().sqlPassword, this);
        }
        
        getServer().getPluginManager().registerEvents(listener, this);
        
        ClassHacker.setPrivateValue(wgPlugin.getDescription(), "version", wgPlugin.getDescription().getVersion() + " with custom flags plugin.");
    }
    
    @Override
    public void onDisable()
    {
        saveAllWorlds();
    }
    
    /**
     * loads the flag values for all worlds
     * should not be called manually
     */
    public void loadAllWorlds()
    {
        Iterator<World> itr = getServer().getWorlds().iterator();
        
        while(itr.hasNext())
        {
            loadFlagsForWorld(itr.next());
        }
    }
    
    /**
     * loads the flags for a single world
     * should not be called manually
     * @param world the world to load the flags for
     */
    public void loadFlagsForWorld(World world)
    {
        //getLogger().log(Level.INFO, "Loading flags for world {0}", world.getName());
        FlagSaveHandler handler;
        if (wgPlugin.getGlobalStateManager().useSqlDatabase)
        {
            handler = jdbcConnector;
        }
        else
        {
            handler = new YAMLSaveHandler(this, wgPlugin);
        }
        handler.loadFlagsForWorld(world);
    }
    
    /**
     * saves all custom flags to YAML file or database
     * should not be called manually
     */
    public void saveAllWorlds()
    {
        Iterator<World> itr = getServer().getWorlds().iterator();
        
        while(itr.hasNext())
        {
            saveFlagsForWorld(itr.next());
        }
    }
    
    /**
     * saves the flag values for a single world
     * should not be called manually
     * @param world the world to save the flags for
     */
    public void saveFlagsForWorld(World world)
    {
        getLogger().log(Level.INFO, "Saving flags for world {0}", world.getName());
        FlagSaveHandler handler;
        if (wgPlugin.getGlobalStateManager().useSqlDatabase)
        {
            handler = jdbcConnector;
        }
        else
        {
            handler = new YAMLSaveHandler(this, wgPlugin);
        }
        
        handler.saveFlagsForWorld(world);
    }
    
    /**
     * adds a custom flag and hooks it into WorldGuard
     * @param flag the flag to add
     */
    public void addCustomFlag(Flag flag)
    {
        if (customFlags.containsKey(flag.getName()))
        {
            if (!customFlags.get(flag.getName()).getClass().equals(flag.getClass()))
            {
                getServer().getLogger().log(Level.WARNING, "Duplicate flag: {0}", flag.getName());
            }
        }
        else
        {
            customFlags.put(flag.getName(), flag);

            addWGFlag(flag);
            
            loadAllWorlds();
        }
    }
    
    
    private void addWGFlag(Flag<?> flag)
    {
        try
        {
            Field flagField = DefaultFlag.class.getField("flagsList");
            
            Flag<?>[] flags = new Flag<?>[DefaultFlag.flagsList.length + 1];
            System.arraycopy(DefaultFlag.flagsList, 0, flags, 0, DefaultFlag.flagsList.length);
            
            flags[DefaultFlag.flagsList.length] = flag;
            
            if(flag == null)
            {
                throw new RuntimeException("flag is null");
            }
            
            ClassHacker.setStaticValue(flagField, flags);
        }
        catch(NoSuchFieldException | RuntimeException ex)
        {
            getServer().getLogger().log(Level.WARNING, "Could not add flag {0} to WorldGuard", flag.getName());
        }
        
        for(int i = 0; i < DefaultFlag.getFlags().length; i++)
        {
            Flag<?> flag1 = DefaultFlag.getFlags()[i];
            if (flag1 == null)
            {
                throw new RuntimeException("Flag["+i+"] is null");
            }
        }
    }
    
    /**
     * retrieves the ClassLoader of this plugin
     * there is no reason to call this from another plugin
     * @return the ClassLoader
     */
    @Override
    public ClassLoader getClassLoader()
    {
        return super.getClassLoader();
    }
}
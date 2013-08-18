/*
 * Copyright (C) 2012 mewin <mewin001@hotmail.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.mewin.WGCustomFlags;

import com.mewin.WGCustomFlags.data.FlagSaveHandler;
import com.mewin.WGCustomFlags.data.JDBCSaveHandler;
import com.mewin.WGCustomFlags.data.YAMLSaveHandler;
import com.mewin.WGCustomFlags.util.ClassHacker;
import com.sk89q.util.yaml.YAMLFormat;
import com.sk89q.util.yaml.YAMLProcessor;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.Flag;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The WGCustomFlagsPlugin that allows you to hook into WorldGuard and setup custom flags
 * @author mewin <mewin001@hotmail.de>
 */
public class WGCustomFlagsPlugin extends JavaPlugin {

    private static final String DEFAULT_CONFIG = "# WGCustomFlags default config\r\n"
                                               + "# If you edit this make sure not to use tabulators, but spaces.\r\n"
                                               + "\r\n"
                                               + "# name: save-handler\r\n"
                                               + "# default: auto\r\n"
                                               + "# description: determines whether a database or a flat file is used to save the flags\r\n"
                                               + "# values: auto - detect which option WorldGuard uses, flat - force flat file usage\r\n"
                                               + "save-handler: auto\r\n"
                                               + "# name: flag-saving\r\n"
                                               + "# default: unload,save\r\n"
                                               + "# description: determines when to save the custom flag values\r\n"
                                               + "# values: one or multiple values of [unload, save, change] seperated with a comma\r\n"
                                               + "# unload - saves the flags when a world is unloaded (highly recommended as the flags are not saved for world if it is unloaded before the server shuts down)\r\n"
                                               + "# save - saves the flags everytime a world is saved (can cause lags if you have a plugin like MultiVerse the auto-saves the worlds very often)\r\n"
                                               + "# change - saves the flags everytime any flag has changed in the world\r\n"
                                               + "flag-saving: unload,save";
    
    private JDBCSaveHandler jdbcConnector = null;
    private WGCustomFlagsListener listener;

    public static HashMap<String, Flag> customFlags;
    public static WorldGuardPlugin wgPlugin;

    private File configFile;
    private YAMLProcessor config;

    /**
     * Constructor for WGCustomFlagsPlugin
     * should not be called manually
     */
    public WGCustomFlagsPlugin() {
        super();
        customFlags = new HashMap<String, Flag>();
        listener = new WGCustomFlagsListener(this);
    }

    /**
     * sets up the plugin and creates connection to the WorldGuard plugin
     */
    private void setupWgPlugin() {
        Plugin plug = getServer().getPluginManager().getPlugin("WorldGuard");
        if(plug == null || !(plug instanceof WorldGuardPlugin)) {
            getLogger().warning("WorldGuard plugin not found, disabling.");

            getServer().getPluginManager().disablePlugin(this);
        } else {
            wgPlugin = (WorldGuardPlugin) plug;
        }
    }

    private void loadConfig() {
        getLogger().info("Loading configuration...");
        if (!configFile.exists()) {
            try {
                getLogger().info("No configuration found, writing defaults.");
                writeDefaultConfig();
            } catch(IOException ex) {
                getLogger().log(Level.SEVERE, "Could not create default configuration.", ex);

                return;
            }
        }

        config = new YAMLProcessor(configFile, true, YAMLFormat.EXTENDED);

        try {
            config.load();
        }
        catch(IOException ex) {
            getLogger().log(Level.SEVERE, "Could read configuration.", ex);
            return;
        }

        getLogger().info("Configuration loaded.");
    }

    private void writeDefaultConfig() throws IOException {
        if (!this.getDataFolder().exists()) {
            this.getDataFolder().mkdirs();
        }

        if (!this.configFile.exists()) {
            this.configFile.createNewFile();
        }

        BufferedWriter out = new BufferedWriter(new FileWriter(configFile));

        out.write(DEFAULT_CONFIG);

        out.close();
    }

    @Override
    public void onEnable() {
        configFile = new File(getDataFolder(), "config.yml");

        setupWgPlugin();
        if (wgPlugin.getGlobalStateManager().useSqlDatabase) {
            jdbcConnector = new JDBCSaveHandler(wgPlugin.getGlobalStateManager().sqlDsn,
                    wgPlugin.getGlobalStateManager().sqlUsername,
                    wgPlugin.getGlobalStateManager().sqlPassword, this);
        }

        getServer().getPluginManager().registerEvents(listener, this);

        loadConfig();

        ClassHacker.setPrivateValue(wgPlugin.getDescription(), "version", wgPlugin.getDescription().getVersion() + " with custom flags plugin.");
    }

    @Override
    public void onDisable()
    {
        saveAllWorlds(false);
        if (jdbcConnector != null)
        {
            jdbcConnector.close();
        }
    }
    /**
     * loads the flag values for all worlds
     * should not be called manually
     */
    public void loadAllWorlds() {
        Iterator<World> itr = getServer().getWorlds().iterator();

        while(itr.hasNext()) {
            loadFlagsForWorld(itr.next());
        }
    }

    /**
     * loads the flags for a single world
     * should not be called manually
     * @param world the world to load the flags for
     */
    public void loadFlagsForWorld(World world) {
        //getLogger().log(Level.INFO, "Loading flags for world {0}", world.getName());
        FlagSaveHandler handler = getSaveHandler();

        handler.loadFlagsForWorld(world);
    }

    /**
     * saves all custom flags to YAML file or database
     * should not be called manually
     */
    public void saveAllWorlds(boolean asynchron)
    {
        Iterator<World> itr = getServer().getWorlds().iterator();

        while(itr.hasNext()) {
            saveFlagsForWorld(itr.next(), asynchron);
        }
    }

    /**
     * saves the flag values for a single world
     * should not be called manually
     * @param world the world to save the flags for
     */
    public void saveFlagsForWorld(final World world, boolean asynchron)
    {
        getLogger().log(Level.FINEST, "Saving flags for world {0}", world.getName());
        final FlagSaveHandler handler = getSaveHandler();

        if (asynchron)
        {
            getServer().getScheduler().runTaskAsynchronously(this, new Runnable()
            {
                @Override
                public void run()
                {
                    handler.saveFlagsForWorld(world);
                }
            });
        }
        else
        {
            handler.saveFlagsForWorld(world);
        }
    }

    /**
     * adds a custom flag and hooks it into WorldGuard
     * @param flag the flag to add
     */
    public void addCustomFlag(Flag flag) {
        if (customFlags.containsKey(flag.getName())) {
            if (!customFlags.get(flag.getName()).getClass().equals(flag.getClass())) {
                getServer().getLogger().log(Level.WARNING, "Duplicate flag: {0}", flag.getName());
            }
        } else {
            customFlags.put(flag.getName(), flag);

            addWGFlag(flag);

            getLogger().log(Level.INFO, "Added custom flag \"{0}\" to WorldGuard.", flag.getName());

            loadAllWorlds();
        }
    }
    
    /**
     * adds flags for all public and static fields of a class that extend Flag
     * @param clazz the class that contains the flags
     */
    public void addCustomFlags(Class clazz) throws Exception
    {
        for (Field f : clazz.getDeclaredFields())
        {
            try
            {
                if (Flag.class.isAssignableFrom(f.getType()) && (f.getModifiers() & (Modifier.STATIC | Modifier.PUBLIC)) > 0)
                {
                    f.setAccessible(true);
                    Flag flag = (Flag) f.get(null);
                    if (flag != null)
                    {
                        addCustomFlag(flag);
                    }
                }
            }
            catch(Exception ex)
            {
                throw new Exception("Could not add custom flag " + f.getName() + " of class " + clazz.getName(), ex);
            }
        }
    }

    private void addWGFlag(Flag<?> flag) {
        try {
            Field flagField = DefaultFlag.class.getField("flagsList");

            Flag<?>[] flags = new Flag<?>[DefaultFlag.flagsList.length + 1];
            System.arraycopy(DefaultFlag.flagsList, 0, flags, 0, DefaultFlag.flagsList.length);

            flags[DefaultFlag.flagsList.length] = flag;

            if(flag == null) {
                throw new RuntimeException("flag is null");
            }

            ClassHacker.setStaticValue(flagField, flags);
        }
        catch(Exception ex) {
            getServer().getLogger().log(Level.WARNING, "Could not add flag {0} to WorldGuard", flag.getName());
        }

        for(int i = 0; i < DefaultFlag.getFlags().length; i++) {
            Flag<?> flag1 = DefaultFlag.getFlags()[i];
            if (flag1 == null) {
                throw new RuntimeException("Flag["+i+"] is null");
            }
        }
    }
    
    /**
     * retrieves the custom flag configuration
     * @return a YAMLProcessor representing the configuration of the plugin
     */
    public YAMLProcessor getConf()
    {
        return this.config;
    }

    private FlagSaveHandler getSaveHandler() {
        if (config.getString("save-handler", "auto").equalsIgnoreCase("auto") && wgPlugin.getGlobalStateManager().useSqlDatabase) {
            return jdbcConnector;
        } else {
            return new YAMLSaveHandler(this, wgPlugin);
        }
    }
}
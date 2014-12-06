/*
 * Copyright (C) 2012-2014 mewin <mewin001@hotmail.de>
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
import com.mewin.WGCustomFlags.flags.CustomSetFlag;
import com.mewin.WGCustomFlags.util.ClassHacker;
import com.sk89q.util.yaml.YAMLFormat;
import com.sk89q.util.yaml.YAMLProcessor;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.EnumFlag;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.SetFlag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;

import com.sk89q.worldguard.protection.managers.storage.RegionDriver;
import com.sk89q.worldguard.protection.managers.storage.sql.SQLDriver;
import com.sk89q.worldguard.util.sql.DataSourceConfig;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
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
                                               + "flag-saving: unload,save\r\n"
                                               + "# name: flag-logging\r\n"
                                               + "# default: true\r\n"
                                               + "# values: true, false\r\n"
                                               + "# set to false if you want to disable the logging of flag loading/saving and adding\r\n"
                                               + "flag-logging: true\r\n";
                                               /*+ "# name: tab-completions\r\n"
                                               + "# default: false\r\n"
                                               + "# description: enable/disable experimental tab completions for the region command of WorldGuard\r\n"
                                               + "# values: true,false\r\n"
                                               + "tab-completions: false";*/
    
    private JDBCSaveHandler jdbcConnector = null;
    private WGCustomFlagsListener listener;
    private PluginListener plListener;

    public static WorldGuardPlugin wgPlugin;

    private File configFile;
    private YAMLProcessor config;
    private boolean flagLogging = true;

    /**
     * Constructor for WGCustomFlagsPlugin
     * should not be called manually
     */
    public WGCustomFlagsPlugin()
    {
        super();
        listener = new WGCustomFlagsListener(this);
        plListener = new PluginListener(this);
    }

    /**
     * sets up the plugin and creates connection to the WorldGuard plugin
     */
    private void setupWgPlugin()
    {
        Plugin plug = getServer().getPluginManager().getPlugin("WorldGuard");
        if(plug == null || !(plug instanceof WorldGuardPlugin)) {
            getLogger().warning("WorldGuard plugin not found, disabling.");

            getServer().getPluginManager().disablePlugin(this);
        } else {
            wgPlugin = (WorldGuardPlugin) plug;
        }
    }

    private void loadConfig()
    {
        getLogger().info("Loading configuration...");
        if (!configFile.exists())
        {
            try
            {
                getLogger().info("No configuration found, writing defaults.");
                writeDefaultConfig();
            }
            catch(IOException ex)
            {
                getLogger().log(Level.SEVERE, "Could not create default configuration.", ex);

                return;
            }
        }

        config = new YAMLProcessor(configFile, true, YAMLFormat.EXTENDED);

        try
        {
            config.load();
        }
        catch(IOException ex)
        {
            getLogger().log(Level.SEVERE, "Could read configuration.", ex);
            return;
        }

        getLogger().info("Configuration loaded.");
    }

    private void writeDefaultConfig() throws IOException
    {
        if (!this.getDataFolder().exists())
        {
            this.getDataFolder().mkdirs();
        }

        if (!this.configFile.exists())
        {
            this.configFile.createNewFile();
        }

        BufferedWriter out = new BufferedWriter(new FileWriter(configFile));

        out.write(DEFAULT_CONFIG);

        out.close();
    }

    @Override
    public void onEnable() {
        FlagManager.setWGCFInstance(this);
        configFile = new File(getDataFolder(), "config.yml");

        setupWgPlugin();
        RegionDriver driver = wgPlugin.getGlobalStateManager().selectedRegionStoreDriver;
        if (driver instanceof SQLDriver)
        {
            SQLDriver sqlDriver = (SQLDriver) driver;
            DataSourceConfig dsConfig = getDataSourceConfig(sqlDriver);
            if (dsConfig != null) {
                jdbcConnector = new JDBCSaveHandler(dsConfig.getDsn(),
                        dsConfig.getUsername(),
                        dsConfig.getPassword(), this);
            }
        }

        getServer().getPluginManager().registerEvents(listener, this);
        getServer().getPluginManager().registerEvents(plListener, this);

        loadConfig();
        
        if (config.getBoolean("tab-completions", false))
        {
            getServer().getPluginManager().registerEvents(new TabCompleteListener(wgPlugin), this);
        }
        
        flagLogging = config.getBoolean("flag-logging", true);

        ClassHacker.setPrivateValue(wgPlugin.getDescription(), "version", wgPlugin.getDescription().getVersion() + " with custom flags plugin.");
    }

    /**
     * Use some really hacky magic to get hold of the package-protected DataSourceConfig.
     */
    private DataSourceConfig getDataSourceConfig(SQLDriver sqlDriver) {
        try {
            Method getConfig = sqlDriver.getClass().getDeclaredMethod("getConfig");
            getConfig.setAccessible(true);
            Object returnValue = getConfig.invoke(sqlDriver);
            getConfig.setAccessible(false);
            if (returnValue instanceof DataSourceConfig) {
                return (DataSourceConfig) returnValue;
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            // Ignore - return null, and continue
        }
        return null;
    }

    public boolean isFlagLogging()
    {
        return this.flagLogging;
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
    public void loadAllWorlds()
    {
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
    public void loadFlagsForWorld(World world)
    {
        //getLogger().log(Level.INFO, "Loading flags for world {0}", world.getName());
        FlagSaveHandler handler = getSaveHandler();

        handler.loadFlagsForWorld(world);
    }

    /**
     * saves all custom flags to YAML file or database
     * should not be called manually
     * @param asynchron if set to true the flags will be saved asynchronously
     */
    public void saveAllWorlds(boolean asynchron)
    {
        Iterator<World> itr = getServer().getWorlds().iterator();

        while(itr.hasNext())
        {
            saveFlagsForWorld(itr.next(), asynchron);
        }
    }

    /**
     * saves the flag values for a single world
     * should not be called manually
     * @param world the world to save the flags for
     * @param asynchron if set to true the flags will be saved asynchronously
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
    public void addCustomFlag(Flag flag)
    {
        FlagManager.addCustomFlag(flag);
    }
    
    /**
     * adds flags for all public and static fields of a class that extend Flag
     * @param clazz the class that contains the flags
     * @throws java.lang.Exception
     */
    public void addCustomFlags(Class clazz) throws Exception
    {
        FlagManager.addCustomFlags(clazz);
    }
    
    /**
     * retrieves the custom flag configuration
     * @return a YAMLProcessor representing the configuration of the plugin
     */
    public YAMLProcessor getConf()
    {
        return this.config;
    }

    private FlagSaveHandler getSaveHandler()
    {
        if (config.getString("save-handler", "auto").equalsIgnoreCase("auto") && jdbcConnector != null)
        {
            return jdbcConnector;
        }
        else
        {
            return new YAMLSaveHandler(this, wgPlugin);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (command.getLabel().equalsIgnoreCase("flags"))
        {
            if (args.length < 1)
            {
                sendFlagList(sender, false);
                return true;
            }
            else
            {
                if (args[0].equalsIgnoreCase("-d"))
                {
                    sendFlagList(sender, true);
                    return true;
                }
                else if (args[0].equalsIgnoreCase("-i") && args.length > 1)
                {
                    Flag<?> flag = FlagManager.getCustomFlag(args[1]);
                    if (flag == null)
                    {
                        for (Flag<?> flag2 : DefaultFlag.getFlags())
                        {
                            if (flag2.getName().equalsIgnoreCase(args[1]))
                            {
                                flag = flag2;
                                break;
                            }
                        }
                        if (flag == null)
                        {
                            sender.sendMessage(ChatColor.RED + "There is no flag with this name.");
                            return true;
                        }
                    }
                    sender.sendMessage(ChatColor.YELLOW + "Flag \"" + flag.getName() + "\":");
                    sender.sendMessage(ChatColor.GRAY + (FlagManager.customFlags.containsKey(flag.getName())? "Custom" : "Default") + " flag");
                    sender.sendMessage(ChatColor.BLUE + "Type: " + flag.getClass().getSimpleName());
                    if (flag instanceof StateFlag)
                    {
                        sender.sendMessage(ChatColor.BLUE + "Default: " + ((StateFlag) flag).getDefault());
                    }
                    else if (flag instanceof EnumFlag)
                    {
                        Class enumClass = (Class) ClassHacker.getPrivateValue(flag, "enumClass");
                        if (enumClass != null)
                        {
                            Object[] enumValues = enumClass.getEnumConstants();
                            String values = enumValues[0].toString();
                            for (int i = 1; i < enumValues.length; i++)
                            {
                                values += "," + enumValues[i];
                            }
                            sender.sendMessage(ChatColor.BLUE + "Values: " + values);
                        }
                    }
                    else if (flag instanceof SetFlag
                            || flag instanceof CustomSetFlag)
                    {
                        Flag<?> subFlag = (Flag<?>) ClassHacker.getPrivateValue(flag, "subFlag");
                        sender.sendMessage(ChatColor.BLUE + "Sub Flag Type: " + subFlag.getClass().getSimpleName());
                        if (subFlag instanceof SetFlag
                                || subFlag instanceof CustomSetFlag)
                        {
                            Class enumClass = (Class) ClassHacker.getPrivateValue(subFlag, "enumClass");
                            if (enumClass != null)
                            {
                                Object[] enumValues = enumClass.getEnumConstants();
                                String values = enumValues[0].toString();
                                for (int i = 1; i < enumValues.length; i++)
                                {
                                    values += "," + enumValues[i];
                                }
                                sender.sendMessage(ChatColor.BLUE + "Values: " + values);
                            }
                        }
                    }
                    String desc = FlagManager.getFlagDescription(flag.getName());
                    if (desc != null)
                    {
                        sender.sendMessage(ChatColor.BLUE + "Description: " + desc);
                    }
                    return true;
                }
                else
                {
                    return false;
                }
            }
        }
        else
        {
            return false;
        }
    }
    
    private void sendFlagList(CommandSender sender, boolean defaults)
    {
        ArrayList<String> flags = new ArrayList<String>();
        Set<String> keys = FlagManager.customFlags.keySet();
        if (!defaults)
        {
            flags.addAll(keys);
        }
        else
        {
            for (Flag<?> flag : DefaultFlag.getFlags())
            {
                flags.add(flag.getName());
            }
        }
        
        String text;
        if (flags.size() > 0)
        {
            String[] names = flags.toArray(new String[0]);
            Arrays.sort(names);
            text = (keys.contains(names[0]) ? ChatColor.AQUA : "") + names[0];
            for(int i = 1; i < names.length; i++)
            {
                text += ChatColor.GRAY + "," + (keys.contains(names[i]) ? ChatColor.AQUA : "") + names[i];
            }
        }
        else
        {
            text = ChatColor.RED + "There are no flags to display.";
        }
        sender.sendMessage(text);
    }
}
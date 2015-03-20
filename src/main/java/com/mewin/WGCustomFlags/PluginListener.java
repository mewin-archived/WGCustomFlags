/*
 * Copyright (C) 2013 mewin<mewin001@hotmail.de>
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

import com.mewin.WGCustomFlags.flags.CustomLocationFlag;
import com.mewin.WGCustomFlags.flags.CustomSetFlag;
import com.mewin.WGCustomFlags.flags.CustomVectorFlag;
import com.sk89q.worldguard.protection.flags.BooleanFlag;
import com.sk89q.worldguard.protection.flags.CommandStringFlag;
import com.sk89q.worldguard.protection.flags.DoubleFlag;
import com.sk89q.worldguard.protection.flags.EntityTypeFlag;
import com.sk89q.worldguard.protection.flags.EnumFlag;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.IntegerFlag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StringFlag;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.yaml.snakeyaml.Yaml;

/**
 *
 * @author mewin<mewin001@hotmail.de>
 */
public class PluginListener implements Listener
{
    private WGCustomFlagsPlugin custPlugin;
    
    public PluginListener(WGCustomFlagsPlugin plug)
    {
        this.custPlugin = plug;
    }
    
    @EventHandler
    public void onPluginEnable(PluginEnableEvent e)
    {
        Plugin plugin = e.getPlugin();
        Yaml yaml = new Yaml();
        InputStream in = plugin.getClass().getResourceAsStream("/flags.yml");
        if (in != null)
        {
            if (custPlugin.isFlagLogging())
            {
                custPlugin.getLogger().log(Level.INFO, "flags.yml found in {0}", plugin.getName());
            }
            Object obj = yaml.load(in);
            if (obj instanceof Map)
            {
                for (Object key : ((Map) obj).keySet())
                {
                    String name = key.toString();
                    Object keyObj = ((Map) obj).get(key);
                    if (keyObj instanceof Map)
                    {
                        Map map = (Map) keyObj;
                        if (map.containsKey("field")
                                && map.get("field") instanceof String)
                        {
                            String field = (String) map.get("field");
                            int dot = field.lastIndexOf(".");
                            String clazz = field.substring(0, dot);
                            try
                            {
                                Field f = Class.forName(clazz).getDeclaredField(field.substring(dot + 1));
                                FlagManager.addCustomFlag(((Flag) f.get(null)));
                                if (map.containsKey("description"))
                                {
                                    FlagManager.addFlagDescription(name, map.get("description").toString());
                                }
                            }
                            catch(Exception ex)
                            {
                                Bukkit.getServer().getLogger().log(Level.WARNING, "Failed to add flag " + name, ex);
                            }
                        }
                        else if (map.containsKey("type"))
                        {
                            Flag newFlag = getFlag(name, map);
                            if (newFlag == null)
                            {
                                Bukkit.getLogger().log(Level.WARNING, "Could not add flag {0}, invalid flag type.", name);
                            }
                            else
                            {
                                FlagManager.addCustomFlag(newFlag);
                                if (map.containsKey("description"))
                                {
                                    FlagManager.addFlagDescription(name, map.get("description").toString());
                                }
                            }
                        }
                        else
                        {
                            //System.out.println(map.toString());
                        }
                    }
                }
            }
        }
    }
    
    private Flag getFlag(String name, Map map)
    {
        String type = map.get("type").toString();
        Flag newFlag = null;

        if (type.equalsIgnoreCase("integer"))
        {
            newFlag = new IntegerFlag(name);
        }
        else if (type.equalsIgnoreCase("double"))
        {
            newFlag = new DoubleFlag(name);
        }
        else if (type.equalsIgnoreCase("boolean")
                || type.equalsIgnoreCase("bool"))
        {
            newFlag = new BooleanFlag(name);
        }
        else if (type.equalsIgnoreCase("string"))
        {
            newFlag = new StringFlag(name);
        }
        else if (type.equalsIgnoreCase("command"))
        {
            newFlag = new CommandStringFlag(name);
        }
        else if (type.equalsIgnoreCase("location"))
        {
            newFlag = new CustomLocationFlag(name);
        }
        else if (type.equalsIgnoreCase("vector"))
        {
            newFlag = new CustomVectorFlag(name);
        }
        else if (type.equalsIgnoreCase("entitytype"))
        {
            newFlag = new EntityTypeFlag(name);
        }
        else if (type.equalsIgnoreCase("state")
                && map.containsKey("default"))
        {
            newFlag = new StateFlag(name, "allow".equalsIgnoreCase(map.get("default").toString()));
        }
        else if (type.equalsIgnoreCase("set")
                && map.containsKey("settype")
                && map.get("settype") instanceof Map)
        {
            Flag tmpFlag = getFlag("type_" + name, (Map) map.get("settype"));
            newFlag = new CustomSetFlag(name, tmpFlag);
        }
        else if (type.equalsIgnoreCase("enum")
                && map.containsKey("enumClass"))
        {
            try
            {
                Class clazz = Class.forName(map.get("enumClass").toString());
                if (clazz != null
                        && clazz.isEnum())
                {
                    newFlag = new EnumFlag(name, clazz);
                }
                else
                {
                    Bukkit.getLogger().log(Level.WARNING, "Could not add flag {0}. Not a valid enum class.", name);
                }
            }
            catch(Exception ex)
            {
                Bukkit.getLogger().log(Level.WARNING, "Could not add flag " + name + ".", ex);
            }
        }
        
        return newFlag;
    }
}
/*
 * Copyright (C) 2014 mewin<mewin001@hotmail.de>
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

import com.mewin.WGCustomFlags.util.ClassHacker;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.Flag;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.logging.Level;
import org.bukkit.Bukkit;

/**
 *
 * @author mewin<mewin001@hotmail.de>
 */
public class FlagManager
{
    private static WGCustomFlagsPlugin custInst = null;
    
    public static final HashMap<String, Flag> customFlags = new HashMap<String, Flag>();
    public static final HashMap<String, String> flagDescriptions = new HashMap<String, String>();

    public static void setWGCFInstance(WGCustomFlagsPlugin cust)
    {
        if (custInst == null)
        {
            custInst = cust;
        }
    }
    
    /**
     * retrieves a specific custom flag by its name
     * does not work with the default WorldGuard flags
     * @param name the name of the flag to retrieve
     * @return the flag with the given name or null if there is no flag with this name
     */
    public static Flag getCustomFlag(String name)
    {
        return customFlags.get(name);
    }
    
    /**
     * adds a custom flag and hooks it into WorldGuard
     * @param flag the flag to add
     */
    public synchronized static void addCustomFlag(Flag flag)
    {
        if (customFlags.containsKey(flag.getName())) {
            if (!customFlags.get(flag.getName()).getClass().equals(flag.getClass())) {
                Bukkit.getServer().getLogger().log(Level.WARNING, "Duplicate flag: {0}", flag.getName());
            }
        } else {
            customFlags.put(flag.getName(), flag);

            FlagManager.addWGFlag(flag);

            if (custInst.isFlagLogging())
            {
                Bukkit.getLogger().log(Level.INFO, "Added custom flag \"{0}\" to WorldGuard.", flag.getName());
            }

            custInst.loadAllWorlds();
        }
    }
    
    
    /**
     * adds a description for a flag that is displayed when the player uses the /flags command
     * @param flag the name of the flag to register the description for
     * @param description the description for the flag
     */
    public static void addFlagDescription(String flag, String description)
    {
        flagDescriptions.put(flag.toLowerCase(), description);
    }
    
    /**
     * retrieves the description for a flag that has been registered using addFlagDescription
     * @param flag the name of the flag to retrieve the description
     * @return the registered description or null if none has been registered
     */
    public static String getFlagDescription(String flag)
    {
        return flagDescriptions.get(flag.toLowerCase());
    }

    private synchronized static void addWGFlag(Flag<?> flag)
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
        catch(Exception ex)
        {
            Bukkit.getServer().getLogger().log(Level.WARNING, "Could not add flag {0} to WorldGuard", flag.getName());
        }

        for(int i = 0; i < DefaultFlag.getFlags().length; i++)
        {
            Flag<?> flag1 = DefaultFlag.getFlags()[i];
            if (flag1 == null) {
                throw new RuntimeException("Flag["+i+"] is null");
            }
        }
    }
    
    
    
    /**
     * adds flags for all public and static fields of a class that extend Flag
     * @param clazz the class that contains the flags
     * @throws java.lang.Exception if an exception occures while adding the flags
     */
    public static void addCustomFlags(Class clazz) throws Exception
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
}
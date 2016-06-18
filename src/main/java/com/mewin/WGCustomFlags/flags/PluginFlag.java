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

package com.mewin.WGCustomFlags.flags;

import com.mewin.WGCustomFlags.WGCustomFlagsPlugin;
import com.sk89q.worldedit.Location;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.FlagContext;
import com.sk89q.worldguard.protection.flags.InvalidFlagFormat;
import com.sk89q.worldguard.protection.flags.RegionGroup;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

/**
 *
 * @author mewin<mewin001@hotmail.de>
 */
public class PluginFlag extends CustomFlag<Plugin>
{

    public PluginFlag(String name)
    {
        super(name);
    }

    public PluginFlag(String name, RegionGroup rg)
    {
        super(name, rg);
    }

    @Override
    public Plugin loadFromDb(String str)
    {
        return unmarshal(str);
    }

    @Override
    public String saveToDb(Plugin o)
    {
        return (String) marshal(o);
    }


    @Override
    public Plugin parseInput(FlagContext fc) throws InvalidFlagFormat
    {
        WorldGuardPlugin plugin = WGCustomFlagsPlugin.wgPlugin;
        CommandSender sender = fc.getSender();
        String input = fc.getUserInput();
        
        Plugin plug = unmarshal(input);
        if (plug == null)
        {
            throw new InvalidFlagFormat("No plugin with this name found.");
        }
        else
        {
            return plug;
        }
    }

    @Override
    public Plugin unmarshal(Object o)
    {
         return Bukkit.getPluginManager().getPlugin(o.toString());
    }

    @Override
    public Object marshal(Plugin t)
    {
        return t.getName();
    }

}
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

import com.mewin.WGCustomFlags.flags.CustomSetFlag;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.EnumFlag;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.SetFlag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatTabCompleteEvent;

/**
 *
 * @author mewin<mewin001@hotmail.de>
 */
public class TabCompleteListener implements Listener
{
    private final Map<String, String> subCommands = new HashMap<String, String>();
    private WorldGuardPlugin wgPlugin;
    
    public TabCompleteListener(WorldGuardPlugin wgp)
    {
        this.wgPlugin = wgp;
    }
    
    @EventHandler
    public void onPlayerChatTabComplete(PlayerChatTabCompleteEvent e)
    {
        if (e.getChatMessage().startsWith("rg")
                || e.getChatMessage().startsWith("region"))
        {
            String[] split = e.getChatMessage().split(" ");
            String[] params = new String[split.length - 1];
            System.arraycopy(split, 1, params, 0, split.length - 1);
            List<String> comps = tabComplete(e.getPlayer(), split[0], params);
            if (comps != null)
            {
                e.getTabCompletions().clear();
                e.getTabCompletions().addAll(comps);
            }
        }
    }
    
    private void initSubCommands()
    {
        subCommands.put("addmember", "");
        subCommands.put("addowner", "");
        subCommands.put("claim", "claim");
        subCommands.put("define", "define");
        subCommands.put("flag", "");
        subCommands.put("info", "info");
        subCommands.put("list", "list");
        subCommands.put("redefine", "redefine");
        subCommands.put("remove", "");
        subCommands.put("removememeber", "");
        subCommands.put("select", "");
        subCommands.put("setparent", "");
        subCommands.put("setpriority", "");
        subCommands.put("teleport", "");
    }
    
    private List<String> tabComplete(CommandSender cs, String alias, String[] params)
    {
        List<String> comps = new ArrayList<String>();
        String cmdStart = "";
        
        if (!(cs instanceof Player))
        {
            return comps;
        }
        
        if (params.length < 2)
        {
            cmdStart = params[0];
            
            for(Map.Entry<String, String> entry : subCommands.entrySet())
            {
                String cmd = entry.getKey();
                String perm = entry.getValue();
                
                if (perm.equals("") || cs.hasPermission("worldguard.region." + perm))
                {
                    comps.add(cmd);
                }
            }
        }
        else if (params.length < 3)
        {
            cmdStart = params[1];
            if (params[0].equalsIgnoreCase("redefine")
                    || params[0].equalsIgnoreCase("select")
                    || params[0].equalsIgnoreCase("info")
                    || params[0].equalsIgnoreCase("i")
                    || params[0].equalsIgnoreCase("addowner")
                    || params[0].equalsIgnoreCase("addmember")
                    || params[0].equalsIgnoreCase("removemember")
                    || params[0].equalsIgnoreCase("removeowner")
                    || params[0].equalsIgnoreCase("remowner")
                    || params[0].equalsIgnoreCase("remmember")
                    || params[0].equalsIgnoreCase("removemem")
                    || params[0].equalsIgnoreCase("remmem")
                    || params[0].equalsIgnoreCase("flag")
                    || params[0].equalsIgnoreCase("f")
                    || params[0].equalsIgnoreCase("setpriority")
                    || params[0].equalsIgnoreCase("setparent")
                    || params[0].equalsIgnoreCase("remove"))
            {
                comps.addAll(completeRegionsForPlayer((Player) cs));
            }
        }
        else if (params.length < 4)
        {
            cmdStart = params[2];
            
            if (params[0].equalsIgnoreCase("addowner")
                    || params[0].equalsIgnoreCase("addmember")
                    || params[0].equalsIgnoreCase("removemember")
                    || params[0].equalsIgnoreCase("removeowner")
                    || params[0].equalsIgnoreCase("remowner")//
                    || params[0].equalsIgnoreCase("remmember")
                    || params[0].equalsIgnoreCase("removemem")
                    || params[0].equalsIgnoreCase("remmem"))
            {
                return null;
            }
        }
        else if (params[0].equalsIgnoreCase("setparent"))
        {
            comps.addAll(completeRegionsForPlayer((Player) cs));
        }
        else if (params[0].equalsIgnoreCase("flag")
                || params[0].equalsIgnoreCase("f"))
        {
            comps.addAll(completeFlagsForPlayer((Player) cs));
        }
        else if (params.length < 5 && "flag".startsWith(params[0].toLowerCase()))
        {
            cmdStart = params[3];
            
            comps.addAll(completeFlagValue(params[2], cmdStart));
        }
        
        cmdStart = cmdStart.toLowerCase();
        Iterator<String> itr = comps.iterator();
        while (itr.hasNext())
        {
            if(!itr.next().toLowerCase().startsWith(cmdStart))
            {
                itr.remove();
            }
        }
        
        return comps;
    }
    
    private List<String> completeRegionsForPlayer(Player player)
    {
        List<String> comps = new ArrayList<String>();
        RegionManager rm = wgPlugin.getRegionManager(player.getWorld());
        
        if (rm == null)
        {
            return comps; 
        }
        
        Map<String, ProtectedRegion> regions = rm.getRegions();
        
        if (player.isOp() || player.hasPermission("worldguard.region.list"))
        {
            for (Map.Entry<String, ProtectedRegion> entry : regions.entrySet())
            {
                comps.add(entry.getKey());
            }
        }
        else
        {
           for (Map.Entry<String, ProtectedRegion> entry : regions.entrySet())
            {
                ProtectedRegion region = entry.getValue();
                
                if (region.isOwner(player.getName()) || region.isMember(player.getName()))
                {
                    comps.add(entry.getKey());
                }
            } 
        }
        
        return comps;
    }
    
    private List<String> completeFlagsForPlayer(Player player)
    {
        List<String> comps = new ArrayList<String>();
        
        Flag[] flags = DefaultFlag.flagsList;
        
        if (player.hasPermission("worldguard.region.flag.flags.*"))
        {
            for (Flag flag : flags)
            {
                comps.add(flag.getName());
            }
        }
        else
        {
            for (Flag flag : flags)
            {
                if (player.hasPermission("worldguard.region.flag." + flag.getName() + ".*"))
                {
                    comps.add(flag.getName());
                }
            }
        }
        
        return comps;
    }
    
    private List<String> completeFlagValue(String flagName, String cmdStart)
    {
        Flag flag = null;
        List<String> comps = new ArrayList<String>();
        
        for (Flag f : DefaultFlag.flagsList)
        {
            if (f.getName().equalsIgnoreCase(flagName))
            {
                flag = f;
            }
        }
        
        if (flag == null)
        {
            return comps;
        }
        
        return completeFlagValue(flag, cmdStart);
    }
    
    private List<String> completeFlagValue(Flag flag, String cmdStart)
    {
        List<String> comps = new ArrayList<String>();
        
        if (flag instanceof StateFlag)
        {
            comps.add("ALLOW");
            comps.add("DENY");
        }
        else if (flag instanceof EnumFlag)
        {
            Class enumClass = (Class) getPrivateValue(flag, "enumClass");
            
            Enum[] values = getEnumValues(enumClass);
            
            for (Enum value : values)
            {
                comps.add(value.name());
            }
        }
        else if (flag instanceof SetFlag || flag instanceof CustomSetFlag)
        {
            String newStart = cmdStart;
            String before = "";
            List<String> newComps;
            Flag subFlag = (Flag) getPrivateValue(flag, "subFlag");
            
            if (newStart.indexOf(",") >= 0)
            {
                before = newStart.substring(0, newStart.lastIndexOf(",")) + ",";
                newStart = newStart.substring(newStart.lastIndexOf(",") + 1);
            }
            
            newComps = completeFlagValue(subFlag, newStart);
            
            for (String newComp : newComps)
            {
                comps.add(before + newComp);
            }
        }
        
        return comps;
    }
    
    private Object getPrivateValue(Object obj, String name)
    {
        try
        {
            Field field = obj.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(obj);
        }
        catch(Exception ex)
        {
            return null;
        }
    }
    
    private Enum[] getEnumValues(Class<? extends Enum> cls)
    {
        try {
            Method method = cls.getDeclaredMethod("values", new Class[0]);
            return (Enum[]) method.invoke(cls, new Object[0]);
        } catch (Exception ex)
        {
            return null;
        }
    }
}
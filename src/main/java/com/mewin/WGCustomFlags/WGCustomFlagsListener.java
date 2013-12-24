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

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.event.world.WorldUnloadEvent;

/**
 *
 * @author mewin <mewin001@hotmail.de>
 */
public class WGCustomFlagsListener implements Listener {
    private WGCustomFlagsPlugin plugin;

    public WGCustomFlagsListener(WGCustomFlagsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent e) {
        this.plugin.loadFlagsForWorld(e.getWorld());
    }

    @EventHandler
    public void onWorldInit(WorldInitEvent e) {
        this.plugin.loadFlagsForWorld(e.getWorld());
    }

    @EventHandler
    public void onWorldSave(WorldSaveEvent e) {
        String conf = plugin.getConf().getString("flag-saving", "save");
        String[] split = conf.split(",");
        for (String s : split)
        {
            if (s.trim().equalsIgnoreCase("save"))
            {
                this.plugin.saveFlagsForWorld(e.getWorld(), true);
                return;
            }
        }
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent e) {
        String conf = plugin.getConf().getString("flag-saving", "unload");
        String[] split = conf.split(",");
        for (String s : split)
        {
            if (s.trim().equalsIgnoreCase("unload"))
            {
                this.plugin.saveFlagsForWorld(e.getWorld(), false);
                return;
            }
        }
    }
    
    @EventHandler
    public void onServerCommand(ServerCommandEvent e)
    {
        String[] split = e.getCommand().toLowerCase().trim().split(" ");
        if (split.length > 1 && (split[0].equalsIgnoreCase("/wg") || split[0].equalsIgnoreCase("/worldguard"))
                && split[1].equalsIgnoreCase("reload") && e.getSender().hasPermission("worldguard.reload"))
        {
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, new Runnable()
            {
                @Override
                public void run()
                {
                    plugin.loadAllWorlds();
                }
            }, 10L); //give wg enough time to reload
        }
        else if ((split[0].equalsIgnoreCase("rg") || split[0].equalsIgnoreCase("region")) && split.length > 1 && split[1].equals("save")
                 && e.getSender().hasPermission("worldguard.region.save")) {
            if (split.length <= 2) {
                plugin.saveAllWorlds(true);
            } else {
                World w = plugin.getServer().getWorld(split[2]);

                if (w != null) {
                    plugin.saveFlagsForWorld(w, true);
                }
            }   
        } else if ((split[0].equalsIgnoreCase("rg") || split[0].equalsIgnoreCase("region")) && split.length > 1 && split[1].equals("load")
                && e.getSender().hasPermission("worldguard.region.save")) {
            if (split.length <= 2) {
                plugin.loadAllWorlds();
            } else {
                World w = plugin.getServer().getWorld(split[2]);

                if (w != null) {
                    plugin.loadFlagsForWorld(w);
                }
            }   
        }
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent e) {
        String[] split = e.getMessage().toLowerCase().trim().split(" ");
        if (split.length > 1 && (split[0].equalsIgnoreCase("/wg") || split[0].equalsIgnoreCase("/worldguard"))
                && split[1].equalsIgnoreCase("reload") && e.getPlayer().hasPermission("worldguard.reload"))
        {
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, new Runnable()
            {
                @Override
                public void run()
                {
                    plugin.loadAllWorlds();
                }
            }, 10L); //give wg enough time to reload
        }
        else if ((!split[0].equalsIgnoreCase("/rg") && !split[0].equalsIgnoreCase("/region")) || split.length < 2) {
            return;
        }

        boolean saveOnChange = false;
        String conf = plugin.getConf().getString("flag-saving", "");
        String[] split2 = conf.split(",");
        for (String s : split2)
        {
            if (s.trim().equalsIgnoreCase("change"))
            {
                saveOnChange = true;
            }
        }
        
        if (split[1].equals("save") && e.getPlayer().hasPermission("worldguard.region.save")) {
            if (split.length <= 2) {
                plugin.saveAllWorlds(true);
            } else {
                World w = plugin.getServer().getWorld(split[2]);

                if (w != null) {
                    plugin.saveFlagsForWorld(w, true);
                }
            }   
        } else if (split[1].equals("load") && e.getPlayer().hasPermission("worldguard.region.load")) {
            if (split.length <= 2) {
                plugin.loadAllWorlds();
            } else {
                World w = plugin.getServer().getWorld(split[2]);

                if (w != null) {
                    plugin.loadFlagsForWorld(w);
                }
            }   
        } else if (saveOnChange && split[1].equals("f") || split[1].equals("flag") && split.length >= 4
                && (e.getPlayer().hasPermission("worldguard.region.flag.flags.*")
                || e.getPlayer().hasPermission("worldguard.region.flag.flags." + split[3] + ".*"))) {
            final World w = e.getPlayer().getWorld();
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable()
            {
                @Override
                public void run()
                {
                    plugin.saveFlagsForWorld(w, true);
                }
            }, 2L); //let wg change the flag first
        }
    }
}
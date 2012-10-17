/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mewin.WGCustomFlags;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.event.world.WorldUnloadEvent;

/**
 *
 * @author mewin
 */
public class WGCustomFlagsListener implements Listener {
    private WGCustomFlagsPlugin plugin;
    
    public WGCustomFlagsListener(WGCustomFlagsPlugin plugin)
    {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onWorldLoad(WorldLoadEvent e)
    {
        this.plugin.loadFlagsForWorld(e.getWorld());
    }
    
    @EventHandler
    public void onWorldInit(WorldInitEvent e)
    {
        this.plugin.loadFlagsForWorld(e.getWorld());
    }
    
    @EventHandler
    public void onWorldSave(WorldSaveEvent e)
    {
        this.plugin.saveFlagsForWorld(e.getWorld());
    }
    
    @EventHandler
    public void onWorldUnload(WorldUnloadEvent e)
    {
        this.plugin.saveFlagsForWorld(e.getWorld());
    }
}

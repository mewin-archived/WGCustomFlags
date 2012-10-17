/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mewin.WGCustomFlags.data;

import com.sk89q.worldguard.protection.flags.Flag;
import java.util.HashMap;
import org.bukkit.World;

/**
 *
 * @author mewin
 */
public interface FlagSaveHandler {
    
    public abstract void saveFlagsForWorld(World world);
    
    public abstract void loadFlagsForWorld(World world);
}

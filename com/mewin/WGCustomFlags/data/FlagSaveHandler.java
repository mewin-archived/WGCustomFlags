package com.mewin.WGCustomFlags.data;

import org.bukkit.World;

/**
 *
 * @author mewin
 */
public interface FlagSaveHandler {

    public abstract void saveFlagsForWorld(World world);

    public abstract void loadFlagsForWorld(World world);
}

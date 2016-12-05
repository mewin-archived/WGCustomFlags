/*
 * Copyright (C) 2012 patrick
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
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.Location;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.FlagContext;
import com.sk89q.worldguard.protection.flags.InvalidFlagFormat;
import com.sk89q.worldguard.protection.flags.RegionGroup;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 *
 * @author patrick
 */
public class CustomLocationFlag extends CustomFlag<Location> {
    public CustomLocationFlag(String name, RegionGroup defaultGroup) {
        super(name, defaultGroup);
    }

    public CustomLocationFlag(String name) {
        super(name);
    }

    @Override
    public Location parseInput(FlagContext flagContext) throws InvalidFlagFormat {
        String input = flagContext.getUserInput();
        WorldGuardPlugin plugin = WGCustomFlagsPlugin.wgPlugin;
        CommandSender sender = flagContext.getSender();
        input = input.trim();

        final Player player;
        try {
            player = plugin.checkPlayer(sender);
        } catch (CommandException e) {
            throw new InvalidFlagFormat(e.getMessage());
        }

        if ("here".equalsIgnoreCase(input)) {
            return BukkitUtil.toLocation(player.getLocation());
        } else if ("none".equalsIgnoreCase(input)) {
            return null;
        } else {
            String[] split = input.split(",");
            if (split.length >= 3) {
                try {
                    final World world = player.getWorld();
                    final double x = Double.parseDouble(split[0]);
                    final double y = Double.parseDouble(split[1]);
                    final double z = Double.parseDouble(split[2]);
                    final float yaw = split.length < 4 ? 0 : Float.parseFloat(split[3]);
                    final float pitch = split.length < 5 ? 0 : Float.parseFloat(split[4]);

                    return new Location(
                            BukkitUtil.getLocalWorld(world),
                            new Vector(
                                    x,
                                    y,
                                    z
                            ),
                            yaw, pitch
                    );
                } catch (NumberFormatException e) { }
            }

            throw new InvalidFlagFormat("Expected 'here' or x,y,z.");
        }
    }

    @Override
    public Location unmarshal(Object o) {
        if (o instanceof String) {
            return stringToLocation((String) o);
        }

        return null;
    }

    @Override
    public Object marshal(Location o) {
        return locationToString(o);
    }

    private double toNumber(Object o) {
        if (o instanceof Number) {
            return ((Number) o).doubleValue();
        } else {
            return 0;
        }

    }

    @Override
    public Location loadFromDb(String str) {
        return this.unmarshal(str);
    }

    @Override
    public String saveToDb(Location o) {
        return (String) this.marshal(o);
    }

    private Location stringToLocation(String string) {
        String[] split = string.split("\\|");

            if (split.length < 6) {
                return null;
            }

            return new Location(new BukkitWorld(Bukkit.getServer().getWorld(split[0])), 
                    new Vector(Double.valueOf(split[1]), Double.valueOf(split[2]), Double.valueOf(split[3])), 
                    Float.valueOf(split[4]), Float.valueOf(split[5]));
    }

    private String locationToString(Location loc) {
        String ret = "";

        ret += loc.getWorld().getName() + "|" + Double.toString(shortenDouble(loc.getPosition().getX(), 2)) + "|";
        ret += Double.toString(shortenDouble(loc.getPosition().getY(), 2)) + "|" + Double.toString(shortenDouble(loc.getPosition().getZ(), 2));
        ret += "|" + Float.toString(shortenFloat(loc.getYaw(), 2)) + "|" + Float.toString(shortenFloat(loc.getPitch(), 2));

        return ret;
    }

    private float shortenFloat(float f, int dig) {
        return (float) (Math.round(f * Math.pow(10, dig)) / Math.pow(10, dig));
    }

    private double shortenDouble(double d, int dig) {
        return Math.round(d * Math.pow(10, dig)) / Math.pow(10, dig);
    }
}

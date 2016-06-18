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
package com.mewin.WGCustomFlags.flags;

import com.mewin.WGCustomFlags.WGCustomFlagsPlugin;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.Location;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.FlagContext;
import com.sk89q.worldguard.protection.flags.InvalidFlagFormat;
import com.sk89q.worldguard.protection.flags.RegionGroup;
import org.bukkit.command.CommandSender;

/**
 *
 * @author mewin <mewin001@hotmail.de>
 */
public class CustomVectorFlag extends CustomFlag<Vector> {
    public CustomVectorFlag(String name, RegionGroup defaultGroup) {
        super(name, defaultGroup);
    }

    public CustomVectorFlag(String name) {
        super(name);
    }

    @Override
    public Vector parseInput(FlagContext fc) throws InvalidFlagFormat
    {
        WorldGuardPlugin plugin = WGCustomFlagsPlugin.wgPlugin;
        CommandSender sender = fc.getSender();
        String input = fc.getUserInput();
        input = input.trim();

        if ("here".equalsIgnoreCase(input)) {
            try {
                return com.sk89q.worldguard.bukkit.BukkitUtil.toVector(plugin.checkPlayer(sender).getLocation());
            } catch (CommandException e) {
                throw new InvalidFlagFormat(e.getMessage());
            }
        } else {
            String[] split = input.split(",");
            if (split.length == 3) {
                try {
                    return new Vector(
                            Double.parseDouble(split[0]),
                            Double.parseDouble(split[1]),
                            Double.parseDouble(split[2])
                    );
                } catch (NumberFormatException e) { }
            }

            throw new InvalidFlagFormat("Expected 'here' or x,y,z.");
        }
    }

    @Override
    public Vector unmarshal(Object o) {
        if (o instanceof String) {
            return stringToVector((String) o);
        }

        return null;
    }

    @Override
    public Object marshal(Vector o) {
        return vectorToString(o);
    }

    private double toNumber(Object o) {
        if (o instanceof Number) {
            return ((Number) o).doubleValue();
        } else {
            return 0;
        }
    }

    @Override
    public Vector loadFromDb(String str) {
        return this.unmarshal(str);
    }

    @Override
    public String saveToDb(Vector o) {
        return (String) this.marshal(o);
    }

    private Vector stringToVector(String string) {
        String[] split = string.split("\\|");

            if (split.length < 3) {
                return null;
            }

            return new Vector(Double.valueOf(split[0]), Double.valueOf(split[1]), Double.valueOf(split[2]));
    }

    private String vectorToString(Vector vec) {
        String ret = "";

        ret += Double.toString(shortenDouble(vec.getX(), 2)) + "|";
        ret += Double.toString(shortenDouble(vec.getY(), 2)) + "|";
        ret += Double.toString(shortenDouble(vec.getZ(), 2));

        return ret;
    }

    private double shortenDouble(double d, int dig) {
        return Math.round(d * Math.pow(10, dig)) / Math.pow(10, dig);
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mewin.WGCustomFlags.flags;

import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.InvalidFlagFormat;
import com.sk89q.worldguard.protection.flags.RegionGroup;
import org.bukkit.command.CommandSender;

/**
 *
 * @author mewin
 */
public class CustomVectorFlag extends CustomFlag<Vector> {
    public CustomVectorFlag(String name, RegionGroup defaultGroup) {
        super(name, defaultGroup);
    }

    public CustomVectorFlag(String name) {
        super(name);
    }

    @Override
    public Vector parseInput(WorldGuardPlugin plugin, CommandSender sender, String input) throws InvalidFlagFormat {
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
                } catch (NumberFormatException e) {
                }
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
    
    private Vector stringToVector(String string)
    {
        String[] split = string.split("\\|");

            if (split.length < 3)
            {
                return null;
            }

            return new Vector(Double.valueOf(split[0]), Double.valueOf(split[1]), Double.valueOf(split[2]));
    }
    
    private String vectorToString(Vector vec)
    {
        String ret = "";
        
        ret += Double.toString(shortenDouble(vec.getX(), 2)) + "|";
        ret += Double.toString(shortenDouble(vec.getY(), 2)) + "|";
        ret += Double.toString(shortenDouble(vec.getZ(), 2));
        
        return ret;
    }
    
    private double shortenDouble(double d, int dig)
    {
        return Math.round(d * Math.pow(10, dig)) / Math.pow(10, dig);
    }
}

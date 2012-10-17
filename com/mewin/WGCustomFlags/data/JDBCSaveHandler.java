/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mewin.WGCustomFlags.data;

import com.mewin.WGCustomFlags.WGCustomFlagsPlugin;
import com.mewin.WGCustomFlags.flags.CustomFlag;
import com.mewin.WGCustomFlags.util.ClassHacker;
import com.sk89q.worldedit.Location;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldguard.protection.flags.BooleanFlag;
import com.sk89q.worldguard.protection.flags.CommandStringFlag;
import com.sk89q.worldguard.protection.flags.DoubleFlag;
import com.sk89q.worldguard.protection.flags.EnumFlag;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.IntegerFlag;
import com.sk89q.worldguard.protection.flags.LocationFlag;
import com.sk89q.worldguard.protection.flags.SetFlag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.flags.VectorFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.World;

/**
 *
 * @author mewin
 */
public class JDBCSaveHandler implements FlagSaveHandler {
    private Connection connection;
    private WGCustomFlagsPlugin plugin;
    
    public JDBCSaveHandler(String dns, String username, String password, WGCustomFlagsPlugin plugin)
    {
        this.plugin = plugin;
        try {
            connection = DriverManager.getConnection(dns, username, password);
            
            if (connection.isReadOnly())
            {
                plugin.getLogger().severe("Database connection is read-only!");
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not connect to database.", ex);
        }
    }
    
    @Override
    public void saveFlagsForWorld(World world)
    {
        RegionManager regionManager = WGCustomFlagsPlugin.wgPlugin.getRegionManager(world);
        if (regionManager == null) 
        {
            plugin.getLogger().info("Regions not activated, no flags saved.");
            return;
        }
        Iterator<Entry<String, ProtectedRegion>> itr = regionManager.getRegions().entrySet().iterator();
        
        
                        
        Statement st;
        try {
            st = connection.createStatement();
                        
            st.executeQuery("TRUNCATE TABLE worldflags");
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error truncating worldflags", ex);
        }
        
        int flagCounter = 0;
        int regionCounter = 0;
        
        while(itr.hasNext())
        {
            Entry<String, ProtectedRegion> entry = itr.next();
            ProtectedRegion region = entry.getValue();
            Iterator<Entry<Flag<?>, Object>> itr2 = region.getFlags().entrySet().iterator();
            
            regionCounter++;
            
            while(itr2.hasNext())
            {
                Entry<Flag<?>, Object> entry2 = itr2.next();
                Flag<?> flag = entry2.getKey();
                Object value = entry2.getValue();
                
                if (WGCustomFlagsPlugin.customFlags.containsKey(flag.getName()))
                {
                    try {
                        String nextSql = "INSERT INTO worldflags(world, region, flagName, flagValue)" +
                                      "VALUES('" + world.getName() + "', '" + region.getId() + "', '" + flag.getName() + "', '";
                        String next = getFlagValue(flag, value);
                        
                        if(next == null)
                        {
                            plugin.getLogger().log(Level.WARNING, "Value for flag {0} off region {1} is null.", new Object[]{flag.getName(), region.getId()});
                            continue;
                        }
                        
                        nextSql += next;
                        
                        nextSql += "')";
                        
                        connection.clearWarnings();
                        
                        Statement st2 = connection.createStatement();
                        if (!st2.execute(nextSql))
                        {
                            SQLWarning warning = connection.getWarnings();
                            
                            if (warning == null)
                            {
                                warning = st2.getWarnings();
                            }
                            
                            if (warning != null)
                            {
                                throw warning;
                            }
                            else
                            {
                                plugin.getLogger().severe("An unknown error occured.");
                            }
                        }
                        else
                        {
                            flagCounter++;
                        }
                    } catch (SQLException ex) {
                        plugin.getLogger().log(Level.SEVERE, "Could not save flags for world" + world.getName(), ex);
                    }
                }
            }
        }
        
        plugin.getLogger().log(Level.INFO, "{0} flags saved for {1} regions", new Object[]{flagCounter, regionCounter});
    }
    
    private String getFlagValue(Flag flag, Object value)
    {
        String nextSql = "";
        if (flag instanceof BooleanFlag)
        {
            nextSql += Boolean.toString((Boolean) value);
        }
        else if(flag instanceof CommandStringFlag || flag instanceof StringFlag)
        {
            nextSql += (String) value;
        }
        else if (flag instanceof DoubleFlag)
        {
            nextSql += Double.toString((Double) value);
        }
        else if (flag instanceof EnumFlag)
        {
            nextSql += ((Enum) value).name();
        }
        else if (flag instanceof IntegerFlag)
        {
            nextSql += Integer.toString((Integer) value);
        }
        else if (flag instanceof LocationFlag)
        {
            Location loc = (Location) value;

            nextSql += loc.getWorld().getName() + "|" + Double.toString(shortenDouble(loc.getPosition().getX(), 2)) + "|";
            nextSql += Double.toString(shortenDouble(loc.getPosition().getY(), 2)) + "|" + Double.toString(shortenDouble(loc.getPosition().getZ(), 2));
            nextSql += "|" + Float.toString(shortenFloat(loc.getYaw(), 2)) + "|" + Float.toString(shortenFloat(loc.getPitch(), 2));
        }
        else if (flag instanceof SetFlag)
        {
            Flag subFlag = (Flag) ClassHacker.getPrivateValue((SetFlag) flag, "subFlag");
            
            Set<Object> set = (Set<Object>) value;
            Iterator<Object> itr = set.iterator();
            
            while(itr.hasNext())
            {
                String str = getFlagValue(subFlag, itr.next());
                
                str = str.replace("\\", "\\\\").replace(";", "\\;");
                
                nextSql += str;
                
                if (itr.hasNext())
                {
                    nextSql += ";";
                }
            }
        }
        else if (flag instanceof StateFlag)
        {
            nextSql += ((StateFlag.State) value).name();
        }
        else if (flag instanceof VectorFlag)
        {
            Vector vec = (Vector) value;

            nextSql += Double.toString(shortenDouble(vec.getX(), 2)) + "|";
            nextSql += Double.toString(shortenDouble(vec.getY(), 2)) + "|";
            nextSql += Double.toString(shortenDouble(vec.getZ(), 2));
        }
        else if (flag instanceof CustomFlag)
        {
            nextSql += ((CustomFlag) flag).saveToDb(value);
        }
        else
        {
            System.out.println("This should not happen either.");
            return null;
        }
        
        return nextSql;
    }
    
    @Override
    public void loadFlagsForWorld(World world)
    {
        RegionManager regionManager = WGCustomFlagsPlugin.wgPlugin.getRegionManager(world);
        
        if (regionManager == null)
        {
            return;
        }
        try {
            CallableStatement st = connection.prepareCall("SELECT * FROM worldflags WHERE world = '" + world.getName() + "'");
            
            ResultSet rs = st.executeQuery();
            
            while(rs.next())
            {
                Flag flag = WGCustomFlagsPlugin.customFlags.get(rs.getString("flagName"));
                ProtectedRegion region = regionManager.getRegion(rs.getString("region"));
                
                if (flag == null || region == null)
                {
                    //System.out.println("Error loading flags from db");
                    continue;
                }
                
                String value = rs.getString("flagValue");
                
                setRegionFlag(region, flag, value);
            }
        } catch (SQLException | NumberFormatException ex) {
            Logger.getLogger(JDBCSaveHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void setRegionFlag(ProtectedRegion region, Flag flag, String value)
    {
        Object val = getFlagValue(region, flag, value);
        
        if (val != null)
        {
            region.setFlag(flag, val);
        }
    }
    
    private Object getFlagValue(ProtectedRegion region, Flag flag, String value)
    {
        if (flag instanceof StringFlag || flag instanceof CommandStringFlag)
        {
            return value;
        }
        else if (flag instanceof EnumFlag)
        {
            Class enumClass = (Class<?>) ClassHacker.getPrivateValue(flag, "enumClass");

            return Enum.valueOf(enumClass, value);
        }
        else if (flag instanceof BooleanFlag)
        {
            return Boolean.parseBoolean(value);
        }
        else if (flag instanceof CustomFlag)
        {
            return ((CustomFlag) flag).loadFromDb(value);
        }
        else if (flag instanceof DoubleFlag)
        {
            return Double.parseDouble(value);
        }
        else if (flag instanceof IntegerFlag)
        {
            return Integer.valueOf(value);
        }
        else if (flag instanceof LocationFlag)
        {
            String[] split = value.split("\\|");

            if (split.length < 6)
            {
                return null;
            }

            region.setFlag(flag, new Location(new BukkitWorld(plugin.getServer().getWorld(split[0])), 
                    new Vector(Double.valueOf(split[1]), Double.valueOf(split[2]), Double.valueOf(split[3])), 
                    Float.valueOf(split[4]), Float.valueOf(split[5])));
        }
        else if (flag instanceof SetFlag)
        {
            Pattern p = Pattern.compile("[^\\\\](\\\\\\\\)*;");
            Matcher matcher = p.matcher(value);
            Flag subFlag = (Flag) ClassHacker.getPrivateValue((SetFlag) flag, "subFlag");
            HashSet<Object> splits = new HashSet<>();
            
            while(matcher.find())
            {                
                splits.add(getFlagValue(region, subFlag, value.substring(0, matcher.end() - 1)));
                value = value.substring(matcher.end());
                
                matcher = p.matcher(value);
            }
            
            splits.add(getFlagValue(region, subFlag, value));
            
            return splits;
        }
        else if (flag instanceof StateFlag)
        {
            region.setFlag(flag, StateFlag.State.valueOf(value));
        }
        else if (flag instanceof VectorFlag)
        {
            String[] split = value.split("\\|");

            if (split.length < 3)
            {
                return null;
            }

            region.setFlag(flag, new Vector(Double.valueOf(split[0]), Double.valueOf(split[1]), Double.valueOf(split[2])));
        }
        
        return null;
    }
    
    private float shortenFloat(float f, int dig)
    {
        return (float) (Math.round(f * Math.pow(10, dig)) / Math.pow(10, dig));
    }
    
    private double shortenDouble(double d, int dig)
    {
        return Math.round(d * Math.pow(10, dig)) / Math.pow(10, dig);
    }
}

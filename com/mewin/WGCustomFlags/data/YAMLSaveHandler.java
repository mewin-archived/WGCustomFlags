/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mewin.WGCustomFlags.data;

import com.mewin.WGCustomFlags.WGCustomFlagsPlugin;
import com.mewin.WGCustomFlags.flags.CustomFlag;
import com.mewin.WGCustomFlags.util.ClassHacker;
import com.sk89q.util.yaml.YAMLFormat;
import com.sk89q.util.yaml.YAMLNode;
import com.sk89q.util.yaml.YAMLProcessor;
import com.sk89q.worldedit.Location;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.EnumFlag;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.LocationFlag;
import com.sk89q.worldguard.protection.flags.SetFlag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.VectorFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.World;

/**
 *
 * @author mewin
 */
public class YAMLSaveHandler implements FlagSaveHandler {
    private WGCustomFlagsPlugin plugin;
    private WorldGuardPlugin wgPlugin;
    
    public YAMLSaveHandler(WGCustomFlagsPlugin plugin, WorldGuardPlugin wgPlugin)
    {
        this.wgPlugin = wgPlugin;
    }
    
    @Override
    public void loadFlagsForWorld(World world)
    {
        YAMLProcessor worldConfig = new YAMLProcessor(new File(wgPlugin.getDataFolder(), "worlds/" + world.getName() + "/customFlags.yml"), true, YAMLFormat.EXTENDED);
        try {
            worldConfig.load();
        } catch (IOException ex) {
            System.out.println("No configuration found for world " + world.getName() + ". Will be created when saved.");
            return;
        }

        RegionManager regionManager = wgPlugin.getRegionManager(world);
        if (regionManager == null)
        {
            return;
        }
        List<YAMLNode> nodeList = worldConfig.getNodeList("regions", null);

        if(nodeList != null)
        {
            Iterator<YAMLNode> itr = nodeList.iterator();

            while(itr.hasNext())
            {
                YAMLNode node = itr.next();

                String regionName = node.getString("region", null);

                if(regionName == null)
                {
                    System.out.println("region name is null");
                    continue;
                }

                ProtectedRegion region = regionManager.getRegion(regionName);

                YAMLNode flags = node.getNode("flags");

                if(flags == null)
                {
                    System.out.println("flags is null");
                    continue;
                }

                Iterator<Map.Entry<String, Flag>> itr2 = WGCustomFlagsPlugin.customFlags.entrySet().iterator();

                while(itr2.hasNext())
                {
                    Map.Entry<String, Flag> next = itr2.next();
                    Flag flag = next.getValue();
                    Object value = castValue(flag, flags.getProperty(next.getKey()));


                    if(value == null)
                    {
                        //System.out.println("Value is null");
                        continue;
                    }


                    region.setFlag(flag, value);
                }
            }
        }
    }
    
    private Object castValue(Flag flag, Object value)
    {
        
        if (value == null)
        {
            return null;
        }
        
        if (flag instanceof StateFlag)
        {
            System.out.print("StateFlag");
            value = StateFlag.State.valueOf((String) value);
        }
        
        if (flag instanceof EnumFlag)
        {
            Class enumClass = (Class<?>) ClassHacker.getPrivateValue(flag, "enumClass");
            value = Enum.valueOf(enumClass, (String) value);
        }
        
        if (flag instanceof LocationFlag)
        {
            YAMLNode nd = new YAMLNode((HashMap<String, Object>) value, true);
            
            value = new Location(new BukkitWorld(plugin.getServer().getWorld(nd.getString("world"))),
                    new Vector(nd.getDouble("x"), nd.getDouble("y"), nd.getDouble("z")),
                    nd.getDouble("yaw").floatValue(), (Float) nd.getDouble("pitch").floatValue());
        }
        
        if (flag instanceof SetFlag)
        {
            List<Object> list = (List<Object>) value;
            
            Flag subFlag = (Flag) ClassHacker.getPrivateValue((SetFlag) flag, "subFlag");
            
            value = new HashSet<>();
            
            Iterator<Object> itr = list.iterator();
            
            while(itr.hasNext())
            {
                ((HashSet<Object>) value).add(castValue(subFlag, itr.next()));
            }
        }
        
        if (flag instanceof VectorFlag)
        {
            YAMLNode nd = new YAMLNode((HashMap<String, Object>) value, true);
            
            value = new Vector(nd.getDouble("x"), nd.getDouble("y"), nd.getDouble("z"));
        }
        
        if (flag instanceof CustomFlag)
        {
            value = ((CustomFlag) flag).fromYAML(value);
        }
        
        return value;
    }
    
    public void saveAllWorlds()
    {
        Iterator<World> itr = plugin.getServer().getWorlds().iterator();
        
        while(itr.hasNext())
        {
            saveFlagsForWorld(itr.next());
        }
    }
    
    @Override
    public void saveFlagsForWorld(World world)
    {
        YAMLProcessor worldConfig = new YAMLProcessor(new File(wgPlugin.getDataFolder(), "worlds/" + world.getName() + "/customFlags.yml"), true, YAMLFormat.EXTENDED);
        RegionManager regionManager = wgPlugin.getRegionManager(world);
        if (regionManager == null)
        {
            return;
        }
        Map<String, ProtectedRegion> regions = regionManager.getRegions();
        Iterator<Map.Entry<String, ProtectedRegion>> itr = regions.entrySet().iterator();

        ArrayList<Object> regionList = new ArrayList<>();

        while(itr.hasNext())
        {
            Map.Entry<String, ProtectedRegion> entry = itr.next();
            ProtectedRegion region = entry.getValue();
            String regionName = entry.getKey();
            Map<Flag<?>, Object> flags = region.getFlags();
            Iterator<Map.Entry<Flag<?>, Object>> itr2 = flags.entrySet().iterator();

            HashMap<String, Object> values = new HashMap<>();

            while(itr2.hasNext())
            {
                Map.Entry<Flag<?>, Object> regionFlag = itr2.next();

                Flag<?> flag = regionFlag.getKey();

                Object value = valueForFlag(flag, region);

                if (WGCustomFlagsPlugin.customFlags.containsKey(flag.getName()))
                {
                    values.put(flag.getName(), value);
                }
            }

            if (values.size() < 1)
            {
                continue;
            }

            HashMap<String, Object> flagMap = new HashMap<>();

            flagMap.put("region", regionName);
            flagMap.put("flags", values);

            regionList.add(flagMap);
        }


        worldConfig.setProperty("regions", regionList);

        if(!worldConfig.save())
        {
            System.out.println("Failed to save config for world " + world.getName());
        }
    }
    
    
    
    private Object valueForFlag(Flag flag, ProtectedRegion region)
    {
        return valueForFlag(flag, region.getFlag(flag));
    }
    
    private Object valueForFlag(Flag flag, Object value)
    {
        
        if (flag instanceof EnumFlag || flag instanceof StateFlag)
        {
            value = ((Enum) value).name();
        }
        
        if (flag instanceof LocationFlag)
        {
            Location loc = (Location) value;
            
            HashMap<String, Object> locMap = new HashMap<>();
            
            locMap.put("world", loc.getWorld().getName());
            locMap.put("x", loc.getPosition().getX());
            locMap.put("y", loc.getPosition().getY());
            locMap.put("z", loc.getPosition().getZ());
            locMap.put("yaw", loc.getYaw());
            locMap.put("pitch", loc.getPitch());
            
            value = locMap;
        }
        
        if (flag instanceof VectorFlag)
        {
            Vector vec = (Vector) value;
            HashMap<String, Object> vecMap = new HashMap<>();
            
            vecMap.put("x", vec.getX());
            vecMap.put("y", vec.getY());
            vecMap.put("z", vec.getZ());
            
            value = vecMap;
        }
        
        if (flag instanceof SetFlag)
        {
            Flag subFlag = (Flag) ClassHacker.getPrivateValue((SetFlag) flag, "subFlag");
            
            Set<?> set = (Set<?>) value;
            value = new ArrayList<>();
            
            Iterator<?> itr = set.iterator();
            
            while(itr.hasNext())
            {
                Object obj = itr.next();
                
                ((ArrayList) value).add(valueForFlag(subFlag, obj));
            }
        }
        
        if (flag instanceof CustomFlag)
        {
            value = ((CustomFlag) flag).getYAML(value);
        }
        
        return value;
    }
    
   
}

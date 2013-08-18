package com.mewin.util;

import com.mewin.WGCustomFlags.flags.CustomSetFlag;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.bukkit.Location;

/**
 *
 * @author mewin<mewin001@hotmail.de>
 */
public final class Util {
    public static boolean flagAllowedAtLocation(WorldGuardPlugin wgp, Object flagValue, Location loc, CustomSetFlag allowFlag, CustomSetFlag denyFlag, Object anyValue) {
        RegionManager rm = wgp.getRegionManager(loc.getWorld());
        if (rm == null) {
            return true;
        }
        ApplicableRegionSet regions = rm.getApplicableRegions(loc);
        Iterator<ProtectedRegion> itr = regions.iterator();
        Map<ProtectedRegion, Boolean> regionsToCheck = new HashMap<ProtectedRegion, Boolean>();
        Set<ProtectedRegion> ignoredRegions = new HashSet<ProtectedRegion>();
        
        while(itr.hasNext()) {
            ProtectedRegion region = itr.next();
            
            if (ignoredRegions.contains(region)) {
                continue;
            }
            
            Object allowed = flagAllowedInRegion(region, flagValue, allowFlag, denyFlag, anyValue);
            
            if (allowed != null) {
                ProtectedRegion parent = region.getParent();
                
                while(parent != null) {
                    ignoredRegions.add(parent);
                    
                    parent = parent.getParent();
                }
                
                regionsToCheck.put(region, (Boolean) allowed);
            }
        }
        
        if (regionsToCheck.size() >= 1) {
            Iterator<Map.Entry<ProtectedRegion, Boolean>> itr2 = regionsToCheck.entrySet().iterator();
            int minPriority = Integer.MIN_VALUE;
            boolean returnValue = false;
            
            while(itr2.hasNext()) {
                Map.Entry<ProtectedRegion, Boolean> entry = itr2.next();
                
                ProtectedRegion region = entry.getKey();
                boolean value = entry.getValue();
                
                if (ignoredRegions.contains(region)) {
                    continue;
                }
                
                if (region.getPriority() < minPriority || region.getPriority() == minPriority && !value)
                {
                    continue;
                }
                
                minPriority = region.getPriority();
                returnValue = value;
            }
            
            return returnValue;
        } else {
            Object allowed = flagAllowedInRegion(rm.getRegion("__global__"), flagValue, allowFlag, denyFlag, anyValue);
            
            if (allowed != null) {
                return (Boolean) allowed;
            } else {
                return true;
            }
        }
    }
    
    public static <T> T getFlagValue(WorldGuardPlugin wgp, Location loc, Flag<T> flag)
    {
        RegionManager rm = wgp.getRegionManager(loc.getWorld());
        if (rm == null)
        {
            return null;
        }
        ApplicableRegionSet regions = rm.getApplicableRegions(loc);
        Iterator<ProtectedRegion> itr = regions.iterator();
        Map<ProtectedRegion, T> regionsToCheck = new HashMap<ProtectedRegion, T>();
        Set<ProtectedRegion> ignoredRegions = new HashSet<ProtectedRegion>();
        
        while(itr.hasNext()) {
            ProtectedRegion region = itr.next();
            
            if (ignoredRegions.contains(region) 
                    || region.getFlag(flag) == null) {
                continue;
            }
            
            ProtectedRegion parent = region.getParent();
            
            while(parent != null) {
                ignoredRegions.add(parent);

                parent = parent.getParent();
            }

            regionsToCheck.put(region, (T) region.getFlag(flag));
        }
        
        if (regionsToCheck.size() >= 1) {
            Iterator<Map.Entry<ProtectedRegion, T>> itr2 = regionsToCheck.entrySet().iterator();
            int minPriority = Integer.MIN_VALUE;
            T returnValue = null;
            
            while(itr2.hasNext()) {
                Map.Entry<ProtectedRegion, T> entry = itr2.next();
                
                ProtectedRegion region = entry.getKey();
                T value = entry.getValue();
                
                if (ignoredRegions.contains(region)) {
                    continue;
                }
                
                if (region.getPriority() < minPriority || region.getPriority() == minPriority)
                {
                    continue;
                }
                
                minPriority = region.getPriority();
                returnValue = value;
            }
            
            return returnValue;
        } else {
            ProtectedRegion global = rm.getRegion("__global__");
            if (rm == null || global == null)
            {
                return null;
            }
            T value = global.getFlag(flag);
            
            return value;
        }
    }
    
    public static Object flagAllowedInRegion(ProtectedRegion region, Object value, CustomSetFlag allowFlag, CustomSetFlag denyFlag, Object anyValue) {
        if (region == null)
        {
            return true;
        }
        else
        {
            HashSet<Object> allowedValues = (HashSet<Object>) region.getFlag(allowFlag);
            HashSet<Object> blockedValues = (HashSet<Object>) region.getFlag(denyFlag);

            if (allowedValues != null && (allowedValues.contains(value) || allowedValues.contains(anyValue))) {
                return true;
            }
            else if(blockedValues != null && (blockedValues.contains(value) || blockedValues.contains(anyValue))) {
                return false;
            } else {
                return null;
            }
        }
    }
}

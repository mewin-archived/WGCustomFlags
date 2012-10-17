/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mewin.WGCustomFlags.flags;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.InvalidFlagFormat;
import com.sk89q.worldguard.protection.flags.RegionGroup;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.command.CommandSender;

/**
 *
 * @author patrick
 */
public class CustomSetFlag<T> extends CustomFlag<Set<T>> {
    
    private Flag<T> subFlag;

    public CustomSetFlag(String name, RegionGroup defaultGroup, Flag<T> subFlag) {
        super(name, defaultGroup);
        this.subFlag = subFlag;
    }

    public CustomSetFlag(String name, Flag<T> subFlag) {
        super(name);
        this.subFlag = subFlag;
    }

    @Override
    public Set<T> parseInput(WorldGuardPlugin plugin, CommandSender sender,
            String input) throws InvalidFlagFormat {
        Set<T> items = new HashSet<T>();

        for (String str : input.split(",")) {
            items.add(subFlag.parseInput(plugin, sender, str.trim()));
        }

        return new HashSet<>(items);
    }

    @Override
    public Set<T> unmarshal(Object ob) {
        Object o = stringToList((String) ob);
        if (o instanceof Collection<?>) {
            Collection<?> collection = (Collection<?>) o;
            Set<T> items = new HashSet<T>();

            for (Object sub : collection) {
                T item = subFlag.unmarshal(sub);
                if (item != null) {
                    items.add(item);
                }
            }

            return items;
        } else {
            return null;
        }
    }

    @Override
    public Object marshal(Set<T> o) {
        List<Object> list = new ArrayList<>();
        for (T item : o) {
            list.add(subFlag.marshal(item));
        }

        return listToString(list);
    }

    @Override
    public Set<T> loadFromDb(String str) {
        return (Set<T>) this.unmarshal(str);
    }

    @Override
    public String saveToDb(Set<T> o) {
        return (String) this.marshal(o);
    }
    
    private String listToString(List<Object> list)
    {
        String str = "";
        Iterator<Object> itr = list.iterator();
        
        while(itr.hasNext())
        {
            str += String.valueOf(itr.next()).replace("\\", "\\\\").replace(";", "\\;") + ";";
        }
        
        return str;
    }
    
    private List<Object> stringToList(String string)
    {
        ArrayList<Object> list = new ArrayList<>();
        Pattern pattern = Pattern.compile("[^\\\\](\\\\\\\\)*;");
        
        Matcher matcher = pattern.matcher(string);
        while(matcher.find())
        {
            int pos = matcher.end();
            
            list.add(string.substring(0, pos - 1));
            
            string = string.substring(pos);
            
            matcher = pattern.matcher(string);
        }
        if (!string.trim().equalsIgnoreCase("")) {
            list.add(string);
        }
        
        return list;
    }
}

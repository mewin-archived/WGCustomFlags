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
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.FlagContext;
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
    public Set<T> parseInput(FlagContext flagContext) throws InvalidFlagFormat {
        String input = flagContext.getUserInput();
        WorldGuardPlugin plugin = WGCustomFlagsPlugin.wgPlugin;
        CommandSender sender = flagContext.getSender();
        Set<T> items = new HashSet<T>();

        for (String str : input.split(",")) {
            items.add(subFlag.parseInput(flagContext));
        }

        return new HashSet<T>(items);
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
        List<Object> list = new ArrayList<Object>();
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

    private String listToString(List<Object> list) {
        String str = "";
        Iterator<Object> itr = list.iterator();

        while(itr.hasNext()) {
            str += String.valueOf(itr.next()).replace("\\", "\\\\").replace(";", "\\;") + ";";
        }

        return str;
    }

    private List<Object> stringToList(String string) {
        ArrayList<Object> list = new ArrayList<Object>();
        Pattern pattern = Pattern.compile("[^\\\\](\\\\\\\\)*;");

        Matcher matcher = pattern.matcher(string);
        while(matcher.find()) {
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

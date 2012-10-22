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

import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.RegionGroup;

/**
 * Sub class for custom flags for WorldGuard custom flags
 * allways use this class as sub class if you want your flags to be saved
 * @author mewin <mewin001@hotmail.de>
 */
public abstract class CustomFlag<T> extends Flag<T> {
    public CustomFlag(String name, RegionGroup rg) {
        super(name, rg);
    }

    public CustomFlag(String name) {
        super(name);
    }

    /**
     * returns a value that can be saved as YAML
     * this could be a String, Integer, Double, Boolean, Map or List
     * by default you don't have to reimplement it, because the marshal method does the same
     * @param value the object saved with the flag
     * @return a value that can be saved as YAML
     */
    public Object getYAML(T value) {
        return this.marshal(value);
    }

    /**
     * returns the instance of the class belonging to the YAML value you saved
     * by default you don't have to reimplement this method, the unmarsharl method does the same
     * @param obj the YAML value that has been saved
     * @return an instance of T fitting to the saved YAML value
     */
    public T fromYAML(Object obj) {
        return this.unmarshal(obj);
    }

    /**
     * returns the instance of T that had been saved to the database before
     * @param str the String that has been saved to the database
     * @return an instance of T that fits to the String
     */    
    public abstract T loadFromDb(String str);

    /**
     * returns a String that can be saved and later loaded again to and from the database
     * @param o the instance of T that has to be saved
     * @return a String that represents the object in the database
     */
    public abstract String saveToDb(T o);
}

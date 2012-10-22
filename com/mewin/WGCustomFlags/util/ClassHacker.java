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
package com.mewin.WGCustomFlags.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 *
 * @author mewin <mewin001@hotmail.de>
 */
public final class ClassHacker {

    public static void setStaticValue(Field field, Object value) {
        try {
            Field modifier = Field.class.getDeclaredField("modifiers");

            modifier.setAccessible(true);
            modifier.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            field.set(null, value);
        } catch(NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) { }
    }

    public static void setPrivateValue(Object obj, String name, Object value) {
        try {
            Field field = obj.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (IllegalArgumentException ex) {
            //Logger.getLogger(ClassHacker.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
           // Logger.getLogger(ClassHacker.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchFieldException | SecurityException ex) {
            //Logger.getLogger(ClassHacker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static Object getPrivateValue(Object obj, String name) {
        try {
            Field field = obj.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(obj);
        } catch(NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
            return null;
        }
    }
}

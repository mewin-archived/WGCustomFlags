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
import java.lang.reflect.Method;
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
        } catch(Exception ex) { }
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
        } catch (Exception ex) {
            //Logger.getLogger(ClassHacker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static Object getPrivateValue(Object obj, String name) {
        Field field = null;
        Class clazz = obj.getClass();
        try
        {
            do
            {
                field = clazz.getDeclaredField(name);
                clazz = clazz.getSuperclass();
            }
            while (field == null && clazz != null);
            if (field == null)
            {
                return null;
            }
            field.setAccessible(true);
            return field.get(obj);
        }
        catch(Exception ex)
        {
            return null;
        }
    }
    
    public static Object callPrivateMethod(Object obj, String name, Class[] paramTypes, Object[] params)
    {
        Method method = null;
        Class clazz = obj.getClass();
        try
        {
            do
            {
                method = clazz.getDeclaredMethod(name, paramTypes);
                clazz = clazz.getSuperclass();
            }
            while (method == null && clazz != null);
            if (method == null)
            {
                return null;
            }
            method.setAccessible(true);
            return method.invoke(obj, params);
        }
        catch(Exception ex)
        {
            return null;
        }
    }
}

package com.mewin.WGCustomFlags.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 *
 * @author mewin
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

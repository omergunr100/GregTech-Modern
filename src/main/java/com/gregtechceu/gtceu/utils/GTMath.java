package com.gregtechceu.gtceu.utils;

import net.minecraft.MethodsReturnNonnullByDefault;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.jetbrains.annotations.Contract;
import org.joml.Vector3f;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class GTMath {

    public static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static int[] split(long value) {
        IntArrayList result = new IntArrayList();
        while (value > 0) {
            int intValue = (int) Math.min(value, Integer.MAX_VALUE);
            result.add(intValue);
            value -= intValue;
        }
        return result.toIntArray();
    }

    public static int saturatedCast(long value) {
        if (value > 2147483647L) {
            return Integer.MAX_VALUE;
        } else {
            return value < -2147483648L ? Integer.MIN_VALUE : (int) value;
        }
    }

    /**
     * @param from the original vector
     * @param to   the wanted vector
     * @return the angle of rotation to make {@code from} point in the direction of {@code to}
     */
    @Contract(pure = true)
    public static float getRotationAngle(final Vector3f from, final Vector3f to) {
        return (float) Math.acos(from.dot(to));
    }

    /**
     * This method isn't pure, {@code from} will be modified!
     * 
     * @param from the original vector
     * @param to   the wanted vector
     * @return the axis of rotation to make {@code from} point in the direction of {@code to}
     */
    public static Vector3f getRotationAxis(Vector3f from, final Vector3f to) {
        return getRotationAxis(from, to, from);
    }

    public static Vector3f getRotationAxis(Vector3f from, Vector3f to, Vector3f dest) {
        return from.cross(to, dest).normalize();
    }
}

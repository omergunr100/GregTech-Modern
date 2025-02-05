package com.gregtechceu.gtceu.utils;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

import org.jetbrains.annotations.Contract;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.security.InvalidParameterException;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GTMatrixUtils {

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

    /**
     * @param from the original vector
     * @param to   the wanted vector
     * @param dest the vector to save the result to
     * @return {@code dest}
     */
    public static Vector3f getRotationAxis(Vector3f from, Vector3f to, Vector3f dest) {
        return from.cross(to, dest).normalize();
    }

    /**
     * Transforms the {@code matrix} and all {@code additional} vectors such that the {@code from} vector will be on the
     * {@code to} vector's axis
     * 
     * @param matrix     the matrix to transform
     * @param from       the original vector
     * @param to         the destination vector
     * @param additional additional vectors to transform
     */
    public static void rotateMatrix(Matrix4f matrix, Vector3f from, Vector3f to, Vector3f... additional) {
        if (from.equals(to)) {
            return;
        }
        if (-from.x == to.x && -from.y == to.y && -from.z == to.z) {
            rotateMatrix(matrix, Mth.PI, 0, 1, 0, additional);
        } else {
            var angle = getRotationAngle(from, to);
            getRotationAxis(from, to);
            rotateMatrix(matrix, angle, from.x, from.y, from.z, additional);
        }
    }

    /**
     * @param matrix     the matrix to transform
     * @param angle      the angle of rotation (radians)
     * @param x          axis of rotation x value
     * @param y          axis of rotation y value
     * @param z          axis of rotation z value
     * @param additional additional vectors to transform
     */
    public static void rotateMatrix(Matrix4f matrix, float angle, float x, float y, float z, Vector3f... additional) {
        matrix.rotate(angle, x, y, z);
        for (var vec : additional) {
            vec.rotateAxis(angle, x, y, z);
        }
    }

    /**
     * @param upward the {@code upwardFacing} of the machine
     * @return the angle of rotation (in radians) along the front face axis to get the correct orientation
     */
    public static float upwardFacingAngle(Direction upward) {
        return switch (upward) {
            case NORTH -> 0;
            case SOUTH -> 2;
            case WEST -> 1;
            case EAST -> 3;
            default -> throw new InvalidParameterException("Upward facing can't be up/down");
        } * Mth.PI / 2;
    }
}

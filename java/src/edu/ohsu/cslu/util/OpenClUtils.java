package edu.ohsu.cslu.util;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLFloatBuffer;
import com.nativelibs4java.opencl.CLIntBuffer;
import com.nativelibs4java.opencl.CLMem;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.CLShortBuffer;

public class OpenClUtils {

    public static CLFloatBuffer copyToDevice(final CLContext context, final CLQueue clQueue,
            final float[] array, final CLMem.Usage usage) {
        final CLFloatBuffer clFloatBuffer = context.createFloatBuffer(usage, array.length);
        copyToDevice(clQueue, clFloatBuffer, array);
        return clFloatBuffer;
    }

    public static void copyToDevice(final CLQueue clQueue, final CLFloatBuffer clFloatBuffer,
            final float[] array) {
        final FloatBuffer mappedInitialArray = clFloatBuffer.map(clQueue, CLMem.MapFlags.Write);
        mappedInitialArray.put(array);
        clFloatBuffer.unmap(clQueue, mappedInitialArray);
    }

    public static CLIntBuffer copyToDevice(final CLContext context, final CLQueue clQueue, final int[] array,
            final CLMem.Usage usage) {
        final CLIntBuffer clIntBuffer = context.createIntBuffer(usage, array.length);
        copyToDevice(clQueue, clIntBuffer, array);
        return clIntBuffer;
    }

    public static void copyToDevice(final CLQueue clQueue, final CLIntBuffer clIntBuffer, final int[] array) {
        final IntBuffer mappedInitialArray = clIntBuffer.map(clQueue, CLMem.MapFlags.Write);
        mappedInitialArray.put(array);
        clIntBuffer.unmap(clQueue, mappedInitialArray);
    }

    public static CLShortBuffer copyToDevice(final CLContext context, final CLQueue clQueue,
            final short[] array, final CLMem.Usage usage) {
        final CLShortBuffer clShortBuffer = context.createShortBuffer(usage, array.length);
        copyToDevice(clQueue, clShortBuffer, array);
        return clShortBuffer;
    }

    public static void copyToDevice(final CLQueue clQueue, final CLShortBuffer clShortBuffer,
            final short[] array) {
        final ShortBuffer mappedInitialArray = clShortBuffer.map(clQueue, CLMem.MapFlags.Write);
        mappedInitialArray.put(array);
        clShortBuffer.unmap(clQueue, mappedInitialArray);
    }

    public static float[] copyFromDevice(final CLQueue clQueue, final CLFloatBuffer clFloatBuffer,
            final int size) {
        final float[] array = new float[size];
        copyFromDevice(clQueue, clFloatBuffer, array);
        return array;
    }

    public static void copyFromDevice(final CLQueue clQueue, final CLFloatBuffer clFloatBuffer,
            final float[] array) {
        final FloatBuffer buf = clFloatBuffer.read(clQueue, 0, array.length);
        buf.get(array);
    }

    public static int[] copyFromDevice(final CLQueue clQueue, final CLIntBuffer clIntBuffer, final int size) {
        final int[] array = new int[size];
        copyFromDevice(clQueue, clIntBuffer, array);
        return array;
    }

    public static void copyFromDevice(final CLQueue clQueue, final CLIntBuffer clIntBuffer, final int[] array) {
        final IntBuffer buf = clIntBuffer.read(clQueue, 0, array.length);
        buf.get(array);
    }

    public static short[] copyFromDevice(final CLQueue clQueue, final CLShortBuffer clShortBuffer,
            final int size) {
        final short[] array = new short[size];
        copyFromDevice(clQueue, clShortBuffer, array);
        return array;
    }

    public static void copyFromDevice(final CLQueue clQueue, final CLShortBuffer clShortBuffer,
            final short[] array) {
        final ShortBuffer buf = clShortBuffer.read(clQueue, 0, array.length);
        buf.get(array);
    }

}

/*
 * Copyright 2010-2014, Oregon Health & Science University
 * 
 * This file is part of the BUBS Parser.
 * 
 * The BUBS Parser is free software: you can redistribute it and/or 
 * modify  it under the terms of the GNU Affero General Public License 
 * as published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * The BUBS Parser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Further documentation and contact information is available at
 *   https://code.google.com/p/bubs-parser/ 
 */
package edu.ohsu.cslu.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import com.nativelibs4java.opencl.CLBuildException;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLFloatBuffer;
import com.nativelibs4java.opencl.CLIntBuffer;
import com.nativelibs4java.opencl.CLMem;
import com.nativelibs4java.opencl.CLProgram;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.CLShortBuffer;

public class OpenClUtils {

    public static CLProgram compileClKernels(final CLContext context, final Class<?> c, final String prefix)
            throws CLBuildException {
        try {
            // Compile OpenCL kernels
            final StringWriter sw = new StringWriter();
            sw.write(prefix + '\n');
            final String filename = c.getCanonicalName().replace('.', File.separatorChar) + ".cl";
            final BufferedReader br = new BufferedReader(new InputStreamReader(c.getClassLoader().getResourceAsStream(
                    filename)));
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                sw.write(line);
                sw.write('\n');
            }
            return context.createProgram(sw.toString()).build();
        } catch (final IOException e) {
            // StringWriter should never IOException
            throw new RuntimeException(e);
        }
    }

    // Float
    public static CLFloatBuffer copyToDevice(final CLQueue clQueue, final float[] array, final CLMem.Usage usage) {
        final CLFloatBuffer clFloatBuffer = clQueue.getContext().createFloatBuffer(usage, array.length);
        copyToDevice(clQueue, clFloatBuffer, array, 0, array.length);
        return clFloatBuffer;
    }

    public static CLFloatBuffer copyToDevice(final CLQueue clQueue, final float[] array, final int offset,
            final int length, final CLMem.Usage usage) {
        final CLFloatBuffer clFloatBuffer = clQueue.getContext().createFloatBuffer(usage, length - offset);
        copyToDevice(clQueue, clFloatBuffer, array, offset, length);
        return clFloatBuffer;
    }

    public static void copyToDevice(final CLQueue clQueue, final CLFloatBuffer clFloatBuffer, final float[] array) {
        copyToDevice(clQueue, clFloatBuffer, array, 0, array.length);
    }

    public static void copyToDevice(final CLQueue clQueue, final CLFloatBuffer clFloatBuffer, final float[] array,
            final int offset, final int length) {
        final FloatBuffer mappedInitialArray = clFloatBuffer.map(clQueue, CLMem.MapFlags.Write);
        mappedInitialArray.put(array, offset, length);
        clFloatBuffer.unmap(clQueue, mappedInitialArray);
    }

    // Int
    public static CLIntBuffer copyToDevice(final CLQueue clQueue, final int[] array, final CLMem.Usage usage) {
        final CLIntBuffer clIntBuffer = clQueue.getContext().createIntBuffer(usage, array.length);
        copyToDevice(clQueue, clIntBuffer, array, 0, array.length);
        return clIntBuffer;
    }

    public static CLIntBuffer copyToDevice(final CLQueue clQueue, final int[] array, final int offset,
            final int length, final CLMem.Usage usage) {
        final CLIntBuffer clIntBuffer = clQueue.getContext().createIntBuffer(usage, length - offset);
        copyToDevice(clQueue, clIntBuffer, array, offset, length);
        return clIntBuffer;
    }

    public static void copyToDevice(final CLQueue clQueue, final CLIntBuffer clFloatBuffer, final int[] array) {
        copyToDevice(clQueue, clFloatBuffer, array, 0, array.length);
    }

    public static void copyToDevice(final CLQueue clQueue, final CLIntBuffer clIntBuffer, final int[] array,
            final int offset, final int length) {
        final IntBuffer mappedInitialArray = clIntBuffer.map(clQueue, CLMem.MapFlags.Write);
        mappedInitialArray.put(array, offset, length);
        clIntBuffer.unmap(clQueue, mappedInitialArray);
    }

    // Short
    public static CLShortBuffer copyToDevice(final CLQueue clQueue, final short[] array, final CLMem.Usage usage) {
        final CLShortBuffer clShortBuffer = clQueue.getContext().createShortBuffer(usage, array.length);
        copyToDevice(clQueue, clShortBuffer, array, 0, array.length);
        return clShortBuffer;
    }

    public static CLShortBuffer copyToDevice(final CLQueue clQueue, final short[] array, final int offset,
            final int length, final CLMem.Usage usage) {
        final CLShortBuffer clShortBuffer = clQueue.getContext().createShortBuffer(usage, length - offset);
        copyToDevice(clQueue, clShortBuffer, array, offset, length);
        return clShortBuffer;
    }

    public static void copyToDevice(final CLQueue clQueue, final CLShortBuffer clFloatBuffer, final short[] array) {
        copyToDevice(clQueue, clFloatBuffer, array, 0, array.length);
    }

    public static void copyToDevice(final CLQueue clQueue, final CLShortBuffer clShortBuffer, final short[] array,
            final int offset, final int length) {
        final ShortBuffer mappedInitialArray = clShortBuffer.map(clQueue, CLMem.MapFlags.Write);
        mappedInitialArray.put(array, offset, length);
        clShortBuffer.unmap(clQueue, mappedInitialArray);
    }

    public static float[] copyFromDevice(final CLQueue clQueue, final CLFloatBuffer clFloatBuffer, final int size) {
        final float[] array = new float[size];
        copyFromDevice(clQueue, clFloatBuffer, array);
        return array;
    }

    public static void copyFromDevice(final CLQueue clQueue, final CLFloatBuffer clFloatBuffer, final float[] array) {
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

    public static short[] copyFromDevice(final CLQueue clQueue, final CLShortBuffer clShortBuffer, final int size) {
        final short[] array = new short[size];
        copyFromDevice(clQueue, clShortBuffer, array);
        return array;
    }

    public static void copyFromDevice(final CLQueue clQueue, final CLShortBuffer clShortBuffer, final short[] array) {
        final ShortBuffer buf = clShortBuffer.read(clQueue, 0, array.length);
        buf.get(array);
    }

}

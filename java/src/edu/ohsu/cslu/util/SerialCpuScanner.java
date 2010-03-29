package edu.ohsu.cslu.util;

import java.util.Arrays;

/**
 * A simple pure-java implementation of the {@link Scanner} interface. All computations are executed serially
 * on te CPU.
 * 
 * @author Aaron Dunlop
 * @since Mar 28, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class SerialCpuScanner extends Scanner.BaseScanner implements Scanner {

    public int[] exclusiveScan(final int[] input, final int fromIndex, final int toIndex,
            final Operator operator) {
        final int[] result = new int[input.length];
        exclusiveScan(input, result, fromIndex, toIndex, operator);

        if (fromIndex != 0 || toIndex != input.length) {
            final int[] result2 = new int[toIndex - fromIndex];
            System.arraycopy(result, fromIndex, result2, 0, result2.length);
            return result2;
        }

        return result;
    }

    public int[] exclusiveSegmentedScan(final int[] input, final byte[] segmentFlags, final Operator operator) {
        final int[] result = new int[input.length];
        int segmentStart = 0;
        for (int i = segmentStart; i < input.length; i++) {
            if (segmentFlags[i] != 0) {
                exclusiveScan(input, result, segmentStart, i + 1, operator);
                segmentStart = i + 1;
            }
        }
        exclusiveScan(input, result, segmentStart, input.length, operator);
        return result;
    }

    public void exclusiveScan(final int[] input, final int[] result, final int fromIndex, final int toIndex,
            final Operator operator) {
        switch (operator) {
        case SUM:
            result[fromIndex] = 0;
            for (int i = fromIndex + 1; i < toIndex; i++) {
                result[i] = input[i - 1] + result[i - 1];
            }
            return;

        case LOGICAL_AND:
            result[fromIndex] = 0;
            for (int i = fromIndex + 1; i < toIndex; i++) {
                result[i] = (input[i - 1] != 0 && result[i - 1] != 0) ? 1 : 0;
            }
            return;

        case LOGICAL_NAND:
            result[fromIndex] = 0;
            for (int i = fromIndex + 1; i < toIndex; i++) {
                result[i] = (input[i - 1] != 0 && result[i - 1] != 0) ? 0 : 1;
            }
            return;

        case MAX:
            result[fromIndex] = 0;
            for (int i = fromIndex + 1; i < toIndex; i++) {
                result[i] = input[i - 1] > result[i - 1] ? input[i - 1] : result[i - 1];
            }
            return;

        case MIN:
            result[fromIndex] = 0;
            for (int i = fromIndex + 1; i < toIndex; i++) {
                result[i] = input[i - 1] < result[i - 1] ? input[i - 1] : result[i - 1];
            }
            return;

        case EQUAL:
            result[fromIndex] = 0;
            result[fromIndex + 1] = 0;
            for (int i = 2; i < toIndex; i++) {
                result[i] = input[i - 1] == input[i - 2] ? 1 : 0;
            }
            return;

        case NOT_EQUAL:
            result[fromIndex] = 0;
            result[fromIndex + 1] = 1;
            for (int i = 2; i < toIndex; i++) {
                result[i] = input[i - 1] == input[i - 2] ? 0 : 1;
            }
            return;

        default:
            throw new RuntimeException("Unsupported operator: " + operator);
        }
    }

    public float[] exclusiveScan(final float[] input, final int fromIndex, final int toIndex,
            final Operator operator) {
        final float[] result = new float[input.length];
        exclusiveScan(input, result, fromIndex, toIndex, operator);

        if (fromIndex != 0 || toIndex != input.length) {
            final float[] result2 = new float[toIndex - fromIndex];
            System.arraycopy(result, fromIndex, result2, 0, result2.length);
            return result2;
        }

        return result;
    }

    public float[] exclusiveSegmentedScan(final float[] input, final byte[] segmentFlags,
            final Operator operator) {
        final float[] result = new float[input.length];
        int segmentStart = 0;
        for (int i = segmentStart; i < input.length; i++) {
            if (segmentFlags[i] != 0) {
                exclusiveScan(input, result, segmentStart, i + 1, operator);
                segmentStart = i + 1;
            }
        }
        exclusiveScan(input, result, segmentStart, input.length, operator);
        return result;
    }

    public void exclusiveScan(final float[] input, final float[] result, final int fromIndex,
            final int toIndex, final Operator operator) {
        switch (operator) {
        case SUM:
            result[fromIndex] = 0;
            for (int i = fromIndex + 1; i < toIndex; i++) {
                result[i] = input[i - 1] + result[i - 1];
            }
            return;

        case LOGICAL_AND:
            result[fromIndex] = 0;
            for (int i = fromIndex + 1; i < toIndex; i++) {
                result[i] = (input[i - 1] != 0 && result[i - 1] != 0) ? 1 : 0;
            }
            return;

        case LOGICAL_NAND:
            result[fromIndex] = 0;
            for (int i = fromIndex + 1; i < toIndex; i++) {
                result[i] = (input[i - 1] != 0 && result[i - 1] != 0) ? 0 : 1;
            }
            return;

        case MAX:
            result[fromIndex] = 0;
            for (int i = fromIndex + 1; i < toIndex; i++) {
                result[i] = input[i - 1] > result[i - 1] ? input[i - 1] : result[i - 1];
            }
            return;

        case MIN:
            result[fromIndex] = 0;
            for (int i = fromIndex + 1; i < toIndex; i++) {
                result[i] = input[i - 1] < result[i - 1] ? input[i - 1] : result[i - 1];
            }
            return;

        case EQUAL:
            result[fromIndex] = 0;
            result[fromIndex + 1] = 0;
            for (int i = 2; i < toIndex; i++) {
                result[i] = input[i - 1] == input[i - 2] ? 1 : 0;
            }
            return;

        case NOT_EQUAL:
            result[fromIndex] = 0;
            result[fromIndex + 1] = 1;
            for (int i = 2; i < toIndex; i++) {
                result[i] = input[i - 1] == input[i - 2] ? 0 : 1;
            }
            return;

        default:
            throw new RuntimeException("Unsupported operator: " + operator);
        }
    }

    public int[] inclusiveScan(final int[] input, final int fromIndex, final int toIndex,
            final Operator operator) {
        final int[] result = new int[input.length];
        inclusiveScan(input, result, fromIndex, toIndex, operator);

        if (fromIndex != 0 || toIndex != input.length) {
            final int[] result2 = new int[toIndex - fromIndex];
            System.arraycopy(result, fromIndex, result2, 0, result2.length);
            return result2;
        }

        return result;
    }

    public int[] inclusiveSegmentedScan(final int[] input, final byte[] segmentFlags, final Operator operator) {
        final int[] result = new int[input.length];
        int segmentStart = 0;
        for (int i = segmentStart; i < input.length; i++) {
            if (segmentFlags[i] != 0) {
                inclusiveScan(input, result, segmentStart, i + 1, operator);
                segmentStart = i + 1;
            }
        }
        inclusiveScan(input, result, segmentStart, input.length, operator);
        return result;
    }

    public void inclusiveScan(final int[] input, final int[] result, final int fromIndex, final int toIndex,
            final Operator operator) {
        switch (operator) {
        case SUM:
            result[fromIndex] = input[fromIndex];
            for (int i = fromIndex + 1; i < toIndex; i++) {
                result[i] = input[i] + result[i - 1];
            }
            return;

        case LOGICAL_AND:
            result[fromIndex] = 1;
            for (int i = fromIndex + 1; i < toIndex; i++) {
                result[i] = (input[i] != 0 && result[i - 1] != 0) ? 1 : 0;
            }
            return;

        case LOGICAL_NAND:
            result[fromIndex] = 0;
            for (int i = fromIndex + 1; i < toIndex; i++) {
                result[i] = (input[i] != 0 && result[i - 1] != 0) ? 0 : 1;
            }
            return;

        case MAX:
            result[fromIndex] = input[fromIndex];
            for (int i = fromIndex + 1; i < toIndex; i++) {
                result[i] = input[i] > result[i - 1] ? input[i] : result[i - 1];
            }
            return;

        case MIN:
            result[fromIndex] = input[fromIndex];
            for (int i = fromIndex + 1; i < toIndex; i++) {
                result[i] = input[i] < result[i - 1] ? input[i] : result[i - 1];
            }
            return;

        case EQUAL:
            result[fromIndex] = 0;
            for (int i = fromIndex + 1; i < toIndex; i++) {
                result[i] = input[i] == input[i - 1] ? 1 : 0;
            }
            return;

        case NOT_EQUAL:
            result[fromIndex] = 1;
            for (int i = fromIndex + 1; i < toIndex; i++) {
                result[i] = input[i] == input[i - 1] ? 0 : 1;
            }
            return;

        default:
            throw new RuntimeException("Unsupported operator: " + operator);
        }
    }

    public float[] inclusiveScan(final float[] input, final int fromIndex, final int toIndex,
            final Operator operator) {
        final float[] result = new float[input.length];
        inclusiveScan(input, result, fromIndex, toIndex, operator);

        if (fromIndex != 0 || toIndex != input.length) {
            final float[] result2 = new float[toIndex - fromIndex];
            System.arraycopy(result, fromIndex, result2, 0, result2.length);
            return result2;
        }

        return result;
    }

    public float[] inclusiveSegmentedScan(final float[] input, final byte[] segmentFlags,
            final Operator operator) {
        final float[] result = new float[input.length];
        int segmentStart = 0;
        for (int i = segmentStart; i < input.length; i++) {
            if (segmentFlags[i] != 0) {
                inclusiveScan(input, result, segmentStart, i + 1, operator);
                segmentStart = i + 1;
            }
        }
        inclusiveScan(input, result, segmentStart, input.length, operator);
        return result;
    }

    public void inclusiveScan(final float[] input, final float[] result, final int fromIndex,
            final int toIndex, final Operator operator) {
        switch (operator) {
        case SUM:
            result[fromIndex] = input[fromIndex];
            for (int i = fromIndex + 1; i < toIndex; i++) {
                result[i] = input[i] + result[i - 1];
            }
            return;

        case LOGICAL_AND:
            result[fromIndex] = 1;
            for (int i = fromIndex + 1; i < toIndex; i++) {
                result[i] = (input[i] != 0 && result[i - 1] != 0) ? 1 : 0;
            }
            return;

        case LOGICAL_NAND:
            result[fromIndex] = 0;
            for (int i = fromIndex + 1; i < toIndex; i++) {
                result[i] = (input[i] != 0 && result[i - 1] != 0) ? 0 : 1;
            }
            return;

        case MAX:
            result[fromIndex] = input[fromIndex];
            for (int i = fromIndex + 1; i < toIndex; i++) {
                result[i] = input[i] > result[i - 1] ? input[i] : result[i - 1];
            }
            return;

        case MIN:
            result[fromIndex] = input[fromIndex];
            for (int i = fromIndex + 1; i < toIndex; i++) {
                result[i] = input[i] < result[i - 1] ? input[i] : result[i - 1];
            }
            return;

        case EQUAL:
            result[fromIndex] = 0;
            for (int i = fromIndex + 1; i < toIndex; i++) {
                result[i] = input[i] == input[i - 1] ? 1 : 0;
            }
            return;

        case NOT_EQUAL:
            result[fromIndex] = 1;
            for (int i = fromIndex + 1; i < toIndex; i++) {
                result[i] = input[i] == input[i - 1] ? 0 : 1;
            }
            return;

        default:
            throw new RuntimeException("Unsupported operator: " + operator);
        }
    }

    public int[] pack(final int[] input, final byte[] flags, final int fromIndex, final int toIndex) {
        int count = 0;
        for (int i = fromIndex; i < toIndex; i++) {
            if (flags[i] != 0) {
                count++;
            }
        }

        final int[] result = new int[count];
        pack(input, result, flags, fromIndex, toIndex);
        return result;
    }

    public void pack(final int[] input, final int[] result, final byte[] flags, final int fromIndex,
            final int toIndex) {

        int resultIndex = 0;
        for (int i = fromIndex; i < toIndex; i++) {
            if (flags[i] != 0) {
                result[resultIndex++] = input[i];
            }
        }
    }

    public float[] pack(final float[] input, final byte[] flags, final int fromIndex, final int toIndex) {
        int count = 0;
        for (int i = fromIndex; i < toIndex; i++) {
            if (flags[i] != 0) {
                count++;
            }
        }

        final float[] result = new float[count];
        pack(input, result, flags, fromIndex, toIndex);
        return result;
    }

    public void pack(final float[] input, final float[] result, final byte[] flags, final int fromIndex,
            final int toIndex) {

        int resultIndex = 0;
        for (int i = fromIndex; i < toIndex; i++) {
            if (flags[i] != 0) {
                result[resultIndex++] = input[i];
            }
        }
    }

    @Override
    public int[] scatter(final int[] input, final int[] indices) {
        final int arraySize = scatterArraySize(indices);
        final int[] result = new int[arraySize];
        final byte[] flags = new byte[arraySize];
        Arrays.fill(flags, (byte) 1);
        scatter(input, result, indices, flags);
        return result;
    }

    @Override
    public int[] scatter(final int[] input, final int[] indices, final byte[] flags) {
        final int arraySize = scatterArraySize(indices, flags);
        final int[] result = new int[arraySize];
        scatter(input, result, indices, flags);
        return result;
    }

    @Override
    public void scatter(final int[] input, final int[] result, final int[] indices, final byte[] flags) {
        for (int i = 0; i < input.length; i++) {
            if (flags[i] != 0) {
                result[indices[i]] = input[i];
            }
        }
    }

    @Override
    public float[] scatter(final float[] input, final int[] indices) {
        final int arraySize = scatterArraySize(indices);
        final float[] result = new float[arraySize];
        final byte[] flags = new byte[arraySize];
        Arrays.fill(flags, (byte) 1);
        scatter(input, result, indices, flags);
        return result;
    }

    @Override
    public float[] scatter(final float[] input, final int[] indices, final byte[] flags) {
        final int arraySize = scatterArraySize(indices, flags);
        final float[] result = new float[arraySize];
        scatter(input, result, indices, flags);
        return result;
    }

    @Override
    public void scatter(final float[] input, final float[] result, final int[] indices, final byte[] flags) {
        for (int i = 0; i < input.length; i++) {
            if (flags[i] != 0) {
                result[indices[i]] = input[i];
            }
        }
    }

    @Override
    public short[] scatter(final short[] input, final int[] indices) {
        final int arraySize = scatterArraySize(indices);
        final short[] result = new short[arraySize];
        final byte[] flags = new byte[arraySize];
        Arrays.fill(flags, (byte) 1);
        scatter(input, result, indices, flags);
        return result;
    }

    @Override
    public short[] scatter(final short[] input, final int[] indices, final byte[] flags) {
        final int arraySize = scatterArraySize(indices, flags);
        final short[] result = new short[arraySize];
        scatter(input, result, indices, flags);
        return result;
    }

    @Override
    public void scatter(final short[] input, final short[] result, final int[] indices, final byte[] flags) {
        for (int i = 0; i < input.length; i++) {
            if (flags[i] != 0) {
                result[indices[i]] = input[i];
            }
        }
    }

    private int scatterArraySize(final int[] indices) {
        final int max = Math.max(indices);
        if (max == 0) {
            return 0;
        }
        return max + 1;
    }

    private int scatterArraySize(final int[] indices, final byte[] flags) {
        int max = 0;
        for (int i = 0; i < indices.length; i++) {
            if (flags[i] != 0 && indices[i] > max) {
                max = indices[i];
            }
        }
        return max + 1;
    }

    @Override
    public void parallelArrayInclusiveSegmentedMax(final float[] floatInput, final float[] floatResult,
            final short[] shortInput, final short[] shortResult, final byte[] segmentFlags) {
        float max = Float.NEGATIVE_INFINITY;
        short s = 0;
        for (int i = 0; i < floatInput.length; i++) {
            if (floatInput[i] > max) {
                max = floatInput[i];
                s = shortInput[i];
            }
            floatResult[i] = max;
            shortResult[i] = s;

            if (segmentFlags[i] != 0) {
                max = Float.NEGATIVE_INFINITY;
                s = 0;
            }
        }
    }

    @Override
    public void flagEndOfKeySegments(final int[] input, final byte[] result) {
        for (int i = 0; i < (input.length - 1); i++) {
            result[i] = (byte) ((input[i] == input[i + 1]) ? 0 : 1);
        }
        result[result.length - 1] = 1;
    }
}

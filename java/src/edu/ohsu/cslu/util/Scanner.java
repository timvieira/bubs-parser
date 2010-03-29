package edu.ohsu.cslu.util;

public interface Scanner {

    public static enum Operator {
        SUM, MAX, MIN, LOGICAL_AND, LOGICAL_NAND, EQUAL, NOT_EQUAL;
    }

    /**
     * Performs an exclusive prefix scan of the input array, using the specified operator.
     * 
     * @param input
     * @param operator
     * @return Result of the scan; an array of the same size as the input.
     */
    public int[] exclusiveScan(final int[] input, final Operator operator);

    /**
     * Performs an exclusive prefix scan of the input array, using the specified operator.
     * 
     * @param input
     * @param operator
     * @return Result of the scan; an array of the same size as the input.
     */
    public float[] exclusiveScan(final float[] input, final Operator operator);

    /**
     * Performs an exclusive prefix scan of a portion of the input array, using the specified operator.
     * 
     * @param input
     * @param fromIndex
     *            Start index, inclusive
     * @param toIndex
     *            End index, exclusive
     * @param operator
     * @return Result of the scan; an array of length (toIndex - fromIndex).
     */
    public int[] exclusiveScan(final int[] input, final int fromIndex, final int toIndex,
            final Operator operator);

    /**
     * Performs an exclusive prefix scan of a portion of the input array, using the specified operator.
     * 
     * @param input
     * @param fromIndex
     *            Start index, inclusive
     * @param toIndex
     *            End index, exclusive
     * @param operator
     * @return Result of the scan; an array of length (toIndex - fromIndex).
     */
    public float[] exclusiveScan(final float[] input, final int fromIndex, final int toIndex,
            final Operator operator);

    /**
     * Performs an exclusive segmented prefix scan of the input array, using the specified operator. The scan
     * is segmented at each non-zero value in the flag array.
     * 
     * @param input
     * @param flags
     *            Segment delimiters. Each non-zero value denotes the end of a segment.
     * @param operator
     * @return Result of the scan; an array of the same size as the input.
     */
    public int[] exclusiveSegmentedScan(final int[] input, final byte[] flags, final Operator operator);

    /**
     * Performs an exclusive segmented prefix scan of the input array, using the specified operator. The scan
     * is segmented at each non-zero value in the flag array.
     * 
     * @param input
     * @param flags
     *            Segment delimiters. Each non-zero value denotes the end of a segment.
     * @param operator
     * @return Result of the scan; an array of the same size as the input.
     */
    public float[] exclusiveSegmentedScan(final float[] input, final byte[] flags, final Operator operator);

    /**
     * Performs an exclusive prefix scan of the input array, using the specified operator, and storing the
     * results in a result array.
     * 
     * @param input
     * @param result
     *            Result array; must be the same length as the input array
     * @param fromIndex
     *            Start index, inclusive
     * @param toIndex
     *            End index, exclusive
     * @param operator
     */
    public void exclusiveScan(final int[] input, final int[] result, final int fromIndex, final int toIndex,
            final Operator operator);

    /**
     * Performs an exclusive prefix scan of the input array, using the specified operator, and storing the
     * results in a result array.
     * 
     * @param input
     * @param result
     *            Result array; must be the same length as the input array
     * @param fromIndex
     *            Start index, inclusive
     * @param toIndex
     *            End index, exclusive
     * @param operator
     */
    public void exclusiveScan(final float[] input, final float[] result, final int fromIndex,
            final int toIndex, final Operator operator);

    /**
     * Performs an inclusive prefix scan of the input array, using the specified operator.
     * 
     * @param input
     * @param operator
     * @return Result of the scan; an array of the same size as the input.
     */
    public int[] inclusiveScan(final int[] input, final Operator operator);

    /**
     * Performs an inclusive prefix scan of the input array, using the specified operator.
     * 
     * @param input
     * @param operator
     * @return Result of the scan; an array of the same size as the input.
     */
    public float[] inclusiveScan(final float[] input, final Operator operator);

    /**
     * Performs an inclusive prefix scan of a portion of the input array, using the specified operator.
     * 
     * @param input
     * @param fromIndex
     *            Start index, inclusive
     * @param toIndex
     *            End index, exclusive
     * @param operator
     * @return Result of the scan; an array of length (toIndex - fromIndex).
     */
    public int[] inclusiveScan(final int[] input, final int fromIndex, final int toIndex,
            final Operator operator);

    /**
     * Performs an inclusive prefix scan of a portion of the input array, using the specified operator.
     * 
     * @param input
     * @param fromIndex
     *            Start index, inclusive
     * @param toIndex
     *            End index, exclusive
     * @param operator
     * @return Result of the scan; an array of length (toIndex - fromIndex).
     */
    public float[] inclusiveScan(final float[] input, final int fromIndex, final int toIndex,
            final Operator operator);

    /**
     * Performs an inclusive segmented prefix scan of the input array, using the specified operator. The scan
     * is segmented at each non-zero value in the flag array.
     * 
     * @param input
     * @param segmentFlags
     *            Segment delimiters. Each non-zero value denotes the end of a segment.
     * @param operator
     * @return Result of the scan; an array of the same size as the input.
     */
    public int[] inclusiveSegmentedScan(final int[] input, final byte[] segmentFlags, final Operator operator);

    /**
     * Performs an inclusive segmented prefix scan of the input array, using the specified operator. The scan
     * is segmented at each non-zero value in the flag array.
     * 
     * @param input
     * @param segmentFlags
     *            Segment delimiters. Each non-zero value denotes the end of a segment.
     * @param operator
     * @return Result of the scan; an array of the same size as the input.
     */
    public float[] inclusiveSegmentedScan(final float[] input, final byte[] segmentFlags,
            final Operator operator);

    /**
     * Performs an inclusive prefix scan of the input array, using the specified operator, and storing the
     * results in a result array.
     * 
     * @param input
     * @param result
     *            Result array; must be the same length as the input array
     * @param fromIndex
     *            Start index, inclusive
     * @param toIndex
     *            End index, exclusive
     * @param operator
     */
    public void inclusiveScan(final int[] input, final int[] result, final int fromIndex, final int toIndex,
            final Operator operator);

    /**
     * Performs an inclusive prefix scan of the input array, using the specified operator, and storing the
     * results in a result array.
     * 
     * @param input
     * @param result
     *            Result array; must be the same length as the input array
     * @param fromIndex
     *            Start index, inclusive
     * @param toIndex
     *            End index, exclusive
     * @param operator
     */
    public void inclusiveScan(final float[] input, final float[] result, final int fromIndex,
            final int toIndex, final Operator operator);

    public int[] pack(final int[] input, final byte[] flags);

    public int[] pack(final int[] input, final byte[] flags, final int fromIndex, final int toIndex);

    public void pack(final int[] input, final int[] result, final byte[] flags, final int fromIndex,
            final int toIndex);

    public float[] pack(final float[] input, final byte[] flags);

    public float[] pack(final float[] input, final byte[] flags, final int fromIndex, final int toIndex);

    public void pack(final float[] input, final float[] result, final byte[] flags, final int fromIndex,
            final int toIndex);

    /**
     * Scatters the input array into a larger array indexed by the specified indices.
     * 
     * @param input
     * @param indices
     *            Indices for the scattered array. The index array must be the same length as the input array.
     * @return Scattered array
     */
    public int[] scatter(final int[] input, final int[] indices);

    /**
     * Scatters the input array into a larger array indexed by the specified indices.
     * 
     * @param input
     * @param indices
     *            Indices for the scattered array. The index array must be the same length as the input array.
     * @return Scattered array
     */
    public float[] scatter(final float[] input, final int[] indices);

    /**
     * Scatters the input array into a larger array indexed by the specified indices.
     * 
     * @param input
     * @param indices
     *            Indices for the scattered array. The index array must be the same length as the input array.
     * @return Scattered array
     */
    public short[] scatter(final short[] input, final int[] indices);

    /**
     * Scatters flagged values of the input array into a larger array indexed by the specified indices.
     * 
     * @param input
     * @param indices
     *            Indices for the scattered array. The index array must be the same length as the input array.
     * @param flags
     *            Flags of which input values should be scattered. Non-zero values are considered 'flagged'.
     *            The flags array must be the same length as the input array.
     * @return Scattered array
     */
    public int[] scatter(final int[] input, final int[] indices, final byte[] flags);

    /**
     * Scatters flagged values of the input array into a larger array indexed by the specified indices.
     * 
     * @param input
     * @param indices
     *            Indices for the scattered array. The index array must be the same length as the input array.
     * @param flags
     *            Flags of which input values should be scattered. Non-zero values are considered 'flagged'.
     *            The flags array must be the same length as the input array.
     * @return Scattered array
     */
    public float[] scatter(final float[] input, final int[] indices, final byte[] flags);

    /**
     * Scatters flagged values of the input array into a larger array indexed by the specified indices.
     * 
     * @param input
     * @param indices
     *            Indices for the scattered array. The index array must be the same length as the input array.
     * @param flags
     *            Flags of which input values should be scattered. Non-zero values are considered 'flagged'.
     *            The flags array must be the same length as the input array.
     * @return Scattered array
     */
    public short[] scatter(final short[] input, final int[] indices, final byte[] flags);

    /**
     * Scatters flagged values of the input array into a larger array indexed by the specified indices.
     * 
     * @param input
     * @param result
     *            Target array. Must be large enough to contain the maximum index from indices.
     * @param indices
     *            Indices for the scattered array. The index array must be the same length as the input array.
     * @param flags
     *            Flags of which input values should be scattered. Non-zero values are considered 'flagged'.
     *            The flags array must be the same length as the input array.
     */
    public void scatter(final int[] input, final int[] result, final int[] indices, final byte[] flags);

    /**
     * Scatters flagged values of the input array into a larger array indexed by the specified indices.
     * 
     * @param input
     * @param result
     *            Target array. Must be large enough to contain the maximum index from indices.
     * @param indices
     *            Indices for the scattered array. The index array must be the same length as the input array.
     * @param flags
     *            Flags of which input values should be scattered. Non-zero values are considered 'flagged'.
     *            The flags array must be the same length as the input array.
     */
    public void scatter(final float[] input, final float[] result, final int[] indices, final byte[] flags);

    /**
     * Scatters flagged values of the input array into a larger array indexed by the specified indices.
     * 
     * @param input
     * @param result
     *            Target array. Must be large enough to contain the maximum index from indices.
     * @param indices
     *            Indices for the scattered array. The index array must be the same length as the input array.
     * @param flags
     *            Flags of which input values should be scattered. Non-zero values are considered 'flagged'.
     *            The flags array must be the same length as the input array.
     */
    public void scatter(final short[] input, final short[] result, final int[] indices, final byte[] flags);

    /**
     * Performs a custom inclusive scan on a parallel array of float[] and short[]. Within each segment, the
     * it chooses the max of the floats and 'carries-along' the short value which occurs at the maximum float
     * encountered.
     * 
     * @param floatInput
     * @param floatResult
     *            Target float array. Must be the same size as the input arrays.
     * @param shortInput
     * @param shortResult
     *            Target short array. Must be the same size as the input arrays.
     * @param segmentFlags
     *            Segment delimiters. Each non-zero value denotes the end of a segment.
     */
    public void parallelArrayInclusiveSegmentedMax(final float[] floatInput, final float[] floatResult,
            final short[] shortInput, final short[] shortResult, final byte[] segmentFlags);

    /**
     * Flags the final instance of each key in a series. e.g. 1, 2, 2, 3, 3, 3, 4 will be flagged 1, 0, 1, 0,
     * 0, 1.
     * 
     * TODO Implement a version that returns a packed bit vector
     * 
     * @param input
     * @param result
     *            Flags (1 = flagged, 0 = un-flagged)
     */
    public void flagEndOfKeySegments(final int[] input, final byte[] result);

    public static abstract class BaseScanner implements Scanner {

        public int[] exclusiveScan(final int[] input, final Operator operator) {
            return exclusiveScan(input, 0, input.length, operator);
        }

        public float[] exclusiveScan(final float[] input, final Operator operator) {
            return exclusiveScan(input, 0, input.length, operator);
        }

        public int[] inclusiveScan(final int[] input, final Operator operator) {
            return inclusiveScan(input, 0, input.length, operator);
        }

        public float[] inclusiveScan(final float[] input, final Operator operator) {
            return inclusiveScan(input, 0, input.length, operator);
        }

        public int[] pack(final int[] input, final byte[] flags) {
            return pack(input, flags, 0, input.length);
        }

        public float[] pack(final float[] input, final byte[] flags) {
            return pack(input, flags, 0, input.length);
        }

    }
}
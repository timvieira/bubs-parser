package edu.ohsu.cslu.util;

import static com.nativelibs4java.opencl.JavaCL.createBestContext;

import java.util.Arrays;

import com.nativelibs4java.opencl.CLIntBuffer;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLProgram;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.CLMem.Usage;

public class OpenClScanner extends Scanner.BaseScanner implements Scanner {

    private final static int MAX_WORKGROUP_INCLUSIVE_SCAN_SIZE = 1024;
    private final static int MAX_LOCAL_GROUP_SIZE = 256;
    private final static int WORKGROUP_SIZE = 256;
    private final static int MAX_BATCH_ELEMENTS = 64 * 1048576;
    private final static int MIN_SHORT_ARRAY_SIZE = 4;
    private final static int MAX_SHORT_ARRAY_SIZE = 4 * WORKGROUP_SIZE;
    private final static int MIN_LARGE_ARRAY_SIZE = 8 * WORKGROUP_SIZE;
    private final static int MAX_LARGE_ARRAY_SIZE = 4 * WORKGROUP_SIZE * WORKGROUP_SIZE;

    private final CLKernel scanExclusiveLocal1Kernel;
    private final CLKernel scanExclusiveLocal2Kernel;
    private final CLKernel uniformUpdateKernel;

    private final CLQueue clQueue;

    // private CLIntBuffer buffer;

    public OpenClScanner() {
        this(createBestContext().createDefaultQueue());
    }

    public OpenClScanner(final CLQueue clQueue) {

        this.clQueue = clQueue;

        // this.workgroupSize = workgroupSize;
        // this.warpSize = warpSize;
        // this.localMemoryBanks = localMemoryBanks;

        try {
            final CLProgram program = OpenClUtils.compileClKernels(clQueue.getContext(), getClass(), "");

            scanExclusiveLocal1Kernel = program.createKernel("scanExclusiveLocal1");
            scanExclusiveLocal2Kernel = program.createKernel("scanExclusiveLocal2");
            uniformUpdateKernel = program.createKernel("uniformUpdate");

        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int[] exclusiveScan(final int[] input, final int fromIndex, final int toIndex,
            final Operator operator) {

        final CLIntBuffer src = OpenClUtils.copyToDevice(clQueue, input, fromIndex, toIndex - fromIndex,
            Usage.InputOutput);
        final CLIntBuffer dest = clQueue.getContext().createIntBuffer(Usage.Output, toIndex - fromIndex);
        //
        // CreatePartialSumBuffers(count);
        // PreScanBuffer(output_buffer, input_buffer, GROUP_SIZE, GROUP_SIZE, count);
        //
        // scanExclusiveLarge(dest, src, fromIndex, toIndex - fromIndex);
        return OpenClUtils.copyFromDevice(clQueue, dest, toIndex - fromIndex);
    }

    // int CreatePartialSumBuffers(final int count)
    // {
    // ElementsAllocated = count;
    //
    // final int group_size = GROUP_SIZE;
    // int element_count = count;
    //
    // int level = 0;
    //
    // do
    // {
    // final int group_count = (int)fmax(1, (int)ceil(element_count / (2.0f * group_size)));
    // if (group_count > 1)
    // {
    // level++;
    // }
    // element_count = group_count;
    //            
    // } while (element_count > 1);
    //
    // ScanPartialSums = (cl_mem*) malloc(level * sizeof(cl_mem));
    // LevelsAllocated = level;
    // memset(ScanPartialSums, 0, sizeof(cl_mem) * level);
    //        
    // element_count = count;
    // level = 0;
    //        
    // do
    // {
    // unsigned final int group_count = (int)fmax(1, (int)ceil(element_count / (2.0f * group_size)));
    // if (group_count > 1)
    // {
    // final size_t buffer_size = group_count * sizeof(float);
    // ScanPartialSums[level++] = clCreateBuffer(ComputeContext, CL_MEM_READ_WRITE, buffer_size, NULL, NULL);
    // }
    //
    // element_count = group_count;
    //
    // } while (element_count > 1);
    //
    // return CL_SUCCESS;
    // }
    //
    // int
    // PreScanBufferRecursive(
    // final cl_mem output_data,
    // final cl_mem input_data,
    // final int max_group_size,
    // final int max_work_item_count,
    // final int element_count,
    // final int level)
    // {
    // unsigned final int group_size = max_group_size;
    // unsigned final int group_count = (int)fmax(1.0f, (int)ceil(element_count / (2.0f * group_size)));
    // unsigned int work_item_count = 0;
    //
    // if (group_count > 1)
    // work_item_count = group_size;
    // else if (IsPowerOfTwo(element_count))
    // work_item_count = element_count / 2;
    // else
    // work_item_count = floorPow2(element_count);
    //            
    // work_item_count = (work_item_count > max_work_item_count) ? max_work_item_count : work_item_count;
    //
    // unsigned final int element_count_per_group = work_item_count * 2;
    // unsigned final int last_group_element_count = element_count - (group_count-1) *
    // element_count_per_group;
    // unsigned int remaining_work_item_count = (int)fmax(1.0f, last_group_element_count / 2);
    // remaining_work_item_count = (remaining_work_item_count > max_work_item_count) ? max_work_item_count :
    // remaining_work_item_count;
    // unsigned int remainder = 0;
    // final size_t last_shared = 0;
    //
    //        
    // if (last_group_element_count != element_count_per_group)
    // {
    // remainder = 1;
    //
    // if(!IsPowerOfTwo(last_group_element_count))
    // remaining_work_item_count = floorPow2(last_group_element_count);
    //            
    // remaining_work_item_count = (remaining_work_item_count > max_work_item_count) ? max_work_item_count :
    // remaining_work_item_count;
    // unsigned final int padding = (2 * remaining_work_item_count) / NUM_BANKS;
    // last_shared = sizeof(float) * (2 * remaining_work_item_count + padding);
    // }
    //
    // remaining_work_item_count = (remaining_work_item_count > max_work_item_count) ? max_work_item_count :
    // remaining_work_item_count;
    // size_t global[] = { (int)fmax(1, group_count - remainder) * work_item_count, 1 };
    // size_t local[] = { work_item_count, 1 };
    //
    // unsigned int padding = element_count_per_group / NUM_BANKS;
    // size_t shared = sizeof(float) * (element_count_per_group + padding);
    //        
    // cl_mem partial_sums = ScanPartialSums[level];
    // int err = CL_SUCCESS;
    //        
    // if (group_count > 1)
    // {
    // err = PreScanStoreSum(global, local, shared, output_data, input_data, partial_sums, work_item_count *
    // 2, 0, 0);
    // if(err != CL_SUCCESS)
    // return err;
    //                
    // if (remainder)
    // {
    // size_t last_global[] = { 1 * remaining_work_item_count, 1 };
    // size_t last_local[] = { remaining_work_item_count, 1 };
    //
    // err = PreScanStoreSumNonPowerOfTwo(
    // last_global, last_local, last_shared,
    // output_data, input_data, partial_sums,
    // last_group_element_count,
    // group_count - 1,
    // element_count - last_group_element_count);
    //            
    // if(err != CL_SUCCESS)
    // return err;
    //                
    // }
    //
    // err = PreScanBufferRecursive(partial_sums, partial_sums, max_group_size, max_work_item_count,
    // group_count, level + 1);
    // if(err != CL_SUCCESS)
    // return err;
    //                
    // err = UniformAdd(global, local, output_data, partial_sums, element_count - last_group_element_count, 0,
    // 0);
    // if(err != CL_SUCCESS)
    // return err;
    //            
    // if (remainder)
    // {
    // size_t last_global[] = { 1 * remaining_work_item_count, 1 };
    // size_t last_local[] = { remaining_work_item_count, 1 };
    //
    // err = UniformAdd(
    // last_global, last_local,
    // output_data, partial_sums,
    // last_group_element_count,
    // group_count - 1,
    // element_count - last_group_element_count);
    //                    
    // if(err != CL_SUCCESS)
    // return err;
    // }
    // }
    // else if (IsPowerOfTwo(element_count))
    // {
    // err = PreScan(global, local, shared, output_data, input_data, work_item_count * 2, 0, 0);
    // if(err != CL_SUCCESS)
    // return err;
    // }
    // else
    // {
    // err = PreScanNonPowerOfTwo(global, local, shared, output_data, input_data, element_count, 0, 0);
    // if(err != CL_SUCCESS)
    // return err;
    // }
    //
    // return CL_SUCCESS;
    // }
    //
    // void
    // PreScanBuffer(
    // final cl_mem output_data,
    // final cl_mem input_data,
    // unsigned int max_group_size,
    // unsigned int max_work_item_count,
    // unsigned int element_count)
    // {
    // PreScanBufferRecursive(output_data, input_data, max_group_size, max_work_item_count, element_count, 0);
    // }

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

    // main exclusive scan routine
    void scanExclusiveLarge(final CLIntBuffer dst, final CLIntBuffer src, final int batchSize,
            final int arrayLength) {

        // Check power-of-two factorization
        if (!Math.isPowerOf2(arrayLength)) {
            throw new RuntimeException("Array length must be a power-of-2");
        }

        // Check supported size range
        if (!((arrayLength >= MIN_LARGE_ARRAY_SIZE) && (arrayLength <= MAX_LARGE_ARRAY_SIZE))) {
            throw new RuntimeException();
        }

        // Check total batch size limit
        if (!((batchSize * arrayLength) <= MAX_BATCH_ELEMENTS)) {
            throw new RuntimeException("Batch size limit exceeded");
        }

        final CLIntBuffer buffer = clQueue.getContext().createIntBuffer(Usage.InputOutput,
            arrayLength / MAX_WORKGROUP_INCLUSIVE_SCAN_SIZE * 4);

        scanExclusiveLocal1(dst, src, (batchSize * arrayLength) / (4 * WORKGROUP_SIZE), 4 * WORKGROUP_SIZE);
        scanExclusiveLocal2(buffer, dst, src, batchSize, arrayLength / (4 * WORKGROUP_SIZE));
        uniformUpdate(dst, buffer, (batchSize * arrayLength) / (4 * WORKGROUP_SIZE));
    }

    void scanExclusiveLocal1(final CLIntBuffer dst, final CLIntBuffer src, final int n, final int size) {

        scanExclusiveLocal1Kernel.setArgs(dst, src, 2 * WORKGROUP_SIZE * 4, size);

        final int localWorkSize = WORKGROUP_SIZE;
        final int globalWorkSize = (n * size) / 4;

        scanExclusiveLocal1Kernel.enqueueNDRange(clQueue, new int[] { globalWorkSize },
            new int[] { localWorkSize });
    }

    void scanExclusiveLocal2(final CLIntBuffer buffer, final CLIntBuffer dst, final CLIntBuffer src,
            final int n, final int size) {

        final int elements = n * size;
        scanExclusiveLocal2Kernel.setArgs(buffer, dst, src, 2 * WORKGROUP_SIZE * 4, elements, size);

        final int localWorkSize = WORKGROUP_SIZE;
        final int globalWorkSize = iSnapUp(elements, WORKGROUP_SIZE);

        scanExclusiveLocal2Kernel.enqueueNDRange(clQueue, new int[] { globalWorkSize },
            new int[] { localWorkSize });
    }

    void uniformUpdate(final CLIntBuffer dst, final CLIntBuffer buffer, final int n) {

        uniformUpdateKernel.setArgs(dst, buffer);

        final int localWorkSize = WORKGROUP_SIZE;
        final int globalWorkSize = n * WORKGROUP_SIZE;

        uniformUpdateKernel
            .enqueueNDRange(clQueue, new int[] { globalWorkSize }, new int[] { localWorkSize });
    }

    private int iSnapUp(final int dividend, final int divisor) {
        return ((dividend % divisor) == 0) ? dividend : (dividend - dividend % divisor + divisor);
    }
}

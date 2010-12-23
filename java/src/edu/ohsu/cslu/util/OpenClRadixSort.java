package edu.ohsu.cslu.util;

import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLIntBuffer;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLProgram;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.CLMem.Usage;

import edu.ohsu.cslu.util.Sort.BaseSort;
import static com.nativelibs4java.opencl.JavaCL.createBestContext;

/**
 * Implements a radix sort in parallel on the GPU.
 * 
 * @author Aaron Dunlop
 * @since Mar 29, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class OpenClRadixSort extends BaseSort {

    private final int warpSize;
    private final int localMemoryBanks;

    private final CLContext clContext;
    private final CLQueue clQueue;
    private final int workgroupSize;

    private final CLKernel radixSortBlocksKeysOnlyKernel;
    private final CLKernel findRadixOffsetsKernel;
    private final CLKernel naiveScanKernel;
    private final CLKernel reorderDataKeysOnlyKernel;

    private final OpenClScanner openClScanner;

    public OpenClRadixSort(final int workgroupSize, final int warpSize, final int localMemoryBanks) {
        this(createBestContext().createDefaultQueue(), workgroupSize, warpSize, localMemoryBanks);
    }

    public OpenClRadixSort(final CLQueue clQueue, final int workgroupSize, final int warpSize, final int localMemoryBanks) {
        this.clQueue = clQueue;
        this.clContext = clQueue.getContext();

        this.workgroupSize = workgroupSize;
        this.warpSize = warpSize;
        this.localMemoryBanks = localMemoryBanks;

        this.openClScanner = new OpenClScanner(clQueue);

        try {
            final CLProgram program = OpenClUtils.compileClKernels(clContext, getClass(), "");

            radixSortBlocksKeysOnlyKernel = program.createKernel("radixSortBlocksKeysOnly");
            findRadixOffsetsKernel = program.createKernel("findRadixOffsets");
            naiveScanKernel = program.createKernel("scanNaive");
            reorderDataKeysOnlyKernel = program.createKernel("reorderDataKeysOnly");
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sort(final int[] array) {
        // TODO Auto-generated method stub

    }

    void radixSortKeysOnly(final CLIntBuffer keys, final int numElements, final int keyBits) {
        final CLIntBuffer clTmpKeys = clContext.createIntBuffer(Usage.InputOutput, 4 * numElements);
        final CLIntBuffer clCounters = clContext.createIntBuffer(Usage.InputOutput, 4 * warpSize);
        final CLIntBuffer clCountersSum = clContext.createIntBuffer(Usage.InputOutput, 4 * warpSize);
        final CLIntBuffer clBlockOffsets = clContext.createIntBuffer(Usage.InputOutput, 4 * warpSize);

        final int bitStep = 4;
        for (int i = 0; i * bitStep < keyBits; i++) {
            radixSortStepKeysOnly(keys, clTmpKeys, clBlockOffsets, clCountersSum, clCounters, bitStep, i * bitStep, numElements);
            i++;
        }
    }

    void radixSortStepKeysOnly(final CLIntBuffer clKeys, final CLIntBuffer clTmpKeys, final CLIntBuffer clBlockOffsets, final CLIntBuffer clCountersSum,
            final CLIntBuffer clCounters, final int nbits, final int startbit, final int numElements) {

        // Four step algorithms from Satish, Harris & Garland
        radixSortBlocksKeysOnly(clKeys, clTmpKeys, nbits, startbit, numElements);

        findRadixOffsets(clTmpKeys, clCounters, clBlockOffsets, startbit, numElements);

        openClScanner.scanExclusiveLarge(clCountersSum, clCounters, 1, numElements / 2 / workgroupSize * 16);

        reorderDataKeysOnly(clKeys, clTmpKeys, clBlockOffsets, clCountersSum, clCounters, startbit, numElements);
    }

    void radixSortBlocksKeysOnly(final CLIntBuffer clKeys, final CLIntBuffer clTmpKeys, final int nbits, final int startbit, final int numElements) {

        final int totalBlocks = numElements / 4 / workgroupSize;
        final int globalWorkSize = workgroupSize * totalBlocks;
        final int localWorkSize = workgroupSize;

        radixSortBlocksKeysOnlyKernel.setArgs(clKeys, clTmpKeys, nbits, startbit, numElements, totalBlocks, 4 * workgroupSize * 4);
        radixSortBlocksKeysOnlyKernel.enqueueNDRange(clQueue, new int[] { globalWorkSize }, new int[] { localWorkSize });
    }

    private void findRadixOffsets(final CLIntBuffer clTmpKeys, final CLIntBuffer clCounters, final CLIntBuffer clBlockOffsets, final int startbit, final int numElements) {

        final int totalBlocks = numElements / 2 / workgroupSize;
        final int globalWorkSize = workgroupSize * totalBlocks;
        final int localWorkSize = workgroupSize;

        findRadixOffsetsKernel.setArgs(clTmpKeys, clCounters, clBlockOffsets, startbit, numElements, totalBlocks, 2 * workgroupSize * 4);
        findRadixOffsetsKernel.enqueueNDRange(clQueue, new int[] { globalWorkSize }, new int[] { localWorkSize });
    }

    private void scanNaiveOCL(final CLIntBuffer clCountersSum, final CLIntBuffer clCounters, final int numElements) {

        final int nHist = numElements / 2 / workgroupSize * 16;
        final int globalWorkSize = nHist;
        final int localWorkSize = nHist;
        final int extra_space = nHist / localMemoryBanks;
        final int shared_mem_size = 4 * (nHist + extra_space);

        naiveScanKernel.setArgs(clCountersSum, clCounters, nHist, 2 * shared_mem_size);
        naiveScanKernel.enqueueNDRange(clQueue, new int[] { globalWorkSize }, new int[] { localWorkSize });
    }

    private void reorderDataKeysOnly(final CLIntBuffer clKeys, final CLIntBuffer clTmpKeys, final CLIntBuffer clBlockOffsets, final CLIntBuffer clCountersSum,
            final CLIntBuffer clCounters, final int startbit, final int numElements) {

        final int totalBlocks = numElements / 2 / workgroupSize;
        final int globalWorkSize = workgroupSize * totalBlocks;
        final int localWorkSize = workgroupSize;

        reorderDataKeysOnlyKernel.setArgs(clKeys, clTmpKeys, clBlockOffsets, clCountersSum, clCounters, startbit, numElements, totalBlocks, 2 * workgroupSize * 4);
        reorderDataKeysOnlyKernel.enqueueNDRange(clQueue, new int[] { globalWorkSize }, new int[] { localWorkSize });
    }

    @Override
    public void sort(final int[] keys, final float[] floatValues) {
        // TODO Auto-generated method stub

    }

    @Override
    public void sort(final int[] keys, final float[] floatValues, final short[] shortValues) {
        // TODO Auto-generated method stub

    }

}

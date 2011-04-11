/*
 * Copyright 2010, 2011 Aaron Dunlop and Nathan Bodenstab
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
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>
 */
package edu.ohsu.cslu.parser.spmv;

import com.nativelibs4java.opencl.CLFloatBuffer;
import com.nativelibs4java.opencl.CLIntBuffer;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLMem;
import com.nativelibs4java.opencl.CLShortBuffer;

import edu.ohsu.cslu.grammar.CsrSparseMatrixGrammar;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.PackedArrayChart;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.PackedArrayChartCell;
import edu.ohsu.cslu.parser.chart.ParallelArrayChart.ParallelArrayChartCell;
import edu.ohsu.cslu.util.OpenClUtils;

public class PackedOpenClSpmvParser extends OpenClSpmvParser<PackedArrayChart> {

    /**
     * Populated non-terminal indices. Parallel array with {@link OpenClSpmvParser#clChartInsideProbabilities}
     * , {@link OpenClSpmvParser#clChartPackedChildren}, and {@link OpenClSpmvParser#clChartMidpoints}
     */
    private CLShortBuffer clChartNonTerminalIndices;

    private CLIntBuffer clChartNumNonTerminals;

    protected final CLFloatBuffer clTmpCellInsideProbabilities;
    protected final CLIntBuffer clTmpCellPackedChildren;
    protected final CLShortBuffer clTmpCellMidpoints;
    protected final CLShortBuffer clPrefixSum;

    private final CLKernel prefixSumKernel;
    private final CLKernel packKernel;

    public PackedOpenClSpmvParser(final ParserDriver opts, final CsrSparseMatrixGrammar grammar) {
        super(opts, grammar);

        try {
            prefixSumKernel = clProgram.createKernel("prefixSum");
            packKernel = clProgram.createKernel("pack");
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        // Allocate OpenCL-hosted memory for temporary cell storage, to perform SpMV into before packing into
        // the actual chart storage
        clTmpCellInsideProbabilities = context.createFloatBuffer(CLMem.Usage.InputOutput,
            grammar.numNonTerms());
        clTmpCellPackedChildren = context.createIntBuffer(CLMem.Usage.InputOutput, grammar.numNonTerms());
        clTmpCellMidpoints = context.createShortBuffer(CLMem.Usage.InputOutput, grammar.numNonTerms());

        clPrefixSum = context.createShortBuffer(CLMem.Usage.InputOutput, grammar.numNonTerms());
    }

    @Override
    protected void initSentence(final int[] tokens) {
        final int sentLength = tokens.length;
        if (chart == null || chart.size() < sentLength) {
            chart = new PackedArrayChart(tokens, grammar);
            clChartNumNonTerminals = context.createIntBuffer(CLMem.Usage.InputOutput, chart.cells);
        } else {
            chart.clear(sentLength);
        }

        super.initSentence(tokens);
    }

    @Override
    protected void allocateOpenClChart() {

        super.allocateOpenClChart();

        if (clChartNonTerminalIndices != null) {
            clChartNonTerminalIndices.release();
        }
        clChartNonTerminalIndices = context
            .createShortBuffer(CLMem.Usage.InputOutput, chart.chartArraySize());
    }

    @Override
    protected void internalCartesianProduct(final ParallelArrayChartCell leftCell,
            final ParallelArrayChartCell rightCell, final CLFloatBuffer tmpClCartesianProductProbabilities,
            final CLShortBuffer tmpClCartesianProductMidpoints) {

        final PackedArrayChartCell packedLeftCell = (PackedArrayChartCell) leftCell;
        final PackedArrayChartCell packedRightCell = (PackedArrayChartCell) rightCell;

        final int leftChildrenStart = packedLeftCell.minLeftChildIndex();
        final int observedLeftChildren = packedLeftCell.maxLeftChildIndex() - leftChildrenStart + 1;
        final int rightChildrenStart = packedRightCell.offset();
        final int observedRightChildren = packedRightCell.maxRightChildIndex() - rightChildrenStart + 1;

        // Bind the arguments of the OpenCL kernel
        cartesianProductKernel.setArgs(clChartNonTerminalIndices, clChartInsideProbabilities,
            clChartPackedChildren, clChartMidpoints, leftChildrenStart, observedLeftChildren,
            rightChildrenStart, observedRightChildren, tmpClCartesianProductProbabilities,
            tmpClCartesianProductMidpoints, rightCell.start());

        // Call the kernel and wait for results
        final int globalWorkSize = edu.ohsu.cslu.util.Math.roundUp(
            (observedLeftChildren * observedRightChildren), LOCAL_WORK_SIZE);
        if (globalWorkSize > 0) {
            cartesianProductKernel.enqueueNDRange(clQueue, new int[] { globalWorkSize },
                new int[] { LOCAL_WORK_SIZE });
            clQueue.finish();
        }
    }

    @Override
    protected void internalBinarySpmvMultiply(final ParallelArrayChartCell chartCell) {

        final PackedArrayChartCell packedChartCell = (PackedArrayChartCell) chartCell;

        // Fill the temporary storage with negative infinity
        fillFloatKernel.setArgs(clTmpCellInsideProbabilities, grammar.numNonTerms(), Float.NEGATIVE_INFINITY);
        final int fillFloatGlobalWorkSize = edu.ohsu.cslu.util.Math.roundUp(grammar.numNonTerms(),
            LOCAL_WORK_SIZE);
        fillFloatKernel.enqueueNDRange(clQueue, new int[] { fillFloatGlobalWorkSize },
            new int[] { LOCAL_WORK_SIZE });

        // Execute the Binary SpMV kernel
        binarySpmvKernel.setArgs(clTmpCellInsideProbabilities, clTmpCellPackedChildren, clTmpCellMidpoints,
            clCartesianProductProbabilities0, clCartesianProductMidpoints0, clBinaryRuleMatrixRowIndices,
            clBinaryRuleMatrixColumnIndices, clBinaryRuleMatrixProbabilities, grammar.numNonTerms());
        final int spmvGlobalWorkSize = edu.ohsu.cslu.util.Math
            .roundUp(grammar.numNonTerms(), LOCAL_WORK_SIZE);
        binarySpmvKernel.enqueueNDRange(clQueue, new int[] { spmvGlobalWorkSize },
            new int[] { LOCAL_WORK_SIZE });
        clQueue.finish();

        // TODO This doesn't really belong in an internal method
        // Copy temporary cell contents from device memory into the cell's temporary storage
        packedChartCell.allocateTemporaryStorage();
        OpenClUtils.copyFromDevice(clQueue, clTmpCellInsideProbabilities,
            packedChartCell.tmpInsideProbabilities);
        OpenClUtils.copyFromDevice(clQueue, clTmpCellPackedChildren, packedChartCell.tmpPackedChildren);
        OpenClUtils.copyFromDevice(clQueue, clTmpCellMidpoints, packedChartCell.tmpMidpoints);
    }

    @Override
    protected void internalUnarySpmvMultiply(final ParallelArrayChartCell chartCell) {

        final PackedArrayChartCell packedChartCell = (PackedArrayChartCell) chartCell;

        // TODO This doesn't really belong in an internal method
        packedChartCell.allocateTemporaryStorage();
        OpenClUtils.copyToDevice(clQueue, clTmpCellInsideProbabilities,
            packedChartCell.tmpInsideProbabilities);
        OpenClUtils.copyToDevice(clQueue, clTmpCellPackedChildren, packedChartCell.tmpPackedChildren);
        OpenClUtils.copyToDevice(clQueue, clTmpCellMidpoints, packedChartCell.tmpMidpoints);

        // Bind the arguments of the OpenCL kernel
        unarySpmvKernel.setArgs(clTmpCellInsideProbabilities, clTmpCellPackedChildren, clTmpCellMidpoints,
            clCsrUnaryRowStartIndices, clCsrUnaryColumnIndices, clCsrUnaryProbabilities,
            grammar.numNonTerms(), chartCell.end());

        // Call the kernel and wait for results
        final int globalWorkSize = edu.ohsu.cslu.util.Math.roundUp(grammar.numNonTerms(), LOCAL_WORK_SIZE);
        unarySpmvKernel.enqueueNDRange(clQueue, new int[] { globalWorkSize }, new int[] { LOCAL_WORK_SIZE });
        clQueue.finish();

        // TODO This doesn't really belong in an internal method
        // Copy temporary cell contents from device memory into the cell's temporary storage
        OpenClUtils.copyFromDevice(clQueue, clTmpCellInsideProbabilities,
            packedChartCell.tmpInsideProbabilities);
        OpenClUtils.copyFromDevice(clQueue, clTmpCellPackedChildren, packedChartCell.tmpPackedChildren);
        OpenClUtils.copyFromDevice(clQueue, clTmpCellMidpoints, packedChartCell.tmpMidpoints);
    }

    @Override
    protected void finalizeCell(final ParallelArrayChartCell chartCell) {

        final PackedArrayChartCell packedCell = (PackedArrayChartCell) chartCell;

        // Call the prefix-sum and pack kernels
        final int globalWorkSize = edu.ohsu.cslu.util.Math.roundUp(grammar.numNonTerms(), LOCAL_WORK_SIZE);

        prefixSumKernel.setArgs(clTmpCellInsideProbabilities, clPrefixSum, clChartNumNonTerminals,
            chart.cellIndex(packedCell.start(), packedCell.end()), grammar.numNonTerms());
        // TODO Thread prefix sum kernel
        prefixSumKernel.enqueueNDRange(clQueue, new int[] { 1 }, new int[] { 1 });

        packKernel.setArgs(clTmpCellInsideProbabilities, clTmpCellPackedChildren, clTmpCellMidpoints,
            clPrefixSum, clChartNonTerminalIndices, clChartInsideProbabilities, clChartPackedChildren,
            clChartMidpoints, chartCell.offset(), grammar.numNonTerms());
        packKernel.enqueueNDRange(clQueue, new int[] { globalWorkSize }, new int[] { LOCAL_WORK_SIZE });

        clQueue.finish();

        // We still have to compute min and max indices on the CPU
        packedCell.clearTemporaryStorage();
        copyChartFromDevice();
        packedCell.allocateTemporaryStorage();
        packedCell.finalizeCell();
    }

    @Override
    protected void copyChartToDevice() {
        OpenClUtils.copyToDevice(clQueue, clChartNonTerminalIndices, chart.nonTerminalIndices);
        OpenClUtils.copyToDevice(clQueue, clChartNumNonTerminals, chart.numNonTerminals());
        super.copyChartToDevice();
    }

    @Override
    protected void copyChartFromDevice() {
        OpenClUtils.copyFromDevice(clQueue, clChartNonTerminalIndices, chart.nonTerminalIndices);
        OpenClUtils.copyFromDevice(clQueue, clChartNumNonTerminals, chart.numNonTerminals());
        super.copyChartFromDevice();
    }
}

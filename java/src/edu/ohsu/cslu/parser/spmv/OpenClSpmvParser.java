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

import static com.nativelibs4java.opencl.JavaCL.createBestContext;

import java.io.StringWriter;

import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLFloatBuffer;
import com.nativelibs4java.opencl.CLIntBuffer;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLMem;
import com.nativelibs4java.opencl.CLProgram;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.CLShortBuffer;

import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.grammar.CsrSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.LeftShiftFunction;
import edu.ohsu.cslu.parser.ChartParser;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.chart.DenseVectorChart.DenseVectorChartCell;
import edu.ohsu.cslu.parser.chart.ParallelArrayChart;
import edu.ohsu.cslu.parser.chart.ParallelArrayChart.ParallelArrayChartCell;
import edu.ohsu.cslu.util.OpenClUtils;

/**
 * {@link SparseMatrixVectorParser} which uses a sparse grammar stored in CSR format ( {@link CsrSparseMatrixGrammar})
 * and implements cross-product and SpMV multiplication using OpenCL kernels.
 * 
 * @author Aaron Dunlop
 * @since Feb 11, 2010
 */
public abstract class OpenClSpmvParser<C extends ParallelArrayChart> extends
        SparseMatrixVectorParser<CsrSparseMatrixGrammar, C> {

    protected final static int LOCAL_WORK_SIZE = 64;

    protected long sentenceCartesianProductUnionTime = 0;

    protected final CLContext context;
    protected final CLProgram clProgram;
    protected final CLKernel fillFloatKernel;
    protected final CLKernel binarySpmvKernel;
    protected final CLKernel unarySpmvKernel;
    protected final CLKernel cartesianProductKernel;
    protected final CLKernel cartesianProductUnionKernel;
    protected final CLQueue clQueue;

    /** Grammar binary rules stored on device */
    protected final CLIntBuffer clBinaryRuleMatrixRowIndices;
    protected final CLIntBuffer clBinaryRuleMatrixColumnIndices;
    protected final CLFloatBuffer clBinaryRuleMatrixProbabilities;

    /** Grammar unary rules stored on device */
    protected final CLIntBuffer clCsrUnaryRowStartIndices;
    protected final CLShortBuffer clCsrUnaryColumnIndices;
    protected final CLFloatBuffer clCsrUnaryProbabilities;

    /** Chart */
    protected CLFloatBuffer clChartInsideProbabilities;
    protected CLIntBuffer clChartPackedChildren;
    protected CLShortBuffer clChartMidpoints;

    // Cartesian-product vector
    protected final CLFloatBuffer clCartesianProductProbabilities0;
    protected final CLShortBuffer clCartesianProductMidpoints0;
    protected final CLFloatBuffer clCartesianProductProbabilities1;
    protected final CLShortBuffer clCartesianProductMidpoints1;

    private int chartSize;

    protected OpenClSpmvParser(final ParserDriver opts, final CsrSparseMatrixGrammar grammar) {
        super(opts, grammar);

        context = createBestContext();
        clQueue = context.createDefaultQueue();

        try {
            // Construct the #defines, etc. and prepend to the OpenCL source code.
            final StringWriter prefix = new StringWriter();
            prefix.write("#define UNARY_PRODUCTION " + Production.UNARY_PRODUCTION + '\n');
            prefix.write("#define LEXICAL_PRODUCTION " + Production.LEXICAL_PRODUCTION + '\n');
            prefix.write("#define PACKING_SHIFT " + ((LeftShiftFunction) grammar.packingFunction()).shift
                    + '\n');
            prefix.write("#define MAX_PACKED_LEXICAL_PRODUCTION "
                    + ((LeftShiftFunction) grammar.packingFunction()).maxPackedLexicalProduction + '\n');
            prefix.write("#define PACKING_SHIFT " + ((LeftShiftFunction) grammar.packingFunction()).shift
                    + '\n');
            prefix.write(grammar.packingFunction().openClPackDefine() + '\n');
            prefix.write(grammar.packingFunction().openClUnpackLeftChild() + '\n');

            // Compile kernels shared by all implementing classes
            final CLProgram clSharedProgram = OpenClUtils.compileClKernels(context, OpenClSpmvParser.class,
                    prefix.toString());
            fillFloatKernel = clSharedProgram.createKernel("fillFloat");
            cartesianProductUnionKernel = clSharedProgram.createKernel("cartesianProductUnion");

            // Compile kernels specific to the subclass
            clProgram = OpenClUtils.compileClKernels(context, getClass(), prefix.toString());
            cartesianProductKernel = clProgram.createKernel("cartesianProduct");
            binarySpmvKernel = clProgram.createKernel("binarySpmv");
            unarySpmvKernel = clProgram.createKernel("unarySpmv");

        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        // Allocate OpenCL-hosted memory for binary rules and copy to the device
        clBinaryRuleMatrixRowIndices = OpenClUtils
                .copyToDevice(clQueue, grammar.csrBinaryRowIndices, CLMem.Usage.Input);
        clBinaryRuleMatrixColumnIndices = OpenClUtils.copyToDevice(clQueue, grammar.csrBinaryColumnIndices,
                CLMem.Usage.Input);
        clBinaryRuleMatrixProbabilities = OpenClUtils.copyToDevice(clQueue, grammar.csrBinaryProbabilities,
                CLMem.Usage.Input);

        // Repeat for unary rules
        clCsrUnaryRowStartIndices = OpenClUtils.copyToDevice(clQueue, grammar.csrUnaryRowStartIndices,
                CLMem.Usage.Input);
        clCsrUnaryColumnIndices = OpenClUtils.copyToDevice(clQueue, grammar.csrUnaryColumnIndices, CLMem.Usage.Input);
        clCsrUnaryProbabilities = OpenClUtils.copyToDevice(clQueue, grammar.csrUnaryProbabilities, CLMem.Usage.Input);

        // And for cross-product storage
        clCartesianProductProbabilities0 = context.createFloatBuffer(CLMem.Usage.InputOutput, grammar
                .packingFunction().packedArraySize());
        clCartesianProductMidpoints0 = context.createShortBuffer(CLMem.Usage.InputOutput, grammar
                .packingFunction().packedArraySize());
        clCartesianProductProbabilities1 = context.createFloatBuffer(CLMem.Usage.InputOutput, grammar
                .packingFunction().packedArraySize());
        clCartesianProductMidpoints1 = context.createShortBuffer(CLMem.Usage.InputOutput, grammar
                .packingFunction().packedArraySize());
    }

    @Override
    protected void initSentence(final ParseTask parseTask) {
        super.initSentence(parseTask);

        if (clChartInsideProbabilities == null || parseTask.sentenceLength() > chartSize) {
            allocateOpenClChart();
        }

        sentenceCartesianProductUnionTime = 0;
    }

    /**
     * Duplicated here from {@link ChartParser} so we can copy the parse chart from the GPU to main memory after
     * parsing.
     * 
     * TODO Call super.findBestParse() and just copy here?
     */
    @Override
    public BinaryTree<String> findBestParse(final ParseTask parseTask) {
        initSentence(parseTask);
        cellSelector.initSentence(this);

        // For most parsers, we populate lexical entries during parsing, but for OpenCL parsers, we have to do it
        // ahead-of-time so we can copy them to the GPU chart storage
        for (int start = 0; start < chart.size(); start++) {
            addLexicalProductions(chart.getCell(start, start + 1));
        }
        copyChartToDevice();

        while (cellSelector.hasNext()) {
            final short[] startAndEnd = cellSelector.next();
            computeInsideProbabilities(chart.getCell(startAndEnd[0], startAndEnd[1]));
        }

        copyChartFromDevice();
        return chart.extractBestParse(grammar.startSymbol);
    }

    /**
     * De-allocates current OpenCL chart storage (if any) and allocates storage adequate for the current sentence
     * length.
     */
    protected void allocateOpenClChart() {

        if (clChartInsideProbabilities != null) {
            clChartInsideProbabilities.release();
            clChartInsideProbabilities = null;
            clChartPackedChildren.release();
            clChartPackedChildren = null;
            clChartMidpoints.release();
            clChartMidpoints = null;
        }

        // Allocate OpenCL-hosted memory for chart storage
        clChartInsideProbabilities = context.createFloatBuffer(CLMem.Usage.InputOutput, chart.chartArraySize());
        clChartPackedChildren = context.createIntBuffer(CLMem.Usage.InputOutput, chart.chartArraySize());
        clChartMidpoints = context.createShortBuffer(CLMem.Usage.InputOutput, chart.chartArraySize());
        chartSize = chart.size();
    }

    // @Override
    // protected void addLexicalProductions(final ParseTask parseTask) {
    // // Populate the lexical productions and part-of-speech tags in CPU space, and then copy the chart to
    // // device memory.
    // super.addLexicalProductions(parseTask);
    // copyChartToDevice();
    // }

    /**
     * TODO If possible, merge or share code with {@link SparseMatrixVectorParser#computeInsideProbabilities(ChartCell)}
     */
    @Override
    protected void computeInsideProbabilities(final ChartCell cell) {

        final long t0 = collectDetailedStatistics ? System.nanoTime() : 0;

        final DenseVectorChartCell spvChartCell = (DenseVectorChartCell) cell;
        final short start = cell.start();
        final short end = cell.end();

        long t2 = 0;

        // Perform binary grammar intersection for span > 1 cells
        if (end - start > 1) {

            internalCartesianProductUnion(start, end);

            final long t1 = System.nanoTime();
            final long time = t1 - t0;
            sentenceCartesianProductTime += time;
            totalCartesianProductTime += time;

            // Multiply the unioned vector with the grammar matrix and populate the current cell with the
            // vector resulting from the matrix-vector multiplication
            internalBinarySpmvMultiply(spvChartCell);
        }

        if (collectDetailedStatistics) {
            t2 = System.nanoTime();
            chart.parseTask.insideBinaryNs += t2 - t0;
        }

        // Handle unary productions
        // TODO: This only goes through unary rules one time, so it can't create unary chains unless such
        // chains are encoded in the grammar. Iterating a few times would probably
        // work, although it's a big-time hack.
        internalUnarySpmvMultiply(spvChartCell);

        final long t3 = System.nanoTime();
        chart.parseTask.unaryAndPruningNs += (t3 - t2);

        finalizeCell(spvChartCell);
        sentenceFinalizeTime += (System.currentTimeMillis() - t3);

    }

    /**
     * Takes the cross-product of all potential child-cell combinations. Unions those cross-products together, saving
     * the maximum probability child combinations. This version copies the chart into device memory before taking the
     * cartesian product and copies the result back into main memory. Primarily to enable unit testing of
     * {@link #internalCartesianProductUnion(int, int)}.
     * 
     * @param start
     * @param end
     * @return Unioned cross-product
     */
    @Override
    protected CartesianProductVector cartesianProductUnion(final int start, final int end) {

        copyChartToDevice();

        internalCartesianProductUnion(start, end);

        final int packedArraySize = grammar.packingFunction().packedArraySize();
        final float[] probabilities = OpenClUtils.copyFromDevice(clQueue, clCartesianProductProbabilities0,
                packedArraySize);
        final short[] midpoints = OpenClUtils.copyFromDevice(clQueue, clCartesianProductMidpoints0, packedArraySize);

        int size = 0;
        for (int i = 0; i < probabilities.length; i++) {
            if (probabilities[i] != Float.NEGATIVE_INFINITY) {
                size++;
            }
        }

        return new CartesianProductVector(grammar, probabilities, midpoints, size);
    }

    /**
     * Performs the actual cartesian-product union via OpenCL kernel calls.
     * 
     * @param start
     * @param end
     */
    protected void internalCartesianProductUnion(final int start, final int end) {

        long t0 = System.currentTimeMillis();

        // Fill the buffer with negative infinity
        fillFloatKernel.setArgs(clCartesianProductProbabilities0, grammar.packingFunction().packedArraySize(),
                Float.NEGATIVE_INFINITY);
        final int globalWorkSize = edu.ohsu.cslu.util.Math.roundUp(
                grammar.packingFunction().packedArraySize(), LOCAL_WORK_SIZE);
        fillFloatKernel.enqueueNDRange(clQueue, new int[] { globalWorkSize }, new int[] { LOCAL_WORK_SIZE });
        clQueue.finish();

        // Compute the cartesian-product of the first midpoint separately
        internalCartesianProduct(chart.getCell(start, start + 1), chart.getCell(start + 1, end),
                clCartesianProductProbabilities0, clCartesianProductMidpoints0);

        long t1 = System.currentTimeMillis();
        sentenceCartesianProductTime += (t1 - t0);

        // Iterate over all other midpoints, unioning together the cartesian-product of discovered
        // non-terminals in each left/right child pair
        for (short midpoint = (short) (start + 2); midpoint <= end - 1; midpoint++) {

            final ParallelArrayChartCell leftCell = chart.getCell(start, midpoint);
            final ParallelArrayChartCell rightCell = chart.getCell(midpoint, end);

            t0 = System.currentTimeMillis();

            // Initialize the target cartesian product array
            fillFloatKernel.setArgs(clCartesianProductProbabilities1, grammar.packingFunction()
                    .packedArraySize(), Float.NEGATIVE_INFINITY);
            fillFloatKernel.enqueueNDRange(clQueue, new int[] { globalWorkSize }, new int[] { LOCAL_WORK_SIZE });
            clQueue.finish();

            // Perform the cartesian product
            internalCartesianProduct(leftCell, rightCell, clCartesianProductProbabilities1,
                    clCartesianProductMidpoints1);

            t1 = System.currentTimeMillis();
            sentenceCartesianProductTime += (t1 - t0);

            // Union the new cross-product with the existing cross-product
            cartesianProductUnionKernel.setArgs(clCartesianProductProbabilities0, clCartesianProductMidpoints0,
                    clCartesianProductProbabilities1, clCartesianProductMidpoints1, grammar.packingFunction()
                            .packedArraySize());
            cartesianProductUnionKernel.enqueueNDRange(clQueue, new int[] { globalWorkSize },
                    new int[] { LOCAL_WORK_SIZE });
            clQueue.finish();

            sentenceCartesianProductUnionTime += (System.currentTimeMillis() - t1);
        }
    }

    /**
     * Creates a single cartesian-product via OpenCL kernel calls. Must be implemented by subclasses.
     * 
     * @param leftCell
     * @param rightCell
     * @param tmpClCrossProductProbabilities
     * @param tmpClCrossProductMidpoints
     */
    protected abstract void internalCartesianProduct(final ParallelArrayChartCell leftCell,
            final ParallelArrayChartCell rightCell, final CLFloatBuffer tmpClCrossProductProbabilities,
            final CLShortBuffer tmpClCrossProductMidpoints);

    /**
     * Performs the binary-rule grammar intersection by Sparse Matrix-Vector multiplication on the OpenCL device.
     * 
     * This version copies the cross-product to device memory and the resulting chart cell back into main memory.
     * Primarily for unit testing of the internal implementation.
     */
    @Override
    public void binarySpmv(final CartesianProductVector cartesianProductVector, final ChartCell chartCell) {

        copyChartToDevice();

        // Copy cross-product to OpenCL memory
        OpenClUtils.copyToDevice(clQueue, clCartesianProductProbabilities0, cartesianProductVector.probabilities);
        OpenClUtils.copyToDevice(clQueue, clCartesianProductMidpoints0, cartesianProductVector.midpoints);

        internalBinarySpmvMultiply(((ParallelArrayChartCell) chartCell));

        copyChartFromDevice();
    }

    /**
     * Performs the binary-rule grammar intersection by Sparse Matrix-Vector multiplication on the OpenCL device.
     */
    protected abstract void internalBinarySpmvMultiply(final ParallelArrayChartCell chartCell);

    /**
     * Performs the unary-rule grammar intersection by Sparse Matrix-Vector multiplication on the OpenCL device.
     * 
     * This version copies the current cell population to device memory and the results back into main memory. Primarily
     * for unit testing of the internal implementation.
     */
    @Override
    public void unarySpmv(final ChartCell chartCell) {

        // Copy current chart cell entries to OpenCL memory
        copyChartToDevice();

        internalUnarySpmvMultiply((ParallelArrayChartCell) chartCell);

        copyChartFromDevice();
    }

    /**
     * Performs the unary-rule grammar intersection by Sparse Matrix-Vector multiplication on the OpenCL device.
     */
    protected abstract void internalUnarySpmvMultiply(final ParallelArrayChartCell chartCell);

    protected void finalizeCell(final ParallelArrayChartCell chartCell) {
        chartCell.finalizeCell();
    }

    /**
     * Copies the chart from main memory to device memory.
     */
    protected void copyChartToDevice() {
        OpenClUtils.copyToDevice(clQueue, clChartInsideProbabilities, chart.insideProbabilities);
        OpenClUtils.copyToDevice(clQueue, clChartPackedChildren, chart.packedChildren);
        OpenClUtils.copyToDevice(clQueue, clChartMidpoints, chart.midpoints);
    }

    /**
     * Copies the chart from device memory to main memory.
     */
    protected void copyChartFromDevice() {
        OpenClUtils.copyFromDevice(clQueue, clChartInsideProbabilities, chart.insideProbabilities);
        OpenClUtils.copyFromDevice(clQueue, clChartPackedChildren, chart.packedChildren);
        OpenClUtils.copyFromDevice(clQueue, clChartMidpoints, chart.midpoints);
    }
}

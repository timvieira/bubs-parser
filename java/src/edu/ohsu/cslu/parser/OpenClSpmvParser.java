package edu.ohsu.cslu.parser;

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

import edu.ohsu.cslu.grammar.CsrSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.LeftShiftFunction;
import edu.ohsu.cslu.parser.chart.ParallelArrayChart;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.chart.DenseVectorChart.DenseVectorChartCell;
import edu.ohsu.cslu.parser.chart.ParallelArrayChart.ParallelArrayChartCell;
import edu.ohsu.cslu.parser.util.ParseTree;
import edu.ohsu.cslu.util.OpenClUtils;

/**
 * {@link SparseMatrixVectorParser} which uses a sparse grammar stored in CSR format (
 * {@link CsrSparseMatrixGrammar}) and implements cross-product and SpMV multiplication using OpenCL kernels.
 * 
 * @author Aaron Dunlop
 * @since Feb 11, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public abstract class OpenClSpmvParser<C extends ParallelArrayChart> extends
        SparseMatrixVectorParser<CsrSparseMatrixGrammar, C> {

    protected final static int LOCAL_WORK_SIZE = 64;

    protected final CLContext context;
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
    protected final CLIntBuffer clUnaryRuleMatrixRowIndices;
    protected final CLIntBuffer clUnaryRuleMatrixColumnIndices;
    protected final CLFloatBuffer clUnaryRuleMatrixProbabilities;

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

    protected OpenClSpmvParser(final ParserOptions opts, final CsrSparseMatrixGrammar grammar) {
        super(opts, grammar);

        context = createBestContext();
        clQueue = context.createDefaultQueue();

        try {
            // Compile OpenCL kernels
            final StringWriter prefix = new StringWriter();
            prefix.write("#define UNARY_PRODUCTION " + Production.UNARY_PRODUCTION + '\n');
            prefix.write("#define LEXICAL_PRODUCTION " + Production.LEXICAL_PRODUCTION + '\n');
            prefix.write("#define PACKING_SHIFT "
                    + ((LeftShiftFunction) grammar.cartesianProductFunction()).shift + '\n');
            prefix.write("#define MAX_PACKED_LEXICAL_PRODUCTION "
                    + ((LeftShiftFunction) grammar.cartesianProductFunction()).maxPackedLexicalProduction
                    + '\n');
            prefix.write("#define PACKING_SHIFT "
                    + ((LeftShiftFunction) grammar.cartesianProductFunction()).shift + '\n');
            prefix.write(grammar.cartesianProductFunction().openClPackDefine() + '\n');
            prefix.write(grammar.cartesianProductFunction().openClUnpackLeftChild() + '\n');

            // Compile kernels shared by all implementing classes
            final CLProgram sharedProgram = OpenClUtils.compileClKernels(context, OpenClSpmvParser.class,
                prefix.toString());
            fillFloatKernel = sharedProgram.createKernel("fillFloat");
            cartesianProductUnionKernel = sharedProgram.createKernel("cartesianProductUnion");

            // Compile kernels specific to the subclass
            final CLProgram program = OpenClUtils.compileClKernels(context, getClass(), prefix.toString());
            cartesianProductKernel = program.createKernel("cartesianProduct");
            binarySpmvKernel = program.createKernel("binarySpmvMultiply");
            unarySpmvKernel = program.createKernel("unarySpmvMultiply");

        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        // Allocate OpenCL-hosted memory for binary rules and copy to the device
        clBinaryRuleMatrixRowIndices = OpenClUtils.copyToDevice(clQueue,
            grammar.binaryRuleMatrixRowIndices(), CLMem.Usage.Input);
        clBinaryRuleMatrixColumnIndices = OpenClUtils.copyToDevice(clQueue, grammar
            .binaryRuleMatrixColumnIndices(), CLMem.Usage.Input);
        clBinaryRuleMatrixProbabilities = OpenClUtils.copyToDevice(clQueue, grammar
            .binaryRuleMatrixProbabilities(), CLMem.Usage.Input);

        // Repeat for unary rules
        clUnaryRuleMatrixRowIndices = OpenClUtils.copyToDevice(clQueue, grammar.unaryRuleMatrixRowIndices(),
            CLMem.Usage.Input);
        clUnaryRuleMatrixColumnIndices = OpenClUtils.copyToDevice(clQueue, grammar
            .unaryRuleMatrixColumnIndices(), CLMem.Usage.Input);
        clUnaryRuleMatrixProbabilities = OpenClUtils.copyToDevice(clQueue, grammar
            .unaryRuleMatrixProbabilities(), CLMem.Usage.Input);

        // And for cross-product storage
        clCartesianProductProbabilities0 = context.createFloatBuffer(CLMem.Usage.InputOutput, grammar
            .cartesianProductFunction().packedArraySize());
        clCartesianProductMidpoints0 = context.createShortBuffer(CLMem.Usage.InputOutput, grammar
            .cartesianProductFunction().packedArraySize());
        clCartesianProductProbabilities1 = context.createFloatBuffer(CLMem.Usage.InputOutput, grammar
            .cartesianProductFunction().packedArraySize());
        clCartesianProductMidpoints1 = context.createShortBuffer(CLMem.Usage.InputOutput, grammar
            .cartesianProductFunction().packedArraySize());
    }

    protected OpenClSpmvParser(final CsrSparseMatrixGrammar grammar) {
        this(new ParserOptions().setCollectDetailedStatistics(true), grammar);
    }

    @Override
    protected void initParser(final int sentLength) {
        super.initParser(sentLength);

        if (clChartInsideProbabilities == null || sentLength > chartSize) {
            allocateOpenClChart();
        }
    }

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
        clChartInsideProbabilities = context.createFloatBuffer(CLMem.Usage.InputOutput, chart
            .chartArraySize());
        clChartPackedChildren = context.createIntBuffer(CLMem.Usage.InputOutput, chart.chartArraySize());
        clChartMidpoints = context.createShortBuffer(CLMem.Usage.InputOutput, chart.chartArraySize());
        chartSize = chart.size();
    }

    @Override
    protected void addLexicalProductions(final int[] sent) throws Exception {
        super.addLexicalProductions(sent);
        copyChartToDevice();
    }

    @Override
    protected ParseTree extractBestParse(final ChartCell cell, final int nonTermIndex) {
        copyChartFromDevice();
        return super.extractBestParse(cell, nonTermIndex);
    }

    @Override
    protected void visitCell(final short start, final short end) {

        final ParallelArrayChartCell spvChartCell = chart.getCell(start, end);

        // final long t0 = System.currentTimeMillis();

        long t2;
        long binarySpmvTime = 0;
        // Skip binary grammar intersection for span-1 cells
        if (end - start > 1) {
            // final CrossProductVector cartesianProductVector = cartesianProductUnion(start, end);
            internalCartesianProductUnion(start, end);

            final long t1 = System.currentTimeMillis();

            // Multiply the unioned vector with the grammar matrix and populate the current cell with the
            // vector resulting from the matrix-vector multiplication
            // binarySpmvMultiply(cartesianProductVector, spvChartCell);

            // Copy cross-product to OpenCL memory
            // OpenClUtils.copyToDevice(clCrossProductProbabilities0, cartesianProductVector.probabilities);
            // OpenClUtils.copyToDevice(clCrossProductMidpoints0, cartesianProductVector.midpoints);

            internalBinarySpmvMultiply(spvChartCell.offset());

            t2 = System.currentTimeMillis();
            binarySpmvTime = t2 - t1;

        } else {
            t2 = System.currentTimeMillis();
        }

        // Handle unary productions
        // TODO: This only goes through unary rules one time, so it can't create unary chains unless such
        // chains are encoded in the grammar. Iterating a few times would probably
        // work, although it's a big-time hack.
        internalUnarySpmvMultiply(spvChartCell.offset(), end);

        final long t3 = System.currentTimeMillis();
        final long unarySpmvTime = t3 - t2;

        spvChartCell.finalizeCell();

        // System.out.format("Visited cell: %2d,%2d (%5d ms). Cross-product: %6d/%6d combinations (%5.0f ms, %4.2f/ms), Multiply: %5d edges (%5.0f ms, %4.2f /ms)\n",
        // start, end, t3
        // - t0, cartesianProductSize, totalProducts, cartesianProductTime, cartesianProductSize /
        // cartesianProductTime,
        // edges, spmvTime, edges / spmvTime);
        totalSpMVTime += binarySpmvTime + unarySpmvTime;
    }

    /**
     * Takes the cross-product of all potential child-cell combinations. Unions those cross-products together,
     * saving the maximum probability child combinations. This version copies the cross-product back into main
     * memory.
     * 
     * @param start
     * @param end
     * @return Unioned cross-product
     */
    @Override
    protected CartesianProductVector cartesianProductUnion(final int start, final int end) {

        copyChartToDevice();

        internalCartesianProductUnion(start, end);

        final int packedArraySize = grammar.cartesianProductFunction().packedArraySize();
        final float[] probabilities = OpenClUtils.copyFromDevice(clQueue, clCartesianProductProbabilities0,
            packedArraySize);
        final short[] midpoints = OpenClUtils.copyFromDevice(clQueue, clCartesianProductMidpoints0,
            packedArraySize);

        int size = 0;
        for (int i = 0; i < probabilities.length; i++) {
            if (probabilities[i] != Float.NEGATIVE_INFINITY) {
                size++;
            }
        }

        return new CartesianProductVector(grammar, probabilities, midpoints, size);
    }

    protected void internalCartesianProductUnion(final int start, final int end) {

        long t0 = System.currentTimeMillis();

        // Fill the buffer with negative infinity
        fillFloatKernel.setArgs(clCartesianProductProbabilities0, grammar.cartesianProductFunction()
            .packedArraySize(), Float.NEGATIVE_INFINITY);
        final int globalWorkSize = edu.ohsu.cslu.util.Math.roundUp(grammar.cartesianProductFunction()
            .packedArraySize(), LOCAL_WORK_SIZE);
        fillFloatKernel.enqueueNDRange(clQueue, new int[] { globalWorkSize }, new int[] { LOCAL_WORK_SIZE });
        clQueue.finish();

        // Compute the cartesian-product of the first midpoint separately
        internalCartesianProduct(chart.getCell(start, start + 1), chart.getCell(start + 1, end),
            clCartesianProductProbabilities0, clCartesianProductMidpoints0);

        long t1 = System.currentTimeMillis();
        totalCartesianProductTime += (t1 - t0);

        // Iterate over all other midpoints, unioning together the cartesian-product of discovered
        // non-terminals in each left/right child pair
        for (short midpoint = (short) (start + 2); midpoint <= end - 1; midpoint++) {

            final ParallelArrayChartCell leftCell = chart.getCell(start, midpoint);
            final ParallelArrayChartCell rightCell = chart.getCell(midpoint, end);

            t0 = System.currentTimeMillis();

            // Initialize the target cartesian product array
            fillFloatKernel.setArgs(clCartesianProductProbabilities1, grammar.cartesianProductFunction()
                .packedArraySize(), Float.NEGATIVE_INFINITY);
            fillFloatKernel.enqueueNDRange(clQueue, new int[] { globalWorkSize },
                new int[] { LOCAL_WORK_SIZE });
            clQueue.finish();

            // Perform the cartesian product
            internalCartesianProduct(leftCell, rightCell, clCartesianProductProbabilities1,
                clCartesianProductMidpoints1);

            t1 = System.currentTimeMillis();
            totalCartesianProductTime += (t1 - t0);

            // Union the new cross-product with the existing cross-product
            cartesianProductUnionKernel.setArgs(clCartesianProductProbabilities0,
                clCartesianProductMidpoints0, clCartesianProductProbabilities1, clCartesianProductMidpoints1,
                grammar.cartesianProductFunction().packedArraySize());
            cartesianProductUnionKernel.enqueueNDRange(clQueue, new int[] { globalWorkSize },
                new int[] { LOCAL_WORK_SIZE });
            clQueue.finish();

            totalCartesianProductUnionTime += (System.currentTimeMillis() - t1);
        }
    }

    protected abstract void internalCartesianProduct(final ParallelArrayChartCell leftCell,
            final ParallelArrayChartCell rightCell, final CLFloatBuffer tmpClCrossProductProbabilities,
            final CLShortBuffer tmpClCrossProductMidpoints);

    /**
     * This version copies the cross-product to device memory and the resulting chart cell back into main
     * memory. Useful for testing, but we can do better if we avoid the repeated copying.
     */
    @Override
    public void binarySpmvMultiply(final CartesianProductVector cartesianProductVector,
            final ChartCell chartCell) {

        copyChartToDevice();

        // Copy cross-product to OpenCL memory
        OpenClUtils.copyToDevice(clQueue, clCartesianProductProbabilities0,
            cartesianProductVector.probabilities);
        OpenClUtils.copyToDevice(clQueue, clCartesianProductMidpoints0, cartesianProductVector.midpoints);

        internalBinarySpmvMultiply(((DenseVectorChartCell) chartCell).offset());

        copyChartFromDevice();
    }

    protected abstract void internalBinarySpmvMultiply(final int chartCellOffset);

    /**
     * This version copies the current cell population to device memory and the results back into main memory.
     * Useful for testing, but we can do better if we avoid the repeated copying.
     */
    @Override
    public void unarySpmvMultiply(final ChartCell chartCell) {

        // Copy current chart cell entries to OpenCL memory
        copyChartToDevice();

        internalUnarySpmvMultiply(((DenseVectorChartCell) chartCell).offset(), (short) chartCell.end());

        copyChartFromDevice();
    }

    protected abstract void internalUnarySpmvMultiply(final int chartCellOffset, final short chartCellEnd);

    protected void copyChartToDevice() {
        OpenClUtils.copyToDevice(clQueue, clChartInsideProbabilities, chart.insideProbabilities);
        OpenClUtils.copyToDevice(clQueue, clChartPackedChildren, chart.packedChildren);
        OpenClUtils.copyToDevice(clQueue, clChartMidpoints, chart.midpoints);
    }

    protected void copyChartFromDevice() {
        OpenClUtils.copyFromDevice(clQueue, clChartInsideProbabilities, chart.insideProbabilities);
        OpenClUtils.copyFromDevice(clQueue, clChartPackedChildren, chart.packedChildren);
        OpenClUtils.copyFromDevice(clQueue, clChartMidpoints, chart.midpoints);
    }
}

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
import edu.ohsu.cslu.parser.chart.DenseVectorChart;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.chart.DenseVectorChart.DenseVectorChartCell;
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
public class OpenClSpmvParser extends SparseMatrixVectorParser<CsrSparseMatrixGrammar, DenseVectorChart> {

    private final static int LOCAL_WORK_SIZE = 64;

    private CLContext context;
    private CLKernel fillFloatKernel;
    private CLKernel binarySpmvKernel;
    private CLKernel unarySpmvKernel;
    private CLKernel cartesianProductKernel;
    private CLKernel cartesianProductUnionKernel;
    private CLQueue clQueue;

    // TODO Make these final once we move kernel compilation into the constructor

    /** Grammar binary rules stored on device */
    private CLIntBuffer clBinaryRuleMatrixRowIndices;
    private CLIntBuffer clBinaryRuleMatrixColumnIndices;
    private CLFloatBuffer clBinaryRuleMatrixProbabilities;

    /** Grammar unary rules stored on device */
    private CLIntBuffer clUnaryRuleMatrixRowIndices;
    private CLIntBuffer clUnaryRuleMatrixColumnIndices;
    private CLFloatBuffer clUnaryRuleMatrixProbabilities;

    private CLFloatBuffer clChartInsideProbabilities;
    private CLIntBuffer clChartPackedChildren;
    private CLShortBuffer clChartMidpoints;

    // Cross-product vector
    private CLFloatBuffer clCartesianProductProbabilities0;
    private CLShortBuffer clCartesianProductMidpoints0;
    private CLFloatBuffer clCartesianProductProbabilities1;
    private CLShortBuffer clCartesianProductMidpoints1;

    public OpenClSpmvParser(final ParserOptions opts, final CsrSparseMatrixGrammar grammar) {
        super(opts, grammar);

        context = createBestContext();
        clQueue = context.createDefaultQueue();
    }

    public OpenClSpmvParser(final CsrSparseMatrixGrammar grammar) {
        this(new ParserOptions().setCollectDetailedStatistics(true), grammar);
    }

    @Override
    protected void initParser(final int sentLength) {
        chart = new DenseVectorChart(sentLength, grammar);

        totalSpMVTime = 0;
        totalCartesianProductTime = 0;

        // TODO Move this to constructor after debugging

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

            final CLProgram program = OpenClUtils.compileClKernels(context, getClass(), prefix.toString());
            fillFloatKernel = program.createKernel("fillFloat");
            binarySpmvKernel = program.createKernel("binarySpmvMultiply");
            unarySpmvKernel = program.createKernel("unarySpmvMultiply");
            cartesianProductKernel = program.createKernel("cartesianProduct");
            cartesianProductUnionKernel = program.createKernel("cartesianProductUnion");
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

        // Allocate OpenCL-hosted memory for chart storage
        clChartInsideProbabilities = context.createFloatBuffer(CLMem.Usage.InputOutput, chart
            .chartArraySize());
        clChartPackedChildren = context.createIntBuffer(CLMem.Usage.InputOutput, chart.chartArraySize());
        clChartMidpoints = context.createShortBuffer(CLMem.Usage.InputOutput, chart.chartArraySize());

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

        final DenseVectorChartCell spvChartCell = chart.getCell(start, end);

        // final long t0 = System.currentTimeMillis();

        long t2;
        long binarySpmvTime = 0;
        // Skip binary grammar intersection for span-1 cells
        if (end - start > 1) {
            // final CrossProductVector cartesianProductVector = cartesianProductUnion(start, end);
            internalCrossProductUnion(start, end);

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

        internalCrossProductUnion(start, end);

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

    private void internalCrossProductUnion(final int start, final int end) {

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

            final DenseVectorChartCell leftCell = chart.getCell(start, midpoint);
            final DenseVectorChartCell rightCell = chart.getCell(midpoint, end);

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

    private void internalCartesianProduct(final DenseVectorChartCell leftCell,
            final DenseVectorChartCell rightCell, final CLFloatBuffer tmpClCrossProductProbabilities,
            final CLShortBuffer tmpClCrossProductMidpoints) {

        final int leftChildrenStart = grammar.posStart;
        final int validLeftChildren = grammar.unaryChildOnlyStart - leftChildrenStart;
        final int rightChildrenStart = 0;
        final int validRightChildren = grammar.leftChildOnlyStart - rightChildrenStart;

        // Bind the arguments of the OpenCL kernel
        cartesianProductKernel.setArgs(clChartInsideProbabilities, clChartPackedChildren, clChartMidpoints,
            leftCell.offset(), leftChildrenStart, validLeftChildren, rightCell.offset(), rightChildrenStart,
            validRightChildren, tmpClCrossProductProbabilities, tmpClCrossProductMidpoints, (short) rightCell
                .start());

        // Call the kernel and wait for results
        final int globalWorkSize = edu.ohsu.cslu.util.Math.roundUp((validLeftChildren * validRightChildren),
            LOCAL_WORK_SIZE);
        cartesianProductKernel.enqueueNDRange(clQueue, new int[] { globalWorkSize },
            new int[] { LOCAL_WORK_SIZE });

        clQueue.finish();
    }

    // private void printCrossProduct(final float[] cp) {
    // for (int i = 0; i < cp.length; i++) {
    // if (cp[i] != Float.NEGATIVE_INFINITY) {
    // System.out.format("%d : %.2f\n", i, cp[i]);
    // }
    // }
    // System.out.println();
    // }

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

    private void internalBinarySpmvMultiply(final int chartCellOffset) {

        // Bind the arguments of the OpenCL kernel
        binarySpmvKernel.setArgs(clChartInsideProbabilities, clChartPackedChildren, clChartMidpoints,
            chartCellOffset, clCartesianProductProbabilities0, clCartesianProductMidpoints0,
            clBinaryRuleMatrixRowIndices, clBinaryRuleMatrixColumnIndices, clBinaryRuleMatrixProbabilities,
            grammar.numNonTerms());

        // Call the kernel and wait for results
        final int globalWorkSize = edu.ohsu.cslu.util.Math.roundUp(grammar.numNonTerms(), LOCAL_WORK_SIZE);
        binarySpmvKernel.enqueueNDRange(clQueue, new int[] { globalWorkSize }, new int[] { LOCAL_WORK_SIZE });
        clQueue.finish();
    }

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

    private void internalUnarySpmvMultiply(final int chartCellOffset, final short chartCellEnd) {

        // Bind the arguments of the OpenCL kernel
        unarySpmvKernel.setArgs(clChartInsideProbabilities, clChartPackedChildren, clChartMidpoints,
            chartCellOffset, clUnaryRuleMatrixRowIndices, clUnaryRuleMatrixColumnIndices,
            clUnaryRuleMatrixProbabilities, grammar.numNonTerms(), chartCellEnd);

        // Call the kernel and wait for results
        final int globalWorkSize = edu.ohsu.cslu.util.Math.roundUp(grammar.numNonTerms(), LOCAL_WORK_SIZE);
        unarySpmvKernel.enqueueNDRange(clQueue, new int[] { globalWorkSize }, new int[] { LOCAL_WORK_SIZE });
        clQueue.finish();
    }

    private void copyChartToDevice() {
        OpenClUtils.copyToDevice(clQueue, clChartInsideProbabilities, chart.insideProbabilities);
        OpenClUtils.copyToDevice(clQueue, clChartPackedChildren, chart.packedChildren);
        OpenClUtils.copyToDevice(clQueue, clChartMidpoints, chart.midpoints);

    }

    private void copyChartFromDevice() {
        OpenClUtils.copyFromDevice(clQueue, clChartInsideProbabilities, chart.insideProbabilities);
        OpenClUtils.copyFromDevice(clQueue, clChartPackedChildren, chart.packedChildren);
        OpenClUtils.copyFromDevice(clQueue, clChartMidpoints, chart.midpoints);
    }
}

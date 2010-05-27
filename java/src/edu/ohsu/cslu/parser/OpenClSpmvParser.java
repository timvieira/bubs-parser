package edu.ohsu.cslu.parser;

import static com.nativelibs4java.opencl.JavaCL.createBestContext;
import static com.nativelibs4java.util.NIOUtils.directFloats;
import static com.nativelibs4java.util.NIOUtils.directShorts;

import java.io.StringWriter;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

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
import edu.ohsu.cslu.parser.OpenClSpmvParser.OpenClChart.OpenClChartCell;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.chart.DenseVectorChart.DenseVectorChartCell;
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
public class OpenClSpmvParser extends
        SparseMatrixVectorParser<CsrSparseMatrixGrammar, OpenClSpmvParser.OpenClChart> {

    private final static int LOCAL_WORK_SIZE = 64;

    private CLContext context;
    private CLKernel fillFloatKernel;
    private CLKernel binarySpmvKernel;
    private CLKernel unarySpmvKernel;
    private CLKernel cartesianProductKernel;
    private CLKernel cartesianProductUnionKernel;
    private CLQueue clQueue;

    // TODO Make these final once we move kernel compilation into the constructor
    private CLIntBuffer clBinaryRuleMatrixRowIndices;
    private CLIntBuffer clBinaryRuleMatrixColumnIndices;
    private CLFloatBuffer clBinaryRuleMatrixProbabilities;

    private CLIntBuffer clUnaryRuleMatrixRowIndices;
    private CLIntBuffer clUnaryRuleMatrixColumnIndices;
    private CLFloatBuffer clUnaryRuleMatrixProbabilities;

    // Temporary chart cell storage
    private CLIntBuffer clChartCellChildren;
    private CLFloatBuffer clChartCellProbabilities;
    private CLShortBuffer clChartCellMidpoints;

    // Cross-product vector
    private CLFloatBuffer clCrossProductProbabilities0;
    private CLShortBuffer clCrossProductMidpoints0;
    private CLFloatBuffer clCrossProductProbabilities1;
    private CLShortBuffer clCrossProductMidpoints1;

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
        chart = new OpenClChart(sentLength, opts.viterbiMax, this);

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

        // Allocate OpenCL-hosted memory for temporary chart cell storage
        clChartCellChildren = context.createIntBuffer(CLMem.Usage.InputOutput, grammar.numNonTerms());
        clChartCellProbabilities = context.createFloatBuffer(CLMem.Usage.InputOutput, grammar.numNonTerms());
        clChartCellMidpoints = context.createShortBuffer(CLMem.Usage.InputOutput, grammar.numNonTerms());

        // And for cross-product storage
        clCrossProductProbabilities0 = context.createFloatBuffer(CLMem.Usage.InputOutput, grammar
            .cartesianProductFunction().packedArraySize());
        clCrossProductMidpoints0 = context.createShortBuffer(CLMem.Usage.InputOutput, grammar
            .cartesianProductFunction().packedArraySize());
        clCrossProductProbabilities1 = context.createFloatBuffer(CLMem.Usage.InputOutput, grammar
            .cartesianProductFunction().packedArraySize());
        clCrossProductMidpoints1 = context.createShortBuffer(CLMem.Usage.InputOutput, grammar
            .cartesianProductFunction().packedArraySize());
    }

    @Override
    protected void visitCell(final short start, final short end) {

        final OpenClChartCell spvChartCell = (OpenClChartCell) chart.getCell(start, end);

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

            internalBinarySpmvMultiply();

            t2 = System.currentTimeMillis();
            binarySpmvTime = t2 - t1;

        } else {
            t2 = System.currentTimeMillis();
            OpenClUtils.copyToDevice(clQueue, clChartCellChildren, spvChartCell.children);
            OpenClUtils.copyToDevice(clQueue, clChartCellProbabilities, spvChartCell.inside);
            OpenClUtils.copyToDevice(clQueue, clChartCellMidpoints, spvChartCell.midpoints);
        }

        // Handle unary productions
        // TODO: This only goes through unary rules one time, so it can't create unary chains unless such
        // chains are encoded in the grammar. Iterating a few times would probably
        // work, although it's a big-time hack.
        internalUnarySpmvMultiply(end);
        OpenClUtils.copyFromDevice(clQueue, clChartCellChildren, spvChartCell.children);
        OpenClUtils.copyFromDevice(clQueue, clChartCellProbabilities, spvChartCell.inside);
        OpenClUtils.copyFromDevice(clQueue, clChartCellMidpoints, spvChartCell.midpoints);

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

        internalCrossProductUnion(start, end);

        final float[] tmpCrossProductProbabilities = new float[grammar.cartesianProductFunction()
            .packedArraySize()];
        final FloatBuffer mappedClCrossProductProbabilities = directFloats(
            tmpCrossProductProbabilities.length, context.getByteOrder());
        clCrossProductProbabilities0.read(clQueue, mappedClCrossProductProbabilities, true);
        mappedClCrossProductProbabilities.get(tmpCrossProductProbabilities);

        final short[] tmpCrossProductMidpoints = new short[grammar.cartesianProductFunction()
            .packedArraySize()];
        final ShortBuffer mappedClCrossProductMidpoints = directShorts(tmpCrossProductMidpoints.length,
            context.getByteOrder());
        clCrossProductMidpoints0.read(clQueue, mappedClCrossProductMidpoints, true);
        mappedClCrossProductMidpoints.get(tmpCrossProductMidpoints);

        int size = 0;
        for (int i = 0; i < tmpCrossProductProbabilities.length; i++) {
            if (tmpCrossProductProbabilities[i] != Float.NEGATIVE_INFINITY) {
                size++;
            }
        }
        return new CartesianProductVector(grammar, tmpCrossProductProbabilities, tmpCrossProductMidpoints,
            size);
    }

    private void internalCrossProductUnion(final int start, final int end) {

        long t0 = System.currentTimeMillis();

        // Fill the buffer with negative infinity
        fillFloatKernel.setArgs(clCrossProductProbabilities0, grammar.cartesianProductFunction()
            .packedArraySize(), Float.NEGATIVE_INFINITY);
        final int globalWorkSize = edu.ohsu.cslu.util.Math.roundUp(grammar.cartesianProductFunction()
            .packedArraySize(), LOCAL_WORK_SIZE);
        fillFloatKernel.enqueueNDRange(clQueue, new int[] { globalWorkSize }, new int[] { LOCAL_WORK_SIZE });
        clQueue.finish();

        short midpoint = (short) (start + 1);

        while (((OpenClChartCell) chart.getCell(start, midpoint)).validLeftChildren.length == 0
                || ((OpenClChartCell) chart.getCell(midpoint, end)).validRightChildren.length == 0) {
            midpoint++;
        }

        // Compute the cross-product of the first midpoint separately
        internalCrossProduct((OpenClChartCell) chart.getCell(start, midpoint), (OpenClChartCell) chart
            .getCell(midpoint, end), clCrossProductProbabilities0, clCrossProductMidpoints0);

        long t1 = System.currentTimeMillis();
        totalCartesianProductTime += (t1 - t0);

        // Iterate over all other midpoints, unioning together the cross-product of discovered
        // non-terminals in each left/right child pair
        for (; midpoint <= end - 1; midpoint++) {

            final OpenClChartCell leftCell = (OpenClChartCell) chart.getCell(start, midpoint);
            final OpenClChartCell rightCell = (OpenClChartCell) chart.getCell(midpoint, end);

            if (leftCell.validLeftChildren.length > 0 && rightCell.validRightChildren.length > 0) {
                t0 = System.currentTimeMillis();

                fillFloatKernel.setArgs(clCrossProductProbabilities1, grammar.cartesianProductFunction()
                    .packedArraySize(), Float.NEGATIVE_INFINITY);
                fillFloatKernel.enqueueNDRange(clQueue, new int[] { globalWorkSize },
                    new int[] { LOCAL_WORK_SIZE });
                clQueue.finish();

                internalCrossProduct(leftCell, rightCell, clCrossProductProbabilities1,
                    clCrossProductMidpoints1);

                t1 = System.currentTimeMillis();
                totalCartesianProductTime += (t1 - t0);

                // Union the new cross-product with the existing cross-product
                cartesianProductUnionKernel.setArgs(clCrossProductProbabilities0, clCrossProductMidpoints0,
                    clCrossProductProbabilities1, clCrossProductMidpoints1, grammar
                        .cartesianProductFunction().packedArraySize());
                cartesianProductUnionKernel.enqueueNDRange(clQueue, new int[] { globalWorkSize },
                    new int[] { LOCAL_WORK_SIZE });
                clQueue.finish();

                totalCartesianProductUnionTime += (System.currentTimeMillis() - t1);
            }
        }
    }

    private void internalCrossProduct(final OpenClChartCell leftCell, final DenseVectorChartCell rightCell,
            final CLFloatBuffer tmpClCrossProductProbabilities, final CLShortBuffer tmpClCrossProductMidpoints) {

        final CLIntBuffer clValidLeftChildren = OpenClUtils.copyToDevice(clQueue, leftCell.validLeftChildren,
            CLMem.Usage.Input);
        final CLFloatBuffer clValidLeftChildrenProbabilities = OpenClUtils.copyToDevice(clQueue,
            leftCell.validLeftChildrenProbabilities, CLMem.Usage.Input);
        final CLShortBuffer clValidRightChildren = OpenClUtils.copyToDevice(clQueue,
            rightCell.validRightChildren, CLMem.Usage.Input);
        final CLFloatBuffer clValidRightChildrenProbabilities = OpenClUtils.copyToDevice(clQueue,
            rightCell.validRightChildrenProbabilities, CLMem.Usage.Input);

        // Bind the arguments of the OpenCL kernel
        cartesianProductKernel.setArgs(clValidLeftChildren, clValidLeftChildrenProbabilities,
            leftCell.validLeftChildren.length, clValidRightChildren, clValidRightChildrenProbabilities,
            rightCell.validRightChildren.length, tmpClCrossProductProbabilities, tmpClCrossProductMidpoints,
            (short) rightCell.start());

        // Call the kernel and wait for results
        final int globalWorkSize = edu.ohsu.cslu.util.Math.roundUp(leftCell.validLeftChildren.length
                * rightCell.validRightChildren.length, LOCAL_WORK_SIZE);
        cartesianProductKernel.enqueueNDRange(clQueue, new int[] { globalWorkSize },
            new int[] { LOCAL_WORK_SIZE });

        clQueue.finish();
        clValidLeftChildren.release();
        clValidLeftChildrenProbabilities.release();
        clValidRightChildren.release();
        clValidRightChildrenProbabilities.release();
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

        final OpenClChartCell openClCell = (OpenClChartCell) chartCell;

        // Copy cross-product to OpenCL memory
        OpenClUtils.copyToDevice(clQueue, clCrossProductProbabilities0, cartesianProductVector.probabilities);
        OpenClUtils.copyToDevice(clQueue, clCrossProductMidpoints0, cartesianProductVector.midpoints);

        internalBinarySpmvMultiply();

        OpenClUtils.copyFromDevice(clQueue, clChartCellChildren, openClCell.children);
        OpenClUtils.copyFromDevice(clQueue, clChartCellProbabilities, openClCell.inside);
        OpenClUtils.copyFromDevice(clQueue, clChartCellMidpoints, openClCell.midpoints);
    }

    private void internalBinarySpmvMultiply() {

        // Bind the arguments of the OpenCL kernel
        binarySpmvKernel.setArgs(clBinaryRuleMatrixRowIndices, clBinaryRuleMatrixColumnIndices,
            clBinaryRuleMatrixProbabilities, grammar.numNonTerms(), clCrossProductProbabilities0,
            clCrossProductMidpoints0, clChartCellChildren, clChartCellProbabilities, clChartCellMidpoints);

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

        final OpenClChartCell openClCell = (OpenClChartCell) chartCell;

        // Copy current chart cell entries to OpenCL memory
        OpenClUtils.copyToDevice(clQueue, clChartCellChildren, openClCell.children);
        OpenClUtils.copyToDevice(clQueue, clChartCellProbabilities, openClCell.inside);
        OpenClUtils.copyToDevice(clQueue, clChartCellMidpoints, openClCell.midpoints);

        internalUnarySpmvMultiply((short) chartCell.end());

        OpenClUtils.copyFromDevice(clQueue, clChartCellChildren, openClCell.children);
        OpenClUtils.copyFromDevice(clQueue, clChartCellProbabilities, openClCell.inside);
        OpenClUtils.copyFromDevice(clQueue, clChartCellMidpoints, openClCell.midpoints);
    }

    private void internalUnarySpmvMultiply(final short chartCellEnd) {

        // Bind the arguments of the OpenCL kernel
        unarySpmvKernel.setArgs(clUnaryRuleMatrixRowIndices, clUnaryRuleMatrixColumnIndices,
            clUnaryRuleMatrixProbabilities, grammar.numNonTerms(), clChartCellChildren,
            clChartCellProbabilities, clChartCellMidpoints, chartCellEnd);

        // Call the kernel and wait for results
        final int globalWorkSize = edu.ohsu.cslu.util.Math.roundUp(grammar.numNonTerms(), LOCAL_WORK_SIZE);
        unarySpmvKernel.enqueueNDRange(clQueue, new int[] { globalWorkSize }, new int[] { LOCAL_WORK_SIZE });
        clQueue.finish();
    }

    public static class OpenClChart extends edu.ohsu.cslu.parser.chart.DenseVectorChart {

        public OpenClChart(final int size, final boolean viterbiMax, final Parser<?> parser) {
            super(new OpenClChartCell[size][size + 1], viterbiMax, parser);

            for (int start = 0; start < size; start++) {
                for (int end = start + 1; end < size + 1; end++) {
                    chart[start][end] = new OpenClChartCell(start, end);
                }
            }
        }

        public class OpenClChartCell extends DenseVectorChartCell {

            public OpenClChartCell(final int start, final int end) {
                super(start, end);
            }

            @Override
            public void finalizeCell() {
                // Count valid left and right children
                numValidLeftChildren = numValidRightChildren = 0;
                for (int nonterminal = 0; nonterminal < sparseMatrixGrammar.numNonTerms(); nonterminal++) {

                    if (inside[nonterminal] != Float.NEGATIVE_INFINITY) {
                        if (sparseMatrixGrammar.isValidLeftChild(nonterminal)) {
                            numValidLeftChildren++;
                        }
                        if (sparseMatrixGrammar.isValidRightChild(nonterminal)) {
                            numValidRightChildren++;
                        }
                    }
                }

                super.finalizeCell();
            }
        }

    }
}

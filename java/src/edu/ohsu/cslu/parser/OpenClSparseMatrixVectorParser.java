package edu.ohsu.cslu.parser;

import static com.nativelibs4java.opencl.JavaCL.createBestContext;
import static com.nativelibs4java.util.NIOUtils.directFloats;
import static com.nativelibs4java.util.NIOUtils.directInts;
import static com.nativelibs4java.util.NIOUtils.directShorts;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
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
import edu.ohsu.cslu.parser.traversal.ChartTraversal.ChartTraversalType;

/**
 * {@link SparseMatrixVectorParser} which uses a sparse grammar stored in CSR format ({@link CsrSparseMatrixGrammar}) and implements cross-product and SpMV multiplication using
 * OpenCL kernels.
 * 
 * @author Aaron Dunlop
 * @since Feb 11, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class OpenClSparseMatrixVectorParser extends SparseMatrixVectorParser {

    private final CsrSparseMatrixGrammar csrSparseMatrixGrammar;
    private CLContext context;
    private CLKernel binarySpmvKernel;
    private CLQueue clQueue;

    // TODO Make these final once we move kernel compilation into the constructor
    private CLIntBuffer clBinaryRuleMatrixRowIndices;
    private CLIntBuffer clBinaryRuleMatrixColumnIndices;
    private CLFloatBuffer clBinaryRuleMatrixProbabilities;

    public OpenClSparseMatrixVectorParser(final CsrSparseMatrixGrammar grammar, final ChartTraversalType traversalType) {
        super(grammar, traversalType);
        this.csrSparseMatrixGrammar = grammar;

        context = createBestContext();
        clQueue = context.createDefaultQueue();
    }

    @Override
    protected void initParser(final int sentLength) {
        super.initParser(sentLength);

        // TODO Move this to constructor after debugging

        try {
            // Compile OpenCL kernels
            final StringWriter sw = new StringWriter();
            final String filename = getClass().getCanonicalName().replace('.', File.separatorChar) + ".cl";
            final BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream(filename)));
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                sw.write(line);
                sw.write('\n');
            }
            final CLProgram program = context.createProgram(sw.toString()).build();
            binarySpmvKernel = program.createKernel("BinarySpmvMultiply");
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        // Allocate OpenCL-hosted memory for grammar
        clBinaryRuleMatrixRowIndices = context.createIntBuffer(CLMem.Usage.Input, csrSparseMatrixGrammar.binaryRuleMatrixRowIndices().length);
        clBinaryRuleMatrixColumnIndices = context.createIntBuffer(CLMem.Usage.Input, csrSparseMatrixGrammar.binaryRuleMatrixColumnIndices().length);
        clBinaryRuleMatrixProbabilities = context.createFloatBuffer(CLMem.Usage.Input, csrSparseMatrixGrammar.binaryRuleMatrixProbabilities().length);

        // Map and populate input buffers with the grammar
        final IntBuffer mappedBinaryRuleMatrixRowIndices = clBinaryRuleMatrixRowIndices.map(clQueue, CLMem.MapFlags.Write);
        mappedBinaryRuleMatrixRowIndices.put(csrSparseMatrixGrammar.binaryRuleMatrixRowIndices());
        clBinaryRuleMatrixRowIndices.unmap(clQueue, mappedBinaryRuleMatrixRowIndices);

        final IntBuffer mappedBinaryRuleMatrixColumnIndices = clBinaryRuleMatrixColumnIndices.map(clQueue, CLMem.MapFlags.Write);
        mappedBinaryRuleMatrixColumnIndices.put(csrSparseMatrixGrammar.binaryRuleMatrixColumnIndices());
        clBinaryRuleMatrixColumnIndices.unmap(clQueue, mappedBinaryRuleMatrixColumnIndices);

        final FloatBuffer mappedBinaryRuleMatrixProbabilities = clBinaryRuleMatrixProbabilities.map(clQueue, CLMem.MapFlags.Write);
        mappedBinaryRuleMatrixProbabilities.put(csrSparseMatrixGrammar.binaryRuleMatrixProbabilities());
        clBinaryRuleMatrixProbabilities.unmap(clQueue, mappedBinaryRuleMatrixProbabilities);
    }

    @Override
    protected void visitCell(final ChartCell cell) {

        final DenseVectorChartCell spvChartCell = (DenseVectorChartCell) cell;
        final short start = (short) cell.start();
        final short end = (short) cell.end();

        final long t0 = System.currentTimeMillis();
        long t1 = t0;
        long crossProductTime = 0;

        // Skip binary grammar intersection for span-1 cells
        if (end - start > 1) {
            final CrossProductVector crossProductVector = crossProductUnion(start, end);

            t1 = System.currentTimeMillis();
            crossProductTime = t1 - t0;

            // Multiply the unioned vector with the grammar matrix and populate the current cell with the
            // vector resulting from the matrix-vector multiplication
            binarySpmvMultiply(crossProductVector, spvChartCell);
        }
        final long t2 = System.currentTimeMillis();
        final long binarySpmvTime = t2 - t1;

        // Handle unary productions
        // TODO: This only goes through unary rules one time, so it can't create unary chains unless such chains are encoded in the grammar. Iterating a few times would probably
        // work, although it's a big-time hack.
        unarySpmvMultiply(spvChartCell);

        final long t3 = System.currentTimeMillis();
        final long unarySpmvTime = t3 - t2;

        // TODO We won't need to do this once we're storing directly into the packed array
        spvChartCell.finalizeCell();

        // System.out.format("Visited cell: %2d,%2d (%5d ms). Cross-product: %6d/%6d combinations (%5.0f ms, %4.2f/ms), Multiply: %5d edges (%5.0f ms, %4.2f /ms)\n", start, end, t3
        // - t0, crossProductSize, totalProducts, crossProductTime, crossProductSize / crossProductTime, edges, spmvTime, edges / spmvTime);
        totalCrossProductTime += crossProductTime;
        totalSpMVTime += binarySpmvTime + unarySpmvTime;
    }

    @Override
    public void binarySpmvMultiply(final CrossProductVector crossProductVector, final DenseVectorChartCell chartCell) {

        final int[] binaryRuleMatrixRowIndices = csrSparseMatrixGrammar.binaryRuleMatrixRowIndices();
        final int[] binaryRuleMatrixColumnIndices = csrSparseMatrixGrammar.binaryRuleMatrixColumnIndices();
        final float[] binaryRuleMatrixProbabilities = csrSparseMatrixGrammar.binaryRuleMatrixProbabilities();

        final float[] crossProductProbabilities = crossProductVector.probabilities;
        final short[] crossProductMidpoints = crossProductVector.midpoints;

        // ============== Begin OpenCL code ================

        // Copy cross-product to OpenCL memory
        final CLFloatBuffer clCrossProductProbabilities = context.createFloatBuffer(CLMem.Usage.Input, crossProductProbabilities.length);
        final FloatBuffer mappedClCrossProductProbabilities = clCrossProductProbabilities.map(clQueue, CLMem.MapFlags.Write);
        mappedClCrossProductProbabilities.put(crossProductProbabilities);
        clCrossProductProbabilities.unmap(clQueue, mappedClCrossProductProbabilities);

        final CLShortBuffer clCrossProductMidpoints = context.createShortBuffer(CLMem.Usage.Input, crossProductMidpoints.length);
        final ShortBuffer mappedClCrossProductMidpoints = clCrossProductMidpoints.map(clQueue, CLMem.MapFlags.Write);
        mappedClCrossProductMidpoints.put(crossProductMidpoints);
        clCrossProductMidpoints.unmap(clQueue, mappedClCrossProductMidpoints);

        // Allocate OpenCL memory for chart cell vectors
        final CLIntBuffer clChartCellChildren = context.createIntBuffer(CLMem.Usage.Output, csrSparseMatrixGrammar.numNonTerms());
        final CLFloatBuffer clChartCellProbabilities = context.createFloatBuffer(CLMem.Usage.Output, csrSparseMatrixGrammar.numNonTerms());
        final CLShortBuffer clChartCellMidpoints = context.createShortBuffer(CLMem.Usage.Output, csrSparseMatrixGrammar.numNonTerms());

        // Bind these memory objects to the arguments of the kernel
        binarySpmvKernel.setArgs(clBinaryRuleMatrixRowIndices, clBinaryRuleMatrixColumnIndices, clBinaryRuleMatrixProbabilities, grammar.numNonTerms(),
                clCrossProductProbabilities, clCrossProductMidpoints, clChartCellChildren, clChartCellProbabilities, clChartCellMidpoints);

        // Call the kernel and wait for results
        binarySpmvKernel.enqueueNDRange(clQueue, new int[] { grammar.numNonTerms() }, new int[] { 1 });
        clQueue.finish();

        final IntBuffer mappedChartCellChildren = directInts(grammar.numNonTerms(), context.getByteOrder());
        clChartCellChildren.read(clQueue, mappedChartCellChildren, true);
        // final int[] tmpChartCellChildren = new int[grammar.numNonTerms()];
        // mappedChartCellChildren.get(tmpChartCellChildren);
        mappedChartCellChildren.get(chartCell.children);

        final FloatBuffer mappedChartCellProbabilities = directFloats(grammar.numNonTerms(), context.getByteOrder());
        clChartCellProbabilities.read(clQueue, mappedChartCellProbabilities, true);
        // final float[] tmpChartCellProbabilities = new float[grammar.numNonTerms()];
        // mappedChartCellProbabilities.get(tmpChartCellProbabilities);
        mappedChartCellProbabilities.get(chartCell.probabilities);

        final ShortBuffer mappedChartCellMidpoints = directShorts(grammar.numNonTerms(), context.getByteOrder());
        clChartCellMidpoints.read(clQueue, mappedChartCellMidpoints, true);
        // final short[] tmpChartCellMidpoints = new short[grammar.numNonTerms()];
        // mappedChartCellMidpoints.get(tmpChartCellMidpoints);
        mappedChartCellMidpoints.get(chartCell.midpoints);

        // =============== End OpenCL code ================-

        // // Iterate over possible parents (matrix rows)
        // for (int parent = 0; parent < grammar.numNonTerms(); parent++) {
        //
        // // Production winningProduction = null;
        // float winningProbability = Float.NEGATIVE_INFINITY;
        // int winningChildren = Integer.MIN_VALUE;
        // short winningMidpoint = 0;
        //
        // // Iterate over possible children of the parent (columns with non-zero entries)
        // for (int i = binaryRuleMatrixRowIndices[parent]; i < binaryRuleMatrixRowIndices[parent + 1]; i++) {
        // final int grammarChildren = binaryRuleMatrixColumnIndices[i];
        // final float grammarProbability = binaryRuleMatrixProbabilities[i];
        //
        // final float crossProductProbability = crossProductProbabilities[grammarChildren];
        // final float jointProbability = grammarProbability + crossProductProbability;
        //
        // if (jointProbability > winningProbability) {
        // winningProbability = jointProbability;
        // winningChildren = grammarChildren;
        // winningMidpoint = crossProductMidpoints[grammarChildren];
        // }
        // }
        //
        // if (winningProbability != Float.NEGATIVE_INFINITY) {
        // chartCellChildren[parent] = winningChildren;
        // chartCellProbabilities[parent] = winningProbability;
        // chartCellMidpoints[parent] = winningMidpoint;
        // }
        // }
        //
        // assertArrayEquals("Wrong child", chartCellChildren, tmpChartCellChildren);
        // assertArrayEquals("Wrong probability", chartCellProbabilities, tmpChartCellProbabilities, .01f);
        // assertArrayEquals("Wrong midpoint", chartCellMidpoints, tmpChartCellMidpoints);
    }

    @Override
    public void unarySpmvMultiply(final DenseVectorChartCell chartCell) {

        final int[] unaryRuleMatrixRowIndices = csrSparseMatrixGrammar.unaryRuleMatrixRowIndices();
        final int[] unaryRuleMatrixColumnIndices = csrSparseMatrixGrammar.unaryRuleMatrixColumnIndices();
        final float[] unaryRuleMatrixProbabilities = csrSparseMatrixGrammar.unaryRuleMatrixProbabilities();

        final int[] chartCellChildren = chartCell.children;
        final float[] chartCellProbabilities = chartCell.probabilities;
        final short[] chartCellMidpoints = chartCell.midpoints;
        final short chartCellEnd = (short) chartCell.end();

        // Iterate over possible parents (matrix rows)
        for (int parent = 0; parent < grammar.numNonTerms(); parent++) {

            float winningProbability = chartCellProbabilities[parent];
            int winningChildren = Integer.MIN_VALUE;
            short winningMidpoint = 0;

            // Iterate over possible children of the parent (columns with non-zero entries)
            for (int i = unaryRuleMatrixRowIndices[parent]; i < unaryRuleMatrixRowIndices[parent + 1]; i++) {

                final int grammarChildren = unaryRuleMatrixColumnIndices[i];
                final int child = csrSparseMatrixGrammar.unpackLeftChild(grammarChildren);
                final float grammarProbability = unaryRuleMatrixProbabilities[i];

                final float currentProbability = chartCell.probabilities[child];
                final float jointProbability = grammarProbability + currentProbability;

                if (jointProbability > winningProbability) {
                    winningProbability = jointProbability;
                    winningChildren = grammarChildren;
                    winningMidpoint = chartCellEnd;
                }
            }

            if (winningChildren != Integer.MIN_VALUE) {
                chartCellChildren[parent] = winningChildren;
                chartCellProbabilities[parent] = winningProbability;
                chartCellMidpoints[parent] = winningMidpoint;
            }
        }
    }

    // /**
    // * Takes the cross-product of all potential child-cell combinations. Unions those cross-products together, saving the maximum probability child combinations.
    // *
    // * @param start
    // * @param end
    // * @return Unioned cross-product
    // */
    // @Override
    // protected final CrossProductVector crossProductUnion(final int start, final int end) {
    //
    // final float[] tmpCrossProductProbabilities = new float[csrSparseMatrixGrammar.packedArraySize()];
    // Arrays.fill(tmpCrossProductProbabilities, Float.NEGATIVE_INFINITY);
    // final short[] tmpCrossProductMidpoints = new short[csrSparseMatrixGrammar.packedArraySize()];
    //
    // int size = 0;
    //
    // // Iterate over all possible midpoints, unioning together the cross-product of discovered
    // // non-terminals in each left/right child pair
    // for (short midpoint = (short) (start + 1); midpoint <= end - 1; midpoint++) {
    //
    // final float[] midpointProbabilities = crossProductVector((DenseVectorChartCell) chart[start][midpoint], (DenseVectorChartCell) chart[midpoint][end]);
    //
    // for (int child = 0; child < midpointProbabilities.length; child++) {
    // final float currentProbability = tmpCrossProductProbabilities[child];
    // final float newProbability = midpointProbabilities[child];
    //
    // if (newProbability > currentProbability) {
    // tmpCrossProductProbabilities[child] = newProbability;
    // tmpCrossProductMidpoints[child] = midpoint;
    //
    // if (currentProbability == Float.NEGATIVE_INFINITY) {
    // size++;
    // }
    // }
    // }
    // }
    //
    // return new CrossProductVector(csrSparseMatrixGrammar, tmpCrossProductProbabilities, tmpCrossProductMidpoints, size);
    // }
    //
    // private float[] crossProductVector(final DenseVectorChartCell leftCell, final DenseVectorChartCell rightCell) {
    //
    // final float tmpCrossProductProbabilities[] = new float[csrSparseMatrixGrammar.packedArraySize()];
    // Arrays.fill(tmpCrossProductProbabilities, Float.NEGATIVE_INFINITY);
    //
    // final int[] leftChildren = leftCell.validLeftChildren;
    // final float[] leftChildrenProbabilities = leftCell.validLeftChildrenProbabilities;
    // final short[] rightChildren = rightCell.validRightChildren;
    // final float[] rightChildrenProbabilities = rightCell.validRightChildrenProbabilities;
    //
    // for (int i = 0; i < leftChildren.length; i++) {
    //
    // final int leftChild = leftChildren[i];
    // final float leftProbability = leftChildrenProbabilities[i];
    //
    // for (int j = 0; j < rightChildren.length; j++) {
    // tmpCrossProductProbabilities[csrSparseMatrixGrammar.pack(leftChild, rightChildren[j])] = leftProbability + rightChildrenProbabilities[j];
    // }
    // }
    // return tmpCrossProductProbabilities;
    // }
}

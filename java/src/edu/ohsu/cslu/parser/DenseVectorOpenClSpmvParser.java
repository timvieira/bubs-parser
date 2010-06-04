package edu.ohsu.cslu.parser;

import com.nativelibs4java.opencl.CLFloatBuffer;
import com.nativelibs4java.opencl.CLShortBuffer;

import edu.ohsu.cslu.grammar.CsrSparseMatrixGrammar;
import edu.ohsu.cslu.parser.chart.DenseVectorChart;
import edu.ohsu.cslu.parser.chart.ParallelArrayChart.ParallelArrayChartCell;

public class DenseVectorOpenClSpmvParser extends OpenClSpmvParser<DenseVectorChart> {

    public DenseVectorOpenClSpmvParser(final ParserOptions opts, final CsrSparseMatrixGrammar grammar) {
        super(opts, grammar);
    }

    public DenseVectorOpenClSpmvParser(final CsrSparseMatrixGrammar grammar) {
        this(new ParserOptions().setCollectDetailedStatistics(true), grammar);
    }

    @Override
    protected void initParser(final int sentLength) {

        if (chart == null || chart.size() < sentLength) {
            chart = new DenseVectorChart(sentLength, grammar);
        } else {
            chart.clear(sentLength);
        }

        super.initParser(sentLength);
    }

    @Override
    protected void internalCartesianProduct(final ParallelArrayChartCell leftCell,
            final ParallelArrayChartCell rightCell, final CLFloatBuffer tmpClCrossProductProbabilities,
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

    @Override
    protected void internalBinarySpmvMultiply(final int chartCellOffset) {

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

    @Override
    protected void internalUnarySpmvMultiply(final int chartCellOffset, final short chartCellEnd) {

        // Bind the arguments of the OpenCL kernel
        unarySpmvKernel.setArgs(clChartInsideProbabilities, clChartPackedChildren, clChartMidpoints,
            chartCellOffset, clUnaryRuleMatrixRowIndices, clUnaryRuleMatrixColumnIndices,
            clUnaryRuleMatrixProbabilities, grammar.numNonTerms(), chartCellEnd);

        // Call the kernel and wait for results
        final int globalWorkSize = edu.ohsu.cslu.util.Math.roundUp(grammar.numNonTerms(), LOCAL_WORK_SIZE);
        unarySpmvKernel.enqueueNDRange(clQueue, new int[] { globalWorkSize }, new int[] { LOCAL_WORK_SIZE });
        clQueue.finish();
    }
}

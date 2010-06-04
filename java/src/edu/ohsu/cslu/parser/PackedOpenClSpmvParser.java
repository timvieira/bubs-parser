package edu.ohsu.cslu.parser;

import com.nativelibs4java.opencl.CLFloatBuffer;
import com.nativelibs4java.opencl.CLMem;
import com.nativelibs4java.opencl.CLShortBuffer;

import edu.ohsu.cslu.grammar.CsrSparseMatrixGrammar;
import edu.ohsu.cslu.parser.chart.PackedArrayChart;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.PackedArrayChartCell;
import edu.ohsu.cslu.parser.chart.ParallelArrayChart.ParallelArrayChartCell;
import edu.ohsu.cslu.util.OpenClUtils;

public class PackedOpenClSpmvParser extends OpenClSpmvParser<PackedArrayChart> {

    private CLShortBuffer clChartNonTerminalIndices;

    public PackedOpenClSpmvParser(final ParserOptions opts, final CsrSparseMatrixGrammar grammar) {
        super(opts, grammar);
    }

    public PackedOpenClSpmvParser(final CsrSparseMatrixGrammar grammar) {
        this(new ParserOptions(), grammar);
    }

    @Override
    protected void initParser(final int sentLength) {

        if (chart == null || chart.size() < sentLength) {
            allocateOpenClChart();
            chart = new PackedArrayChart(sentLength, grammar);
        } else {
            chart.clear(sentLength);
        }

        super.initParser(sentLength);
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
        final int observedLeftChildren = packedLeftCell.maxLeftChildIndex() - leftChildrenStart;
        final int rightChildrenStart = 0;
        final int observedRightChildren = packedRightCell.maxRightChildIndex();

        // Bind the arguments of the OpenCL kernel
        cartesianProductKernel.setArgs(clChartNonTerminalIndices, clChartInsideProbabilities,
            clChartPackedChildren, clChartMidpoints, leftChildrenStart, observedLeftChildren,
            rightChildrenStart, observedRightChildren, tmpClCartesianProductProbabilities,
            tmpClCartesianProductMidpoints, (short) rightCell.start());

        // Call the kernel and wait for results
        final int globalWorkSize = edu.ohsu.cslu.util.Math.roundUp(
            (observedLeftChildren * observedRightChildren), LOCAL_WORK_SIZE);
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

    @Override
    protected void copyChartToDevice() {
        OpenClUtils.copyToDevice(clQueue, clChartNonTerminalIndices, chart.nonTerminalIndices);
        super.copyChartToDevice();
    }

    @Override
    protected void copyChartFromDevice() {
        OpenClUtils.copyFromDevice(clQueue, clChartNonTerminalIndices, chart.nonTerminalIndices);
        super.copyChartFromDevice();
    }
}

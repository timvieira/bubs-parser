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
import com.nativelibs4java.opencl.CLShortBuffer;

import edu.ohsu.cslu.grammar.CsrSparseMatrixGrammar;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.DenseVectorChart;
import edu.ohsu.cslu.parser.chart.ParallelArrayChart.ParallelArrayChartCell;

/**
 * OpenCL parser which stores the chart in a `dense vector' format {@link DenseVectorChart}.
 * 
 * @author Aaron Dunlop
 * @since Jun 8, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class DenseVectorOpenClSpmvParser extends OpenClSpmvParser<DenseVectorChart> {

    public DenseVectorOpenClSpmvParser(final ParserDriver opts, final CsrSparseMatrixGrammar grammar) {
        super(opts, grammar);
    }

    @Override
    protected void initSentence(final int[] tokens) {
        final int sentLength = tokens.length;
        if (chart == null || chart.size() < sentLength) {
            chart = new DenseVectorChart(tokens, grammar);
        } else {
            chart.clear(sentLength);
        }

        super.initSentence(tokens);
    }

    @Override
    protected void internalCartesianProduct(final ParallelArrayChartCell leftCell,
            final ParallelArrayChartCell rightCell, final CLFloatBuffer tmpClCrossProductProbabilities,
            final CLShortBuffer tmpClCrossProductMidpoints) {

        final int leftChildrenStart = grammar.leftChildrenStart;
        final int validLeftChildren = grammar.leftChildrenEnd - leftChildrenStart + 1;
        final int rightChildrenStart = grammar.rightChildrenStart;
        final int validRightChildren = grammar.rightChildrenEnd - grammar.rightChildrenStart + 1;

        // Bind the arguments of the OpenCL kernel
        cartesianProductKernel
            .setArgs(clChartInsideProbabilities, clChartPackedChildren, clChartMidpoints, leftCell.offset(),
                leftChildrenStart, validLeftChildren, rightCell.offset(), rightChildrenStart,
                validRightChildren, tmpClCrossProductProbabilities, tmpClCrossProductMidpoints,
                rightCell.start());

        // Call the cartesian-product kernel with |V_r| X |V_l| threads (rounded up to the next multiple of
        // LOCAL_WORK_SIZE)
        final int globalWorkSize = edu.ohsu.cslu.util.Math.roundUp((validLeftChildren * validRightChildren),
            LOCAL_WORK_SIZE);
        cartesianProductKernel.enqueueNDRange(clQueue, new int[] { globalWorkSize },
            new int[] { LOCAL_WORK_SIZE });

        clQueue.finish();
    }

    @Override
    protected void internalBinarySpmvMultiply(final ParallelArrayChartCell chartCell) {

        // Bind the arguments of the OpenCL kernel
        binarySpmvKernel.setArgs(clChartInsideProbabilities, clChartPackedChildren, clChartMidpoints,
            chartCell.offset(), clCartesianProductProbabilities0, clCartesianProductMidpoints0,
            clBinaryRuleMatrixRowIndices, clBinaryRuleMatrixColumnIndices, clBinaryRuleMatrixProbabilities,
            grammar.numNonTerms());

        // Call the binary SpMV kernel with |V| threads (rounded up to the nearest multiple of
        // LOCAL_WORK_SIZE)
        final int globalWorkSize = edu.ohsu.cslu.util.Math.roundUp(grammar.numNonTerms(), LOCAL_WORK_SIZE);
        binarySpmvKernel.enqueueNDRange(clQueue, new int[] { globalWorkSize }, new int[] { LOCAL_WORK_SIZE });
        clQueue.finish();
    }

    @Override
    protected void internalUnarySpmvMultiply(final ParallelArrayChartCell chartCell) {

        // Bind the arguments of the OpenCL kernel
        unarySpmvKernel.setArgs(clChartInsideProbabilities, clChartPackedChildren, clChartMidpoints,
            chartCell.offset(), clCsrUnaryRowStartIndices, clCsrUnaryColumnIndices, clCsrUnaryProbabilities,
            grammar.numNonTerms(), chartCell.end());

        // Call the unary SpMV kernel with |V| threads (rounded up to the nearest multiple of LOCAL_WORK_SIZE)
        final int globalWorkSize = edu.ohsu.cslu.util.Math.roundUp(grammar.numNonTerms(), LOCAL_WORK_SIZE);
        unarySpmvKernel.enqueueNDRange(clQueue, new int[] { globalWorkSize }, new int[] { LOCAL_WORK_SIZE });
        clQueue.finish();
    }
}

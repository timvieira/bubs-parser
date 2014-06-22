/*
 * Copyright 2010-2014, Oregon Health & Science University
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
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Further documentation and contact information is available at
 *   https://code.google.com/p/bubs-parser/ 
 */

package edu.ohsu.cslu.lela;

import java.io.File;

import cltool4j.BaseCommandlineTool;
import cltool4j.GlobalConfigProperties;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.parser.ParserDriver;

/**
 * @author Aaron Dunlop
 * @since Nov 5, 2012
 */
public class ScoreLikelihood extends BaseCommandlineTool {

    @Option(name = "-g", required = true, metaVar = "file", usage = "Grammar file")
    private File grammarFile;

    @Override
    protected void run() throws Exception {

        final ParserDriver opts = new ParserDriver();
        opts.cellSelectorModel = ConstrainedCellSelector.MODEL;
        GlobalConfigProperties.singleton().setProperty(ParserDriver.OPT_NT_COMPARATOR_CLASS, "LexicographicComparator");

        final ConstrainedCscSparseMatrixGrammar grammar = new ConstrainedCscSparseMatrixGrammar(
                fileAsBufferedReader(grammarFile));
        final SparseMatrixGrammar baseGrammar = (SparseMatrixGrammar) grammar.toUnsplitGrammar();

        final ConstrainedSplitInsideOutsideParser parser = new ConstrainedSplitInsideOutsideParser(opts, grammar);

        // Iterate over the training corpus, parsing and accumulating likelihood
        double corpusLikelihood = 0f;
        for (final String line : inputLines()) {
            final ConstrainingChart constrainingChart = new ConstrainingChart(NaryTree.read(line, String.class)
                    .binarize(grammar.grammarFormat, grammar.binarization()), baseGrammar);

            parser.findBestParse(constrainingChart);
            corpusLikelihood += parser.chart.getInside(0, parser.chart.size(), 0);
        }

        System.out.format("Likelihood: %.2f\n", corpusLikelihood);
    }

    public static void main(final String[] args) {
        run(args);
    }

}

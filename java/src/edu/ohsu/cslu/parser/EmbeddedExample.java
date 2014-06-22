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
package edu.ohsu.cslu.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import edu.ohsu.cslu.grammar.DecisionTreeTokenClassifier;
import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashPackingFunction;
import edu.ohsu.cslu.parser.cellselector.CompleteClosureModel;
import edu.ohsu.cslu.parser.fom.BoundaryLex;
import edu.ohsu.cslu.parser.fom.FigureOfMeritModel.FOMType;
import edu.ohsu.cslu.parser.spmv.CscSpmvParser;

/**
 * Demonstrates a very simple method of embedding BUBS functionality into user code, including reading in a grammar and
 * pruning model and parsing simple example sentences.
 * 
 * Usage: EmbeddedExample <grammar file> <edge selector model> <cell selector model>
 * 
 * e.g. EmbeddedExample models/wsj_l0mm_16_.55_6.gr.gz models/wsj_l0mm_16_.55_6.lexfom.gz models/wsj_cc.mdl.99
 */
public class EmbeddedExample {

    public static void main(final String[] args) throws FileNotFoundException, IOException, ClassNotFoundException {

        // The 'ParserDriver' class also serves as the container for parser options
        final ParserDriver opts = new ParserDriver();

        // Instantiate a Grammar class and load in the grammar from disk
        final LeftCscSparseMatrixGrammar grammar = new LeftCscSparseMatrixGrammar(uncompressFile(args[0]),
                new DecisionTreeTokenClassifier(), PerfectIntPairHashPackingFunction.class);
        opts.setGrammar(grammar);

        // Create FOMModel and CellSelectorModel instances and load models from disk
        opts.fomModel = new BoundaryLex(FOMType.BoundaryLex, grammar, uncompressFile(args[1]));
        opts.cellSelectorModel = new CompleteClosureModel(new File(args[2]), null);

        // Create a Parser instance
        final CscSpmvParser parser = new CscSpmvParser(opts, grammar);

        // Parse example sentences and write output to STDOUT
        ParseTask result = parser
                .parseSentence("The economy 's temperature will be taken from several vantage points this week , with readings on trade , output , housing and inflation .");
        System.out.println(result.parseBracketString(false));

        result = parser
                .parseSentence("The most troublesome report may be the August merchandise trade deficit due out tomorrow .");
        System.out.println(result.parseBracketString(false));
    }

    // Open and uncompress a gzipped file, returning a BufferedReader
    private static BufferedReader uncompressFile(final String filename) throws FileNotFoundException, IOException {
        return new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(filename))));
    }
}

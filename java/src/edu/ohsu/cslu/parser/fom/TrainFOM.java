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
package edu.ohsu.cslu.parser.fom;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import cltool4j.BaseCommandlineTool;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.parser.fom.FigureOfMeritModel.FOMType;

public class TrainFOM extends BaseCommandlineTool {

    @Option(name = "-fom", required = true, usage = "FOM to train.  Supports BoundaryPOS, BoundaryLex, Prior, Discriminative, Ngram")
    private FOMType fomType = null;

    @Option(name = "-g", required = true, metaVar = "grammar", usage = "Grammar file (text, gzipped text, or binary serialized")
    private String grammarFile = null;

    @Option(name = "-counts", usage = "Write model counts instead of log probabilities")
    private boolean writeCounts = false;

    @Option(name = "-smooth", metaVar = "N", usage = "Apply add-N smoothing to model")
    private float smoothingCount = 0.5f;

    @Option(name = "-prune", metaVar = "N", usage = "Prune entries with count less than N")
    private int pruneCount = 0;

    // TODO Convert to 'File' and read with cltool4j functions
    @Option(name = "-lexCounts", metaVar = "FILE", usage = "Lines <word> <count> for all lexical entries")
    private String lexCountFile = null;

    // TODO Convert to 'File' and read with cltool4j functions
    @Option(name = "-lexMap", metaVar = "FILE", usage = "Lines <word> <class> for all lexical entries")
    private String lexMapFile = null;

    @Option(name = "-unkThresh", metaVar = "N", usage = "Convert lexical items to UNK with frequency <= N")
    private int unkThresh = 5;

    @Option(name = "-posNgramOrder", metaVar = "N", usage = "POS n-gram order for feature extraction (only Boundary)")
    private int posNgramOrder = 2;

    @Option(name = "-feats", usage = "Feature template file OR feature template string: lt rt lt_lt-1 rw_rt loc ...")
    private String featTemplate = null;

    @Option(name = "-e", aliases = { "--extractFeats" })
    private boolean extractFeatures = false;

    @Option(name = "-i", aliases = { "--iterations" }, metaVar = "count", usage = "Iterations over training corpus")
    private int iterations = 10;

    @Option(name = "-l", aliases = { "--learningRate" }, usage = "Learning rate for Logistic Regression model")
    private float learningRate = 1.0f;

    @Option(name = "-input", usage = "Input treebank file")
    private String inputFileName;

    public static void main(final String[] args) {
        run(args);
    }

    @Override
    public void run() throws Exception {

        final BufferedWriter outputStream = new BufferedWriter(new OutputStreamWriter(System.out));
        final BufferedReader inputStream = new BufferedReader(new InputStreamReader(System.in));

        switch (fomType) {

        case BoundaryPOS:
            BoundaryInOut.train(inputStream, outputStream, grammarFile, smoothingCount, writeCounts, posNgramOrder);
            break;

        case BoundaryLex:
            final BoundaryLex boundaryModel = new BoundaryLex(FOMType.BoundaryLex);
            boundaryModel.train(inputStream, outputStream, grammarFile, smoothingCount, writeCounts, pruneCount,
                    lexCountFile, unkThresh, lexMapFile);
            break;

        case Discriminative:
            DiscriminativeFOM.train(inputFileName, outputStream, grammarFile, featTemplate, iterations, learningRate);
            if (extractFeatures) {
                // DiscriminativeFOMLR.extractFeatures(inputStream, outputStream, grammarFile, featTemplate);
            } else {
                // DiscriminativeFOMLR.train(inputStream, outputStream, grammarFile, featTemplate, iterations,
                // learningRate);
            }
            break;

        case Prior:
            PriorFOM.train(inputStream, outputStream, grammarFile, smoothingCount, writeCounts);
            break;

        case Ngram:
            final NGramOutside nGramModel = new NGramOutside();
            nGramModel.train(inputStream, outputStream, grammarFile, smoothingCount, writeCounts, pruneCount,
                    lexCountFile, unkThresh);
            break;

        default:
            throw new IllegalArgumentException("FOM type '" + fomType + "' not supported.");

        }
    }
}

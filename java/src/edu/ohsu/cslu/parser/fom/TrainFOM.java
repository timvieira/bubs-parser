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
import edu.ohsu.cslu.parser.fom.FigureOfMerit.FOMType;

public class TrainFOM extends BaseCommandlineTool {

    @Option(name = "-fom", required = true, usage = "FOM to train.  Supports BoundaryPOS, BoundaryLex, Prior, Discriminative")
    private FOMType fomType = null;

    @Option(name = "-g", required = true, metaVar = "grammar", usage = "Grammar file (text, gzipped text, or binary serialized")
    private String grammarFile = null;

    @Option(name = "-counts", usage = "Write model counts instead of log probabilities")
    private boolean writeCounts = false;

    @Option(name = "-smooth", metaVar = "N", usage = "Apply add-N smoothing to model")
    private float smoothingCount = (float) 0.0;

    @Option(name = "-prune", metaVar = "N", usage = "Prune entries with count less than N")
    private int pruneCount = 0;

    @Option(name = "-lexCounts", metaVar = "FILE", usage = "Lines <word> <count> for all lexical entries")
    private String lexCountFile = null;

    @Option(name = "-lexMap", metaVar = "FILE", usage = "Lines <word> <class> for all lexical entries")
    private String lexMapFile = null;

    @Option(name = "-unkThresh", metaVar = "N", usage = "Convert lexical items to UNK with frequency <= N")
    private int unkThresh = 0;

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

    public BufferedWriter outputStream = new BufferedWriter(new OutputStreamWriter(System.out));
    public BufferedReader inputStream = new BufferedReader(new InputStreamReader(System.in));

    public static void main(final String[] args) {
        run(args);
    }

    @Override
    public void run() throws Exception {
        if (fomType == FOMType.BoundaryPOS) {
            BoundaryInOut.train(inputStream, outputStream, grammarFile, smoothingCount, writeCounts, posNgramOrder);
        } else if (fomType == FOMType.BoundaryLex) {
            final BoundaryLex model = new BoundaryLex(FOMType.BoundaryLex);
            model.train(inputStream, outputStream, grammarFile, smoothingCount, writeCounts, posNgramOrder, pruneCount,
                    lexCountFile, unkThresh, lexMapFile);
        } else if (fomType == FOMType.Discriminative) {
            if (extractFeatures) {
                DiscriminativeFOM.extractFeatures(inputStream, outputStream, grammarFile, featTemplate);
            } else {
                DiscriminativeFOM.train(inputStream, outputStream, grammarFile, featTemplate, iterations, learningRate);
            }
        } else if (fomType == FOMType.Prior) {
            PriorFOM.train(inputStream, outputStream, grammarFile, smoothingCount, writeCounts);
        } else {
            throw new IllegalArgumentException("FOM type '" + fomType + "' not supported.");
        }
    }

}

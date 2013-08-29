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
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import cltool4j.BaseCommandlineTool;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.parser.fom.FigureOfMeritModel.FOMType;

/**
 * Trains a figure-of-merit model
 * 
 * @author Nathan Bodenstab
 */
public class TrainFOM extends BaseCommandlineTool {

    @Option(name = "-fom", required = true, usage = "FOM to train.  Supports BoundaryPOS, BoundaryLex, Prior, Discriminative, Ngram")
    private FOMType fomType = null;

    @Option(name = "-g", required = true, metaVar = "grammar", usage = "Grammar file (text, gzipped text, or binary serialized")
    private String grammarFile = null;

    // TODO Remove?
    @Option(name = "-counts", usage = "Write model counts instead of log probabilities")
    private boolean writeCounts = false;

    @Option(name = "-smooth", metaVar = "N", usage = "Apply add-N smoothing to model")
    private float smoothingCount = 0.5f;

    @Option(name = "-clusters", metaVar = "FILE", usage = "Lines <word> <class> for all lexical entries (Lexical FOM only)")
    private File clusterFile = null;

    @Option(name = "-unkThresh", metaVar = "N", usage = "Convert lexical items to UNK with frequency <= N (Lexical FOM only)")
    private int unkThresh = 5;

    @Option(name = "-posNgramOrder", metaVar = "N", usage = "POS n-gram order for feature extraction (POS FOM only)")
    private int posNgramOrder = 2;

    public static void main(final String[] args) {
        run(args);
    }

    @Override
    public void run() throws Exception {

        final BufferedWriter outputStream = new BufferedWriter(new OutputStreamWriter(System.out));
        final BufferedReader inputStream = new BufferedReader(new InputStreamReader(System.in));

        switch (fomType) {

        case BoundaryPOS:
            BoundaryPosModel.train(grammarFile, inputStream, outputStream, smoothingCount, writeCounts, posNgramOrder);
            break;

        case BoundaryLex:
            final BoundaryLex boundaryModel = new BoundaryLex();
            boundaryModel.train(inputStream, outputStream, grammarFile, smoothingCount, writeCounts, unkThresh,
                    clusterFile);
            break;

        default:
            throw new IllegalArgumentException("FOM type '" + fomType + "' not supported.");

        }
    }
}

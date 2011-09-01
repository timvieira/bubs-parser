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

    @Option(name = "-fom", required = true, usage = "FOM to train.  Supports BoundaryInOut,Discriminative")
    private FOMType fomType = null;

    @Option(name = "-g", required = true, metaVar = "grammar", usage = "Grammar file (text, gzipped text, or binary serialized")
    private String grammarFile = null;

    @Option(name = "-counts", usage = "Write model counts instead of log probabilities (only BoundaryInOut)")
    public boolean writeCounts = false;

    @Option(name = "-smooth", metaVar = "N", usage = "Apply add-N smoothing to model (only BoundaryInOut)")
    public float smoothingCount = (float) 0.5;

    @Option(name = "-posNgramOrder", metaVar = "N", usage = "POS n-gram order for feature extraction")
    public int posNgramOrder = 2;

    @Option(name = "-feats", usage = "Feature template file OR feature template string: lt rt lt_lt-1 rw_rt loc ...")
    public String featTemplate = null;

    @Option(name = "-extractFeats")
    public boolean extractFeatures = false;

    public BufferedWriter outputStream = new BufferedWriter(new OutputStreamWriter(System.out));
    public BufferedReader inputStream = new BufferedReader(new InputStreamReader(System.in));

    public static void main(final String[] args) {
        run(args);
    }

    @Override
    public void run() throws Exception {
        if (fomType == FOMType.BoundaryInOut) {
            BoundaryInOut.train(inputStream, outputStream, grammarFile, smoothingCount, writeCounts, posNgramOrder);
        } else if (fomType == FOMType.Discriminative) {
            DiscriminativeFOM.train(inputStream, outputStream, grammarFile, featTemplate, extractFeatures);
        } else {
            throw new IllegalArgumentException("FOM type '" + fomType + "' not supported.");
        }
    }

}

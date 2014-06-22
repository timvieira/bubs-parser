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

package edu.ohsu.cslu.perceptron;

import java.io.BufferedReader;
import java.io.File;
import java.util.Arrays;

import cltool4j.BaseCommandlineTool;
import cltool4j.args4j.Option;

/**
 * @author Aaron Dunlop
 * @since Jan 29, 2013
 */
public class ProfileTagger extends BaseCommandlineTool {

    @Option(name = "-m", metaVar = "file", usage = "Model file (Java Serialized Object)")
    private File modelFile;

    @Option(name = "-i", metaVar = "iterations", usage = "Iterations")
    private int iterations;

    @Override
    protected void run() throws Exception {

        final Tagger t = new Tagger();
        t.readModel(fileAsInputStream(modelFile));

        final BufferedReader br = inputAsBufferedReader();
        br.mark(20 * 1024 * 1024);
        int sentences = 0, words = 0, correct = 0;
        final MulticlassTaggerFeatureExtractor fe = new MulticlassTaggerFeatureExtractor(t.featureTemplates, t.lexicon,
                t.decisionTreeUnkClassSet, null, t.tagSet());

        final long t0 = System.currentTimeMillis();

        for (int i = 0; i < iterations; i++) {
            for (final String line : inputLines(br)) {
                sentences++;
                final MulticlassTagSequence tagSequence = new MulticlassTagSequence(line, t.lexicon,
                        t.decisionTreeUnkClassSet, null, null, null, t.tagSet());

                for (int j = 0; j < tagSequence.length; j++) {
                    tagSequence.predictedClasses[j] = t.classify(fe.featureVector(tagSequence, j));
                    if (tagSequence.predictedClasses[j] == tagSequence.goldClasses[j]) {
                        correct++;
                    }
                    words++;
                }
                Arrays.fill(tagSequence.predictedClasses, (short) 0);
            }
            br.reset();
        }

        final long ms = System.currentTimeMillis() - t0;
        System.out.format("Sentences: %d  Words: %d  Time(ms): %d  w/s: %.2f  Accuracy: %.2f\n", sentences, words, ms,
                words * 1000.0 / ms, correct * 100.0 / words);
    }

    public static void main(final String[] args) {
        run(args);
    }

}

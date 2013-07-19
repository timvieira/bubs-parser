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
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.nio.charset.Charset;

import cltool4j.GlobalConfigProperties;
import edu.ohsu.cslu.grammar.DecisionTreeTokenClassifier;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.Parser.ResearchParserType;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.Chart;
import edu.ohsu.cslu.parser.spmv.SparseMatrixVectorParser.PackingFunctionType;

/**
 * Represents a local prioritization model (for historical reasons, also known as a 'figure of merit'). A prioritization
 * model ranks constituents within a chart cell. A simple implementation ({@link InsideProb}) ranks by inside
 * probability. Most effective models also incorporate some heuristic estimate of the outside probability as well, thus
 * reranking candidate labels and (hopefully) allowing effective beam-search with a narrower beam.
 * 
 * This model creates instances using that model (see {@link FigureOfMerit}). Implementations may constrain parse forest
 * population (for agenda parsers) or cell population (for chart parsers) by lexical analysis of the sentence or other
 * outside information.
 * 
 * Implementations of this model class should be thread-safe; i.e., after reading in or initializing the model, it must
 * be safe to call {@link #createFOM()} simultaneously from multiple threads. Note that the {@link FigureOfMerit}
 * instances returned are not expected to be thread-safe. To parse multiple sentences simultaneously, the user should
 * obtain a {@link FigureOfMerit} instance for each thread, using {@link #createFOM()}.
 */
public abstract class FigureOfMeritModel {

    protected FOMType type;
    private final static float normInsideTune = GlobalConfigProperties.singleton()
            .getFloatProperty("normInsideTune", 0);
    private final static boolean geometricInsideNorm = GlobalConfigProperties.singleton().getBooleanProperty(
            "geometricInsideNorm", false);

    // float ngramOutsideTune = GlobalConfigProperties.singleton().getFloatProperty("ngramOutsideTune");

    static public enum FOMType {
        Inside, BoundaryPOS, BoundaryLex, Discriminative;
    }

    public FigureOfMeritModel(final FOMType type) {
        this.type = type;
    }

    /**
     * @return a new {@link FigureOfMerit} instance based on this model.
     */
    public abstract FigureOfMerit createFOM();

    /**
     * Reads in a grammar from disk.
     */
    protected static Grammar readGrammar(final String grammarFile, final ResearchParserType researchParserType,
            final PackingFunctionType packingFunctionType) throws IOException {
        // Handle gzipped and non-gzipped grammar files
        return ParserDriver.createGrammar(ParserDriver.fileAsBufferedReader(grammarFile, Charset.forName("UTF-8")),
                researchParserType, new DecisionTreeTokenClassifier(), packingFunctionType);
    }

    public abstract class FigureOfMerit implements Serializable {

        private static final long serialVersionUID = 1L;

        public FigureOfMerit() {
        }

        public float calcFOM(final int start, final int end, final short parent, final float insideProbability) {
            throw new UnsupportedOperationException("Not implemented in " + getClass().getName());
        }

        public float calcLexicalFOM(final int start, final int end, final short parent, final float insideProbability) {
            throw new UnsupportedOperationException("Not implemented in " + getClass().getName());
        }

        public void initSentence(final ParseTask parseTask, final Chart chart) {
        }

        protected float normInside(final int start, final int end, final float insideProb) {
            if (geometricInsideNorm) {
                // Geometric mean normalization (used by C&C)
                // non-log: sqrt(span, insideProb)
                return insideProb / (end - start);
            }

            // Gives larger spread of constituent scores over all span lengths (compared to geometric mean)
            // non-log: insideProb * e^(span*X)
            return normInsideTune != 0 ? insideProb + (end - start) * normInsideTune : insideProb;
        }

        /**
         * @throws IOException if input from the {@link Reader} fails
         */
        public void train(final BufferedReader reader) throws IOException {
            throw new UnsupportedOperationException("Not implemented.");
        }

        /**
         * @throws IOException if input from the {@link Reader} fails
         */
        public void readModel(final BufferedReader reader) throws IOException {
            // NOTE: some models have nothing to be read
            // throw new Exception("Not implemented.");
        }

        /**
         * @throws IOException if input from the {@link Reader} fails
         */
        public void writeModel(final BufferedWriter reader) throws IOException {
            throw new UnsupportedOperationException("Not implemented.");
        }

    }
}

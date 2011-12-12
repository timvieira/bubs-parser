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

import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.Parser;
import edu.ohsu.cslu.parser.chart.Chart;

/**
 * A figure-of-merit model for a particular parser. {@link FigureOfMerit} instances are not required (or expected) to be
 * thread-safe across multiple simultaneous sentences, so an instance should not be shared by multiple {@link Parser}
 * instances. Model parameters should be stored in a shared {@link FigureOfMeritModel}.
 */
public abstract class FigureOfMerit implements Serializable {

    private static final long serialVersionUID = 1L;

    static public enum FOMType {
        Inside, NormalizedInside, Boundary, Prior, InsideWithFwdBkwd, WeightedFeatures, Discriminative
    }

    // public abstract float calcFOM(ChartEdge edge);

    public float calcFOM(final int start, final int end, final short parent, final float insideProbability) {
        throw new UnsupportedOperationException("Not implemented in " + getClass().getName());
    }

    public float calcLexicalFOM(final int start, final int end, final short parent, final float insideProbability) {
        throw new UnsupportedOperationException("Not implemented in " + getClass().getName());
    }

    public void initSentence(final ParseTask parseTask, final Chart chart) {
        // default is to do nothing
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

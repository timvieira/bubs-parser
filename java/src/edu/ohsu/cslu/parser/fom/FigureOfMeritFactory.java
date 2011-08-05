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

import cltool4j.BaseLogger;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.parser.fom.FigureOfMerit.FOMType;

/**
 * Represents a model for edge selection and creates edge selector instances using that model (see {@link FigureOfMerit}
 * ). Implementations may constrain parse forest population (for agenda parsers) or cell population (for chart parsers)
 * by lexical analysis of the sentence or other outside information.
 */
public class FigureOfMeritFactory {

    protected FOMType type;

    public FigureOfMeritFactory(final FOMType type) {
        this.type = type;
    }

    /**
     * @return a new {@link FigureOfMerit} instance based on this model.
     */
    public FigureOfMerit createEdgeSelector(final Grammar grammar) {
        switch (type) {
        case Inside:
            return new InsideProb();
        case NormalizedInside:
            return new NormalizedInsideProb();
        case WeightedFeatures:
            return new WeightedFeatures(grammar);
        default:
            BaseLogger.singleton().info("ERROR: EdgeSelectorType " + type + " not supported.");
            System.exit(1);
            return null;
        }
    }
}

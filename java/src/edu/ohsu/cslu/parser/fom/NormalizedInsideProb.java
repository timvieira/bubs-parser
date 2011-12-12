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

import cltool4j.GlobalConfigProperties;

/**
 * Normalizes inside grammar probability by span length.
 * 
 * @author Nathan Bodenstab
 */
public class NormalizedInsideProb extends InsideProb {

    private static final long serialVersionUID = 1L;

    // @Override
    // public float calcFOM(final ChartEdge edge) {
    // final int spanLength = edge.end() - edge.start();
    // // return edge.inside() + spanLength * ParserDriver.param1;
    // return edge.inside() + spanLength;
    // }

    // float normInsideTune = (float) Math.log(GlobalConfigProperties.singleton().getFloatProperty("normInsideTune"));
    float normInsideTune = GlobalConfigProperties.singleton().getFloatProperty("normInsideTune");

    // float logAlpha = (float) Math.log(normInsideTune);
    // float logOneMinusAlpha = (float) Math.log(1 - normInsideTune);

    @Override
    public float calcFOM(final int start, final int end, final short parent, final float insideProbability) {
        // log [ nth root of insideProb + normInsideTune * insideProb ]
        // log(nth root of x)) = log(x)/n
        // return (float) Util.logSum(logAlpha + insideProbability / (end - start + 1), logOneMinusAlpha
        // + insideProbability);
        // return insideProbability / (end - start + 1);

        // From first 10 sents, range btwn 0.7 - 0.9 gives a curve, but acc drops fast.
        return (float) (insideProbability / Math.pow((end - start + 1), normInsideTune));

        // System.out.println((end - start + 1) + "\t" + insideProbability);

        // From first 10 sets, range btwn 4 and 6
        // return insideProbability + normInsideTune * (end - start + 1);
    }

    @Override
    public float calcLexicalFOM(final int start, final int end, final short parent, final float insideProbability) {
        return insideProbability;
    }

}

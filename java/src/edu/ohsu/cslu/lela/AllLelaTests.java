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
package edu.ohsu.cslu.lela;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ TestStringCountGrammar.class, TestFractionalCountGrammar.class, TestProductionListGrammar.class,
        TestConstrainingChart.class, TestConstrainedChart.class, TestConstrainedInsideOutsideParser.class,
        TestTrainGrammar.class })
public class AllLelaTests {

    /**
     * <pre>
     *            top
     *             |
     *             a 
     *             |
     *       --------------
     *       |           |
     *       a           b
     *       |           |
     *    -------     --------
     *    |     |     |      |
     *    a     b     b      a
     *    |     |     |      |
     *  -----   d     b      d
     *  |   |         |
     *  a   a         d
     *  |   |
     *  c   c
     *  
     *  top -> a 1
     *  a -> a b 2/6
     *  a -> a a 1/6
     *  a -> c 2/6
     *  a -> d 1/6
     *  b -> b a 1/4
     *  b -> b 1/4
     *  b -> d 2/4
     * </pre>
     */
    public final static String STRING_SAMPLE_TREE = "(top (a (a (a (a c) (a c)) (b d)) (b (b (b d)) (a d))))";

    public final static String TREE_WITH_LONG_UNARY_CHAIN = "(TOP (S (NP (NP (RB Not) (PDT all) (DT those)) (SBAR (WHNP (WP who)) (S (VP (VBD wrote))))) (VP (VBP oppose) (NP (DT the) (NNS changes))) (. .)))";

}

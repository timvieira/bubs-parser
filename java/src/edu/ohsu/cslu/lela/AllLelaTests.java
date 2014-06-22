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
package edu.ohsu.cslu.lela;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ TestStringCountGrammar.class, TestFractionalCountGrammar.class, TestConstrainingChart.class,
        TestConstrainedChart.class, TestConstrainedSplitInsideOutsideParser.class, TestTrainGrammar.class })
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
     *    a     d     b      c
     *    |     |     |      |
     *  -----   f     d      f
     *  |   |         |
     *  c   c         f
     *  |   |
     *  e   e
     *  
     *  top -> a 1
     *  a -> a b 1/3
     *  a -> a d 1/3
     *  a -> c c 1/3
     *  c -> e 2/3
     *  c -> f 1/3
     *  b -> b c 1/2
     *  b -> d 1/2
     *  d -> f 1
     * </pre>
     */
    public final static String STRING_SAMPLE_TREE = "(top (a (a (a (c e) (c e)) (d f)) (b (b (d f)) (c f))))";

    public final static String TREE_WITH_LONG_UNARY_CHAIN = "(TOP (S (NP (NP (RB Not) (PDT all) (DT those)) (SBAR (WHNP (WP who)) (S (VP (VBD wrote))))) (VP (VBP oppose) (NP (DT the) (NNS changes))) (. .)))";

}

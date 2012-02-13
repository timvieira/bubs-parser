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

package edu.ohsu.cslu.parser.ml;

import static org.junit.Assert.assertEquals;

import java.io.FileWriter;

import org.cjunit.FilteredRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import cltool4j.GlobalConfigProperties;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.Parser.InputFormat;
import edu.ohsu.cslu.parser.cellselector.LeftRightBottomTopTraversal;
import edu.ohsu.cslu.parser.ecp.ChartParserTestCase;

/**
 * @author Aaron Dunlop
 * @since Feb 9, 2012
 */
@RunWith(FilteredRunner.class)
public class TestConstrainedCphSpmlParser extends ChartParserTestCase<ConstrainedCphSpmlParser> {

    @Override
    @Before
    public void setUp() throws Exception {
        GlobalConfigProperties.singleton().setProperty("normInsideTune", "0");
        super.setUp();
    }

    @Test
    public void testSimpleGrammar2() throws Exception {
        parser = createParser(simpleGrammar2, LeftRightBottomTopTraversal.MODEL, parserOptions(), configProperties());

        final String constrainingTree = "(ROOT (S (NP (DT The) (NP (NN fish) (NN market))) (VP (VB stands) (RB last))))";
        final ParseTask task = new ParseTask(constrainingTree, InputFormat.Tree, simpleGrammar2, null);
        assertEquals("(ROOT (S (NP (DT The) (NP (NN fish) (NN market))) (VP (VB stands) (RB last))))", parser
                .findBestParse(task).toString());
    }

    @Test
    public void testF2_21() throws Exception {
        final FileWriter tmpFile = new FileWriter("/tmp/base.gr");
        tmpFile.write(((ConstrainedCphSpmlParser) parser).baseGrammar.toString());
        tmpFile.close();

        final String constrainingTree = "(ROOT (S (S (NP (NP (NP (DT The) (NN economy) (POS 's))) (NN temperature)) (VP (MD will) (VP (VB be) (VP (VP (VP (VP (VBN taken) (PP (IN from) (NP (NP (JJ several) (NN vantage)) (NNS points)))) (NP (DT this) (NN week))) (, ,)) (PP (IN with) (NP (NP (NNS readings)) (PP (IN on) (NP (NP (NN trade) (, ,) (NN output) (, ,) (NN housing) (, ,) (CC and) (NN inflation)))))))))) (. .)))";

        final ParseTask task = new ParseTask(constrainingTree, InputFormat.Tree, f2_21_grammar, null);
        assertEquals(
                "(ROOT (S (S|<NP-VP> (NP (NP (NP|<DT-NN> (DT The) (NN economy) (POS 's))) (NN temperature)) (VP (MD will) (VP (VB be) (VP (VP|<NP-,> (VP|<PP-NP> (VP|<VBN-PP> (VBN taken) (PP (IN from) (NP (NP|<JJ-NN> (JJ several) (NN vantage)) (NNS points)))) (NP (DT this) (NN week))) (, ,)) (PP (IN with) (NP (NP (NNS readings)) (PP (IN on) (NP (NN trade) (, ,) (NN output) (, ,) (NN housing) (CC and) (NN inflation))))))))) (. .)))",
                parser.findBestParse(task).toString());
    }
}

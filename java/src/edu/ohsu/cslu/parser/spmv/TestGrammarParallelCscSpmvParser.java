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
package edu.ohsu.cslu.parser.spmv;

import java.util.logging.Level;

import org.cjunit.PerformanceTest;
import org.junit.Test;

import cltool4j.BaseLogger;
import cltool4j.GlobalConfigProperties;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashPackingFunction;
import edu.ohsu.cslu.parser.ParserDriver;

/**
 * Tests for {@link GrammarParallelCscSpmvParser}.
 * 
 * @author Aaron Dunlop
 * @since Mar 11, 2011
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class TestGrammarParallelCscSpmvParser extends
        SparseMatrixVectorParserTestCase<GrammarParallelCscSpmvParser, PerfectIntPairHashPackingFunction> {

    @Override
    @Test
    @PerformanceTest({ "mbp", "13460" })
    public void profileSentences11Through20() throws Exception {
        internalProfileSentences11Through20();
    }

    @Override
    public void setUp() throws Exception {
        GlobalConfigProperties.singleton().setProperty(ParserDriver.OPT_GRAMMAR_THREAD_COUNT, "8");
        BaseLogger.singleton().setLevel(Level.FINER);
        super.setUp();
    }
}

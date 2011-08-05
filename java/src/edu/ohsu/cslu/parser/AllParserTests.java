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
package edu.ohsu.cslu.parser;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import edu.ohsu.cslu.parser.ecp.TestECPCellCrossHash;
import edu.ohsu.cslu.parser.ecp.TestECPCellCrossList;
import edu.ohsu.cslu.parser.ecp.TestECPCellCrossMatrix;
import edu.ohsu.cslu.parser.ecp.TestECPGramLoop;
import edu.ohsu.cslu.parser.ecp.TestECPGramLoopBerkFilter;
import edu.ohsu.cslu.parser.ml.AllMatrixLoopParserTests;
import edu.ohsu.cslu.parser.spmv.AllSparseMatrixVectorParserTests;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        // TODO AllGrammarTests.class,
        TestParser.class, TestECPGramLoop.class, TestECPGramLoopBerkFilter.class, TestECPCellCrossHash.class,
        TestECPCellCrossList.class, TestECPCellCrossMatrix.class, AllSparseMatrixVectorParserTests.class,
        AllMatrixLoopParserTests.class })
public class AllParserTests {

}

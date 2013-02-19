/*
 * Copyright 2010, 2011 Aaron Dunlop and Nathan Bodenstab
 * 
 * This file is part of the cslu-common project.
 * 
 * cslu-common is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * cslu-common is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with cslu-common. If not, see <http://www.gnu.org/licenses/>
 */
package edu.ohsu.cslu.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import edu.ohsu.cslu.counters.AllCounterTests;
import edu.ohsu.cslu.datastructs.AllDataStructureTests;
import edu.ohsu.cslu.grammar.TestGrammarFormatType;
import edu.ohsu.cslu.hash.AllHashTests;
import edu.ohsu.cslu.util.AllUtilTests;

/**
 * The entire regression suite for common code (counters, datastructs, etc.).
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ AllCounterTests.class, AllDataStructureTests.class, TestGrammarFormatType.class,
        AllHashTests.class, AllUtilTests.class })
public class AllCommonTests {
}

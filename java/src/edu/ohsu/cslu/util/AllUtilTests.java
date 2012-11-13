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
package edu.ohsu.cslu.util;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Util test suite.
 * 
 * @author Aaron Dunlop
 * @since Sep 25, 2008
 * 
 *        $Id$
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ TestMath.class, TestStrings.class, TestEvalb.class })
public class AllUtilTests {
}

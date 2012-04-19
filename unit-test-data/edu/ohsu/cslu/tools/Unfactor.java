/*
 * Copyright 2010-2012 Aaron Dunlop and Nathan Bodenstab
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

package edu.ohsu.cslu.tools;

import cltool4j.BaseCommandlineTool;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.grammar.GrammarFormatType;

/**
 * @author Aaron Dunlop
 * @since Apr 18, 2012
 */
public class Unfactor extends BaseCommandlineTool {

    @Option(name = "-f", metaVar = "format", usage = "Grammar format")
    private GrammarFormatType grammarFormatType = GrammarFormatType.Berkeley;

    @Override
    protected void run() throws Exception {
        for (final String line : inputLines()) {
            if (line.equals("()") || line.equals("")) {
                System.out.println(line);
            } else {
                System.out.println(BinaryTree.read(line, String.class).unfactor(grammarFormatType));
            }
        }
    }

    public static void main(final String[] args) {
        run(args);
    }

}

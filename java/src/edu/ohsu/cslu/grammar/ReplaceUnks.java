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
package edu.ohsu.cslu.grammar;

import java.io.File;

import cltool4j.BaseCommandlineTool;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.util.Strings;

/**
 * Replaces any unknown words in the input with the appropriate UNK token from the grammar
 * 
 * @author Aaron Dunlop
 */
public class ReplaceUnks extends BaseCommandlineTool {

    @Option(name = "-g", metaVar = "FILE", required = true, usage = "Grammar file (text, gzipped text, or binary serialized)")
    private File grammarFile;

    @Override
    protected void run() throws Exception {
        final Grammar g = new ListGrammar(fileAsBufferedReader(grammarFile), new DecisionTreeTokenClassifier());

        for (final String s : inputLines()) {
            final String treebankTokens[] = Tokenizer.treebankTokenize(s).split(" ");
            for (int i = 0; i < treebankTokens.length; i++) {
                treebankTokens[i] = g.tokenClassifier.lexiconEntry(treebankTokens[i], i == 0, g.lexSet);
            }

            System.out.println(Strings.join(treebankTokens, " "));
        }
    }

    public static void main(final String[] args) {
        run(args);
    }

}

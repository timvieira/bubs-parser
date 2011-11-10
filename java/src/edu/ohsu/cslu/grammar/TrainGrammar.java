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
package edu.ohsu.cslu.grammar;

import java.io.IOException;
import java.io.InputStreamReader;

import cltool4j.BaseCommandlineTool;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.datastructs.narytree.NaryTree.Factorization;
import edu.ohsu.cslu.lela.ProductionListGrammar;
import edu.ohsu.cslu.lela.StringCountGrammar;

public class TrainGrammar extends BaseCommandlineTool {

    @Option(name = "-fact", metaVar = "TYPE", usage = "Factorizes unfactored trees. If not specified, assumes trees are already binarized")
    private Factorization factorization = null;

    @Option(name = "-gf", metaVar = "FORMAT", usage = "Grammar Format (required if factorization is specified)")
    private GrammarFormatType grammarFormatType = GrammarFormatType.Berkeley;

    @Option(name = "-unkThresh", metaVar = "THRESH", usage = "The number of observations of a word required in order to add it to the lexicon.")
    private int lexicalUnkThreshold = 1;

    @Override
    protected void run() throws IOException {
        final StringCountGrammar cg = new StringCountGrammar(new InputStreamReader(System.in), factorization,
                grammarFormatType);
        final ProductionListGrammar plg = new ProductionListGrammar(cg);

        System.out.println(plg.toString(false, null, grammarFormatType, lexicalUnkThreshold));
    }

    public static void main(final String[] args) {
        run(args);
    }
}

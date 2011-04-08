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
package edu.ohsu.cslu.ella;

import java.io.FileWriter;
import java.io.InputStreamReader;

import cltool4j.BaseCommandlineTool;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.datastructs.narytree.BinaryTree.Factorization;
import edu.ohsu.cslu.grammar.GrammarFormatType;

public class InduceGrammar extends BaseCommandlineTool {

    @Option(name = "-gp", aliases = { "--grammar-file-prefix" }, required = true, metaVar = "prefix", usage = "Grammar file prefix")
    private String grammarPrefix;

    @Option(name = "-f", aliases = { "--factorization" }, metaVar = "type", usage = "Factorizes unfactored trees. If not specified, assumes trees are already binarized")
    private Factorization factorization = null;

    @Option(name = "-gf", aliases = { "--grammar-format" }, metaVar = "format", usage = "Grammar Format (required if factorization is specified)")
    private GrammarFormatType grammarFormatType = null;

    @Option(name = "-unk", aliases = { "--unk-threshold" }, metaVar = "threshold", usage = "The number of observations of a word required in order to add it to the lexicon.")
    private int lexicalUnkThreshold = 1;

    @Override
    protected void run() throws Exception {
        final StringCountGrammar cg = new StringCountGrammar(new InputStreamReader(System.in), factorization, grammarFormatType,
                lexicalUnkThreshold);
        final ProductionListGrammar plg = new ProductionListGrammar(cg);

        final FileWriter pcfgWriter = new FileWriter(grammarPrefix + ".pcfg");
        pcfgWriter.write(plg.pcfgString());
        pcfgWriter.close();

        final FileWriter lexiconWriter = new FileWriter(grammarPrefix + ".lex");
        lexiconWriter.write(plg.lexiconString());
        lexiconWriter.close();
    }

    public static void main(final String[] args) {
        run(args);
    }
}

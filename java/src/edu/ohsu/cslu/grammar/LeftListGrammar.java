package edu.ohsu.cslu.grammar;

import java.io.Reader;
import java.util.LinkedList;

import edu.ohsu.cslu.parser.ParserOptions.GrammarFormatType;

public class LeftListGrammar extends GrammarByChild {

    protected LinkedList<Production>[] binaryProdsByLeftNonTerm;

    public LeftListGrammar(final String grammarFile, final String lexiconFile, final GrammarFormatType grammarFormat) throws Exception {
        super(grammarFile, lexiconFile, grammarFormat);
    }

    public LeftListGrammar(final Reader grammarFile, final Reader lexiconFile, final GrammarFormatType grammarFormat) throws Exception {
        super(grammarFile, lexiconFile, grammarFormat);
    }

    @SuppressWarnings( { "cast", "unchecked" })
    @Override
    protected void init(final Reader grammarFile, final Reader lexiconFile, final GrammarFormatType grammarFormat) throws Exception {
        readGrammarAndLexicon(grammarFile, lexiconFile, grammarFormat);
        binaryProdsByLeftNonTerm = (LinkedList<Production>[]) new LinkedList[this.numNonTerms()];

        for (final Production p : this.binaryProductions) {
            if (binaryProdsByLeftNonTerm[p.leftChild] == null) {
                binaryProdsByLeftNonTerm[p.leftChild] = new LinkedList<Production>();
            }
            binaryProdsByLeftNonTerm[p.leftChild].add(p);
        }

        // delete the original binary prods since we're storing them by left child now
        this.binaryProductions = null;
    }

    public LinkedList<Production> getBinaryProductionsWithLeftChild(final int leftChild) {
        final LinkedList<Production> prodList = binaryProdsByLeftNonTerm[leftChild];
        if (prodList != null) {
            return prodList;
        }
        // return an empty list instead of 'null' so we can still iterate over it
        return new LinkedList<Production>();
    }

    @Override
    public Production getBinaryProduction(final int parent, final int leftChild, final int rightChild) {
        for (final Production p : getBinaryProductionsWithLeftChild(leftChild)) {
            if (p.parent == parent && p.rightChild == rightChild) {
                return p;
            }
        }
        return null;
    }
}

package edu.ohsu.cslu.grammar;

import java.io.Reader;
import java.util.LinkedList;

public class LeftRightListsGrammar extends LeftListGrammar {

    private LinkedList<Production>[] binaryProdsByRightNonTerm;

    public LeftRightListsGrammar(final String grammarFile, final String lexiconFile,
            final GrammarFormatType grammarFormat) throws Exception {
        super(grammarFile, lexiconFile, grammarFormat);
    }

    public LeftRightListsGrammar(final Reader grammarFile, final Reader lexiconFile,
            final GrammarFormatType grammarFormat) throws Exception {
        super(grammarFile, lexiconFile, grammarFormat);
    }

    @SuppressWarnings({ "cast", "unchecked" })
    @Override
    protected void init(final Reader grammarFile, final Reader lexiconFile) throws Exception {
        readGrammarAndLexicon(grammarFile, lexiconFile);

        binaryProdsByLeftNonTerm = (LinkedList<Production>[]) new LinkedList[this.numNonTerms()];
        binaryProdsByRightNonTerm = (LinkedList<Production>[]) new LinkedList[this.numNonTerms()];

        for (final Production p : this.binaryProductions) {
            if (binaryProdsByLeftNonTerm[p.leftChild] == null) {
                binaryProdsByLeftNonTerm[p.leftChild] = new LinkedList<Production>();
            }
            binaryProdsByLeftNonTerm[p.leftChild].add(p);

            if (binaryProdsByRightNonTerm[p.rightChild] == null) {
                binaryProdsByRightNonTerm[p.rightChild] = new LinkedList<Production>();
            }
            binaryProdsByRightNonTerm[p.rightChild].add(p);
        }

        // delete the original binary prods since we're storing them by left child now
        this.binaryProductions = null;
    }

    public LinkedList<Production> getBinaryProductionsWithRightChild(final int rightChild) {
        final LinkedList<Production> prodList = binaryProdsByRightNonTerm[rightChild];
        if (prodList != null) {
            return prodList;
        }
        return new LinkedList<Production>();
    }
}

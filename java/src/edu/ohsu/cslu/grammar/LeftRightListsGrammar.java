package edu.ohsu.cslu.grammar;

import java.io.Reader;
import java.util.LinkedList;

public class LeftRightListsGrammar extends LeftListGrammar {

    private LinkedList<Production>[] binaryProdsByRightNonTerm;

    public LeftRightListsGrammar(final String grammarFile) throws Exception {
        super(grammarFile);
    }

    public LeftRightListsGrammar(final Reader grammarFile) throws Exception {
        super(grammarFile);
    }

    @SuppressWarnings({ "cast", "unchecked" })
    @Override
    protected GrammarFormatType init(final Reader grammarFile) throws Exception {
        final GrammarFormatType gf = readGrammarAndLexicon(grammarFile);

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
        return gf;
    }

    public LinkedList<Production> getBinaryProductionsWithRightChild(final int rightChild) {
        final LinkedList<Production> prodList = binaryProdsByRightNonTerm[rightChild];
        if (prodList != null) {
            return prodList;
        }
        return new LinkedList<Production>();
    }
}

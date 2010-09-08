package edu.ohsu.cslu.grammar;

import java.io.Reader;
import java.util.LinkedList;

public class LeftListGrammar extends GrammarByChild {

    protected LinkedList<Production>[] binaryProdsByLeftNonTerm;

    public LeftListGrammar(final String grammarFile) throws Exception {
        super(grammarFile);
    }

    public LeftListGrammar(final Reader grammarFile) throws Exception {
        super(grammarFile);
    }

    @SuppressWarnings({ "cast", "unchecked" })
    @Override
    protected GrammarFormatType init(final Reader grammarFile) throws Exception {
        final GrammarFormatType gf = readGrammarAndLexicon(grammarFile);
        binaryProdsByLeftNonTerm = (LinkedList<Production>[]) new LinkedList[this.numNonTerms()];

        for (final Production p : this.binaryProductions) {
            if (binaryProdsByLeftNonTerm[p.leftChild] == null) {
                binaryProdsByLeftNonTerm[p.leftChild] = new LinkedList<Production>();
            }
            binaryProdsByLeftNonTerm[p.leftChild].add(p);
        }

        // delete the original binary prods since we're storing them by left child now
        this.binaryProductions = null;
        return gf;
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

package edu.ohsu.cslu.grammar;

import java.io.FileReader;
import java.io.Reader;
import java.util.LinkedList;

public class LeftRightListsGrammar extends LeftListGrammar {

    private LinkedList<Production>[] binaryProdsByRightNonTerm;

    public LeftRightListsGrammar(final Reader grammarFile) throws Exception {
        super(grammarFile);
        init();
    }

    public LeftRightListsGrammar(final String grammarFile) throws Exception {
        this(new FileReader(grammarFile));
    }

    public LeftRightListsGrammar(final Grammar g) throws Exception {
        super(g);
        init();
    }

    @SuppressWarnings("unchecked")
    private void init() {
        binaryProdsByLeftNonTerm = new LinkedList[this.numNonTerms()];
        binaryProdsByRightNonTerm = new LinkedList[this.numNonTerms()];

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
    }

    public LinkedList<Production> getBinaryProductionsWithRightChild(final int rightChild) {
        final LinkedList<Production> prodList = binaryProdsByRightNonTerm[rightChild];
        if (prodList != null) {
            return prodList;
        }
        return new LinkedList<Production>();
    }
}

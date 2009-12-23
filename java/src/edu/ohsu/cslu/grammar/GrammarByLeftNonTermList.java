package edu.ohsu.cslu.grammar;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedList;

public class GrammarByLeftNonTermList extends Grammar {

    private ArrayList<LinkedList<Production>> binaryProdsByLeftNonTerm, binaryProdsByRightNonTerm;

    public GrammarByLeftNonTermList(final String grammarFile, final String lexiconFile) throws IOException {
        super(grammarFile, lexiconFile);
    }

    public GrammarByLeftNonTermList(final Reader grammarFile, final Reader lexiconFile) throws IOException {
        super(grammarFile, lexiconFile);
    }

    @Override
    protected void init(final Reader grammarFile, final Reader lexiconFile) throws IOException {
        super.init(grammarFile, lexiconFile);

        binaryProdsByLeftNonTerm = new ArrayList<LinkedList<Production>>(this.numNonTerms());
        binaryProdsByRightNonTerm = new ArrayList<LinkedList<Production>>(this.numNonTerms());
        for (int i = 0; i < this.numNonTerms(); i++) {
            binaryProdsByLeftNonTerm.add(i, null);
            binaryProdsByRightNonTerm.add(i, null);
        }

        for (final Production p : this.binaryProds) {
            if (binaryProdsByLeftNonTerm.get(p.leftChild) == null) {
                binaryProdsByLeftNonTerm.set(p.leftChild, new LinkedList<Production>());
            }
            binaryProdsByLeftNonTerm.get(p.leftChild).add(p);

            if (binaryProdsByRightNonTerm.get(p.rightChild) == null) {
                binaryProdsByRightNonTerm.set(p.rightChild, new LinkedList<Production>());
            }
            binaryProdsByRightNonTerm.get(p.rightChild).add(p);
        }

        // delete the original binary prods since we're storing them by left child now
        this.binaryProds = null;
    }

    public LinkedList<Production> getBinaryProdsWithLeftChild(final int nonTerm) {
        return binaryProdsByLeftNonTerm.get(nonTerm);
    }

    public LinkedList<Production> getBinaryProdsWithRightChild(final int nonTerm) {
        return binaryProdsByRightNonTerm.get(nonTerm);
    }
}

package edu.ohsu.cslu.grammar;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class GrammarByLeftNonTermHash extends ArrayGrammar {

    private ArrayList<HashMap<Integer, LinkedList<Production>>> binaryProdHash;

    public GrammarByLeftNonTermHash(final String grammarFile, final String lexiconFile) throws IOException {
        super(grammarFile, lexiconFile);
    }

    public GrammarByLeftNonTermHash(final Reader grammarFile, final Reader lexiconFile) throws IOException {
        super(grammarFile, lexiconFile);
    }

    @Override
    protected void init(final Reader grammarFile, final Reader lexiconFile) throws IOException {
        super.init(grammarFile, lexiconFile);

        binaryProdHash = new ArrayList<HashMap<Integer, LinkedList<Production>>>(this.numNonTerms());
        for (int i = 0; i < this.numNonTerms(); i++) {
            binaryProdHash.add(i, null);
        }

        // add productions to array (left child) of hash maps (right child) which returns
        // a list of valid productions
        for (final Production p : this.binaryProds) {
            if (binaryProdHash.get(p.leftChild) == null) {
                binaryProdHash.set(p.leftChild, new HashMap<Integer, LinkedList<Production>>());
            }

            if (binaryProdHash.get(p.leftChild).containsKey(p.rightChild) == false) {
                binaryProdHash.get(p.leftChild).put(p.rightChild, new LinkedList<Production>());
            }

            binaryProdHash.get(p.leftChild).get(p.rightChild).add(p);
        }

        // delete the original binary prods since we're storing them by left child now
        this.binaryProds = null;
    }

    public LinkedList<Production> getBinaryProdsByChildren(final int leftChild, final int rightChild) {
        final HashMap<Integer, LinkedList<Production>> leftChildHash = binaryProdHash.get(leftChild);
        if (leftChildHash == null) {
            return null;
        } else {
            return leftChildHash.get(rightChild);
        }
    }
}

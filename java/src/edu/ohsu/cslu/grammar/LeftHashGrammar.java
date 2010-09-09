package edu.ohsu.cslu.grammar;

import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

public class LeftHashGrammar extends Grammar {

    private ArrayList<HashMap<Integer, LinkedList<Production>>> binaryProdHash;

    public LeftHashGrammar(final Reader grammarFile) throws Exception {
        super(grammarFile);
        init();
    }

    public LeftHashGrammar(final String grammarFile) throws Exception {
        this(new FileReader(grammarFile));
    }

    public LeftHashGrammar(final Grammar g) throws Exception {
        super(g);
        init();
    }

    private void init() {
        binaryProdHash = new ArrayList<HashMap<Integer, LinkedList<Production>>>(this.numNonTerms());
        for (int i = 0; i < this.numNonTerms(); i++) {
            binaryProdHash.add(i, null);
        }

        // add productions to array (left child) of hash maps (right child) which returns
        // a list of valid productions
        for (final Production p : this.binaryProductions) {
            if (binaryProdHash.get(p.leftChild) == null) {
                binaryProdHash.set(p.leftChild, new HashMap<Integer, LinkedList<Production>>());
            }

            if (binaryProdHash.get(p.leftChild).containsKey(p.rightChild) == false) {
                binaryProdHash.get(p.leftChild).put(p.rightChild, new LinkedList<Production>());
            }

            binaryProdHash.get(p.leftChild).get(p.rightChild).add(p);
        }
    }

    public Collection<Production> getBinaryProductionsWithChildren(final int leftChild, final int rightChild) {
        final HashMap<Integer, LinkedList<Production>> leftChildHash = binaryProdHash.get(leftChild);
        if (leftChildHash == null) {
            return new LinkedList<Production>();
        }
        final LinkedList<Production> productions = leftChildHash.get(rightChild);
        if (productions == null) {
            return new LinkedList<Production>();
        }
        return productions;
    }

    @Override
    public Production getBinaryProduction(final String A, final String B, final String C) {
        if (nonTermSet.hasSymbol(A) && nonTermSet.hasSymbol(B) && nonTermSet.hasSymbol(C)) {
            final int parent = nonTermSet.getIndex(A);
            final int leftChild = nonTermSet.getIndex(B);
            final int rightChild = nonTermSet.getIndex(C);
            // System.out.println("A=" + parent + "(" + A + ") B=" + leftChild + "(" + B + ") C=" + rightChild
            // + "(" + C + ")");
            for (final Production p : getBinaryProductionsWithChildren(leftChild, rightChild)) {
                if (p.parent == parent) {
                    return p;
                }
            }
        }
        return null;
    }
}

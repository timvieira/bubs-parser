package edu.ohsu.cslu.grammar;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

import edu.ohsu.cslu.parser.ParserOptions.GrammarFormatType;

public class LeftHashGrammar extends GrammarByChild {

    private ArrayList<HashMap<Integer, LinkedList<Production>>> binaryProdHash;

    public LeftHashGrammar(final String grammarFile, final String lexiconFile, final GrammarFormatType grammarFormat) throws Exception {
        super(grammarFile, lexiconFile, grammarFormat);
    }

    public LeftHashGrammar(final Reader grammarFile, final Reader lexiconFile, final GrammarFormatType grammarFormat) throws Exception {
        super(grammarFile, lexiconFile, grammarFormat);
    }

    @Override
    protected void init(final Reader grammarFile, final Reader lexiconFile, final GrammarFormatType grammarFormat) throws Exception {
        readGrammarAndLexicon(grammarFile, lexiconFile, grammarFormat);

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

        // delete the original binary prods since we're storing them by left child now
        this.binaryProductions = null;
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
}

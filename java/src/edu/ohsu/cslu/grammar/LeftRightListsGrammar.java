package edu.ohsu.cslu.grammar;

import java.io.Reader;
import java.util.LinkedList;

import edu.ohsu.cslu.parser.ParserDriver.GrammarFormatType;

public class LeftRightListsGrammar extends LeftListGrammar {

    // private ArrayList<LinkedList<Production>> binaryProdsByLeftNonTerm, binaryProdsByRightNonTerm;
    private LinkedList<Production>[] binaryProdsByRightNonTerm;

    public LeftRightListsGrammar(final String grammarFile, final String lexiconFile, final GrammarFormatType grammarFormat) throws Exception {
        super(grammarFile, lexiconFile, grammarFormat);
    }

    public LeftRightListsGrammar(final Reader grammarFile, final Reader lexiconFile, final GrammarFormatType grammarFormat) throws Exception {
        super(grammarFile, lexiconFile, grammarFormat);
    }

    @SuppressWarnings( { "cast", "unchecked" })
    @Override
    protected void init(final Reader grammarFile, final Reader lexiconFile, final GrammarFormatType grammarFormat) throws Exception {
        readGrammarAndLexicon(grammarFile, lexiconFile, grammarFormat);

        // binaryProdsByLeftNonTerm = new ArrayList<LinkedList<Production>>(this.numNonTerms());
        // binaryProdsByRightNonTerm = new ArrayList<LinkedList<Production>>(this.numNonTerms());
        binaryProdsByLeftNonTerm = (LinkedList<Production>[]) new LinkedList[this.numNonTerms()];
        binaryProdsByRightNonTerm = (LinkedList<Production>[]) new LinkedList[this.numNonTerms()];
        // for (int i = 0; i < this.numNonTerms(); i++) {
        // binaryProdsByLeftNonTerm.add(i, null);
        // binaryProdsByRightNonTerm.add(i, null);
        // }

        for (final Production p : this.binaryProductions) {
            // if (binaryProdsByLeftNonTerm.get(p.leftChild) == null) {
            // binaryProdsByLeftNonTerm.set(p.leftChild, new LinkedList<Production>());
            // }
            // binaryProdsByLeftNonTerm.get(p.leftChild).add(p);
            //
            // if (binaryProdsByRightNonTerm.get(p.rightChild) == null) {
            // binaryProdsByRightNonTerm.set(p.rightChild, new LinkedList<Production>());
            // }
            // binaryProdsByRightNonTerm.get(p.rightChild).add(p);

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

    // inherited from LeftListGrammar
    // public LinkedList<Production> getBinaryProductionsWithLeftChild(final int leftChild) {

    @Override
    public LinkedList<Production> getBinaryProductionsWithRightChild(final int rightChild) {
        // final LinkedList<Production> prodList = binaryProdsByRightNonTerm.get(nonTerm);
        final LinkedList<Production> prodList = binaryProdsByRightNonTerm[rightChild];
        if (prodList != null) {
            return prodList;
        }
        return new LinkedList<Production>();
    }
}

package edu.ohsu.cslu.grammar;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedList;

import edu.ohsu.cslu.parser.ParserDriver.GrammarFormatType;

public class GrammarByLeftNonTermList extends ArrayGrammar {

    private ArrayList<LinkedList<Production>> binaryProdsByLeftNonTerm, binaryProdsByRightNonTerm;

    public GrammarByLeftNonTermList(final Reader grammarFile, final Reader lexiconFile, final GrammarFormatType grammarFormat) throws IOException {
        super(grammarFile, lexiconFile, grammarFormat);

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

    public GrammarByLeftNonTermList(final String grammarFile, final String lexiconFile, final GrammarFormatType grammarFormat) throws IOException {
        this(new FileReader(grammarFile), new FileReader(lexiconFile), grammarFormat);
    }

    public LinkedList<Production> getBinaryProdsWithLeftChild(final int nonTerm) {
        final LinkedList<Production> prodList = binaryProdsByLeftNonTerm.get(nonTerm);
        if (prodList != null) {
            return prodList;
        }
        // return an empty list instead of 'null' so we can still iterate over it
        return new LinkedList<Production>();
    }

    public LinkedList<Production> getBinaryProdsWithRightChild(final int nonTerm) {
        final LinkedList<Production> prodList = binaryProdsByRightNonTerm.get(nonTerm);
        if (prodList != null) {
            return prodList;
        }
        return new LinkedList<Production>();
    }
}

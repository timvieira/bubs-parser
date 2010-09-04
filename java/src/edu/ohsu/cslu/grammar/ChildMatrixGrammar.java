package edu.ohsu.cslu.grammar;

import java.io.Reader;
import java.util.LinkedList;

public class ChildMatrixGrammar extends GrammarByChild {

    public LinkedList<Production>[][] binaryProdMatrix;

    public ChildMatrixGrammar(final String grammarFile, final String lexiconFile, final GrammarFormatType grammarFormat)
            throws Exception {
        super(grammarFile, lexiconFile, grammarFormat);
    }

    public ChildMatrixGrammar(final Reader grammarFile, final Reader lexiconFile, final GrammarFormatType grammarFormat)
            throws Exception {
        super(grammarFile, lexiconFile, grammarFormat);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void init(final Reader grammarFile, final Reader lexiconFile) throws Exception {
        super.init(grammarFile, lexiconFile);

        binaryProdMatrix = new LinkedList[this.numNonTerms()][this.numNonTerms()];

        for (final Production p : this.binaryProductions) {
            if (binaryProdMatrix[p.leftChild][p.rightChild] == null) {
                binaryProdMatrix[p.leftChild][p.rightChild] = new LinkedList<Production>();
            }
            binaryProdMatrix[p.leftChild][p.rightChild].add(p);
        }

        // delete the original binary prods since we're storing them by left child now
        this.binaryProductions = null;
    }

}

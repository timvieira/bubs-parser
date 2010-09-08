package edu.ohsu.cslu.grammar;

import java.io.Reader;
import java.util.LinkedList;

public class ChildMatrixGrammar extends GrammarByChild {

    public LinkedList<Production>[][] binaryProdMatrix;

    public ChildMatrixGrammar(final String grammarFile) throws Exception {
        super(grammarFile);
    }

    public ChildMatrixGrammar(final Reader grammarFile) throws Exception {
        super(grammarFile);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected GrammarFormatType init(final Reader grammarFile) throws Exception {
        final GrammarFormatType gf = super.init(grammarFile);

        binaryProdMatrix = new LinkedList[this.numNonTerms()][this.numNonTerms()];

        for (final Production p : this.binaryProductions) {
            if (binaryProdMatrix[p.leftChild][p.rightChild] == null) {
                binaryProdMatrix[p.leftChild][p.rightChild] = new LinkedList<Production>();
            }
            binaryProdMatrix[p.leftChild][p.rightChild].add(p);
        }

        // delete the original binary prods since we're storing them by left child now
        this.binaryProductions = null;
        return gf;
    }

}

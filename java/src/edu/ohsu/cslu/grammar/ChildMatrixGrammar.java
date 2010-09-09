package edu.ohsu.cslu.grammar;

import java.io.FileReader;
import java.io.Reader;
import java.util.LinkedList;

public class ChildMatrixGrammar extends Grammar {

    public LinkedList<Production>[][] binaryProdMatrix;

    public ChildMatrixGrammar(final Reader grammarFile) throws Exception {
        super(grammarFile);
        init();
    }

    public ChildMatrixGrammar(final String grammarFile) throws Exception {
        this(new FileReader(grammarFile));
    }

    public ChildMatrixGrammar(final Grammar g) throws Exception {
        super(g);
        init();
    }

    @SuppressWarnings("unchecked")
    private void init() {
        binaryProdMatrix = new LinkedList[this.numNonTerms()][this.numNonTerms()];

        for (final Production p : this.binaryProductions) {
            if (binaryProdMatrix[p.leftChild][p.rightChild] == null) {
                binaryProdMatrix[p.leftChild][p.rightChild] = new LinkedList<Production>();
            }
            binaryProdMatrix[p.leftChild][p.rightChild].add(p);
        }
    }

}

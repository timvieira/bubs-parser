package edu.ohsu.cslu.grammar;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedList;

import edu.ohsu.cslu.parser.ParserDriver.GrammarFormatType;

public class GrammarByChildMatrix extends ArrayGrammar {

    public ArrayList<ArrayList<LinkedList<Production>>> binaryProdMatrix;
    public LinkedList<Production>[][] binaryProdMatrix2;

    public GrammarByChildMatrix(final Reader grammarFile, final Reader lexiconFile, final GrammarFormatType grammarFormat) throws IOException {
        super(grammarFile, lexiconFile, grammarFormat);

        binaryProdMatrix2 = new LinkedList[this.numNonTerms()][this.numNonTerms()];

        binaryProdMatrix = new ArrayList<ArrayList<LinkedList<Production>>>(this.numNonTerms());
        for (int i = 0; i < this.numNonTerms(); i++) {
            binaryProdMatrix.add(i, null);
            for (int j = 0; j < this.numNonTerms(); j++) {
                binaryProdMatrix2[i][j] = null;
            }
        }

        for (final Production p : this.binaryProds) {
            if (binaryProdMatrix.get(p.leftChild) == null) {
                binaryProdMatrix.set(p.leftChild, new ArrayList<LinkedList<Production>>(this.numNonTerms()));
                for (int i = 0; i < this.numNonTerms(); i++) {
                    binaryProdMatrix.get(p.leftChild).add(i, null);
                }
            }

            if (binaryProdMatrix.get(p.leftChild).get(p.rightChild) == null) {
                binaryProdMatrix.get(p.leftChild).set(p.rightChild, new LinkedList<Production>());
                if (binaryProdMatrix2[p.leftChild][p.rightChild] == null) {
                    binaryProdMatrix2[p.leftChild][p.rightChild] = new LinkedList<Production>();
                }
            }
            binaryProdMatrix.get(p.leftChild).get(p.rightChild).add(p);
            binaryProdMatrix2[p.leftChild][p.rightChild].add(p);
        }

        // delete the original binary prods since we're storing them by left child now
        this.binaryProds = null;
    }

    public GrammarByChildMatrix(final String grammarFile, final String lexiconFile, final GrammarFormatType grammarFormat) throws IOException {
        this(new FileReader(grammarFile), new FileReader(lexiconFile), grammarFormat);
    }

    public LinkedList<Production> getBinaryProdsByChildren(final int leftChild, final int rightChild) {
        final ArrayList<LinkedList<Production>> leftChildArray = binaryProdMatrix.get(leftChild);
        if (binaryProdMatrix == null) {
            return null;
        } else {
            return leftChildArray.get(rightChild);
        }
    }
}

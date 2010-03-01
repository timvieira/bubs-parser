package edu.ohsu.cslu.grammar;

import java.io.FileReader;
import java.io.Reader;

import edu.ohsu.cslu.datastructs.vectors.PackedBitVector;
import edu.ohsu.cslu.parser.ParserDriver.GrammarFormatType;

/**
 * Stores a grammar as a pair of unordered production arrays. Used by parsers which iterate over the entire grammar at each step. For this parsing method, the efficiency of array
 * storage is particularly important, but order of the grammar is unimportant.
 * 
 * @author Nathan Bodenstab
 * @since Dec 10, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class ArrayGrammar extends Grammar {

    /** Unary productions, stored in the order read in from the grammar file */
    public Production[] unaryProds;

    /** Binary productions, stored in the order read in from the grammar file */
    public Production[] binaryProds;

    private PackedBitVector possibleLeftChild;
    private PackedBitVector possibleRightChild;

    public ArrayGrammar(final Reader grammarFile, final Reader lexiconFile, final GrammarFormatType grammarFormat) throws Exception {
        super(grammarFile, lexiconFile, grammarFormat);

        unaryProds = unaryProductions.toArray(new Production[unaryProductions.size()]);
        binaryProds = binaryProductions.toArray(new Production[binaryProductions.size()]);

        markLeftRightChildren();
    }

    public ArrayGrammar(final String grammarFile, final String lexiconFile, final GrammarFormatType grammarFormat) throws Exception {
        this(new FileReader(grammarFile), new FileReader(lexiconFile), grammarFormat);
    }

    private void markLeftRightChildren() {
        possibleLeftChild = new PackedBitVector(this.numNonTerms());
        possibleRightChild = new PackedBitVector(this.numNonTerms());
        // Arrays.fill(possibleLeftChild, false);
        // Arrays.fill(possibleRightChild, false);
        for (final Production p : binaryProds) {
            possibleLeftChild.set(p.leftChild, true);
            possibleRightChild.set(p.rightChild, true);
        }
    }
}

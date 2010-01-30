package edu.ohsu.cslu.grammar;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import edu.ohsu.cslu.parser.ParserDriver.GrammarFormatType;

/**
 * Stores a grammar as a sparse matrix of probabilities.
 * 
 * Matrix columns are indexed by production index (an int).
 * 
 * Matrix rows are indexed by the concatenation of left and right child indices (two 32-bit ints concatenated into a long).
 * 
 * @author Aaron Dunlop
 * @since Dec 31, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */

public abstract class BaseSparseMatrixGrammar extends BaseSortedGrammar {

    public BaseSparseMatrixGrammar(final Reader grammarFile, final Reader lexiconFile, final GrammarFormatType grammarFormat) throws IOException {
        super(grammarFile, lexiconFile, grammarFormat);
    }

    public BaseSparseMatrixGrammar(final String grammarFile, final String lexiconFile, final GrammarFormatType grammarFormat) throws IOException {
        this(new FileReader(grammarFile), new FileReader(lexiconFile), grammarFormat);
    }
}

package edu.ohsu.cslu.grammar;

import java.util.List;

import edu.ohsu.cslu.grammar.BaseGrammar.Production;
import edu.ohsu.cslu.grammar.Tokenizer.Token;

/**
 * Interface for PCFG implementations.
 * 
 * @author Aaron Dunlop
 * @since Jan 7, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public interface Grammar {

    public Token[] tokenize(final String sentence) throws Exception;

    public List<Production> getLexProdsForToken(final Token token);

    public List<Production> getUnaryProdsWithChild(final int child);

    /**
     * Returns the log probability of the specified binary grammar rule
     * 
     * @param parent non-terminal
     * @param leftChild non-terminal
     * @param rightChild non-terminal
     * @return Log probability of the specified rule
     */
    public float logProbability(final String parent, final String leftChild, final String rightChild);

    /**
     * Returns the log probability of the specified unary grammar rule
     * 
     * @param parent non-terminal
     * @param child non-terminal
     * @return Log probability of the specified rule
     */
    public float logProbability(final String parent, final String child);

    /**
     * Returns the log probability of the specified lexical rule
     * 
     * @param parent non-terminal
     * @param child terminal
     * @return Log probability of the specified rule
     */
    public float lexicalLogProbability(final String parent, final String child);

    /**
     * Returns a string representation of important statistics about the grammar (number of non-terminals, counts of unary, binary, and lexical rules, etc.). Grammar
     * implementations should provide any statistics significant to their own storage formats.
     * 
     * @return a string representation of important statistics about the grammar
     */
    public String getStats();
}

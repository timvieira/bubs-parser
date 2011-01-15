package edu.ohsu.cslu.ella;


/**
 * A Grammar computed from observation counts.
 * 
 * @author Aaron Dunlop
 * @since Jan 13, 2011
 * 
 * @version $Revision$ $Date$ $Author$
 */
public interface CountGrammar {

    /**
     * Returns the number of observations of a binary rule.
     * 
     * @param parent
     * @param leftChild
     * @param rightChild
     * @return the number of observations of a binary rule.
     */
    public int binaryRuleObservations(final String parent, final String leftChild, final String rightChild);

    /**
     * Returns the number of observations of a unary rule.
     * 
     * @param parent
     * @param child
     * @return the number of observations of a unary rule.
     */
    public int unaryRuleObservations(final String parent, final String child);

    /**
     * Returns the number of observations of a lexical rule.
     * 
     * @param parent
     * @param child
     * @return the number of observations of a lexical rule.
     */
    public int lexicalRuleObservations(final String parent, final String child);

    /**
     * @return The total number of observed rules
     */
    public int totalRules();

    /**
     * @return The number of observed binary rules
     */
    public int binaryRules();

    /**
     * @return The number of observed unary rules
     */
    public int unaryRules();

    /**
     * @return The number of observed lexical rules
     */
    public int lexicalRules();

    /**
     * Returns the total number of times a non-terminal was observed as a parent
     * 
     * @param parent
     * @return the total number of times a non-terminal was observed as a parent
     */
    public int observations(final String parent);

}
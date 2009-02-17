package edu.ohsu.cslu.parsing.grammar;

import edu.ohsu.cslu.common.Vocabulary;

/**
 * Represents a probabilistic context-free grammar (PCFG) in which all categories and productions
 * are modeled as strings.
 * 
 * @author Aaron Dunlop
 * @since Jul 21, 2008
 * 
 *        $Id$
 */
public interface StringGrammar extends Grammar, Vocabulary
{
    /**
     * @return The special start symbol (S-dagger)
     */
    public String startSymbol();

    /**
     * Returns the number of occurrences of a specified category.
     * 
     * @param category
     * @return count
     */
    public int occurrences(String category);

    /**
     * Returns the number of occurrences of a specified unary production given a category.
     * 
     * @param category
     * @param production
     * @return count
     */
    public int occurrences(String category, String production);

    /**
     * Returns the number of occurrences of a specified binary production given a category.
     * 
     * @param category
     * @param production1
     * @param production2
     * @return count
     */
    public int occurrences(String category, String production1, String production2);

    /**
     * Returns the probability of a specified unary production given a category.
     * 
     * @param category
     * @param production
     * @return Probability of the specified production
     */
    public float probability(String category, String production);

    /**
     * Returns the probability of a specified binary production given a category.
     * 
     * @param category
     * @param production1
     * @param production2
     * @return Probability of the specified production
     */
    public float probability(String category, String production1, String production2);

    /**
     * Returns the log probability of a specified unary production given a category.
     * 
     * @param category
     * @param production
     * @return Log probability of the specified production
     */
    public float logProbability(String category, String production);

    /**
     * Returns the log probability of a specified binary production given a category.
     * 
     * @param category
     * @param production1
     * @param production2
     * @return Log probability of the specified production
     */
    public float logProbability(String category, String production1, String production2);

    /**
     * @return The complete set of categories modeled in this grammar
     */
    public String[] stringCategories();

    /**
     * @return The complete set of productions modeled in this grammar
     */
    public String[] stringProductions();

    /**
     * Maps an integer production index to the String production label it represents
     * 
     * @param productionIndex
     * @return String production label mapped by the specified index
     */
    public String mapProduction(int productionIndex);

    /**
     * Maps a String production to its associated integer index
     * 
     * @param production
     * @return Integer production mapped from the specified label
     */
    public int mapProduction(String production);

    /**
     * @return The set of productions which occur as production 1 in this grammar (either as a unary
     *         production or as the first production in a binary production)
     */
    public String[] firstStringProductions();

    /**
     * @return The set of productions which occur as production 2 of a binary production in this
     *         grammar
     */
    public String[] secondStringProductions();

    /**
     * @return The set of categories which could produce the specified production, either as a unary
     *         production or as the first production in a binary production.
     */
    public String[] possibleCategories(String production1);

    /**
     * @return The set of categories which could produce the specified binary production
     */
    public String[] binaryProductionCategories(String production1, String production2);

    /**
     * @return The set of categories which could produce the specified binary production, and which
     *         are in turn unary productions themselves of the specified category.
     */
    public String[] validTopCategories(String production1, String production2);

    /**
     * Returns true if the specified production is valid as a unary production
     * 
     * @param production
     * @return true if the specified production is valid as a unary production
     */
    public boolean validUnaryProduction(String production);

    /**
     * Returns true if the specified production is valid as the first production in a binary
     * production
     * 
     * @param production
     * @return true if the specified production is valid as the first production in a binary
     *         production
     */
    public boolean validFirstProduction(String production);

    /**
     * Returns true if the specified production is valid as the second production in a binary
     * production
     * 
     * @param production
     * @return true if the specified production is valid as the second production in a binary
     *         production
     */
    public boolean validSecondProduction(String production);

    /**
     * Returns the set of productions which are valid as the second production in a binary
     * production starting with the specified first production.
     * 
     * @param production1 The first production in a binary production
     * @return Set of valid second productions
     */
    public String[] validSecondProductions(String production1);
}
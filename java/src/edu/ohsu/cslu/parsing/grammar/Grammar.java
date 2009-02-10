package edu.ohsu.cslu.parsing.grammar;

/**
 * Represents a Probabilistic Context Free Grammar (PCFG). Such grammars may be built up
 * programatically or may be inferred from a corpus of data.
 * 
 * A PCFG consists of
 * <ul>
 * <li>A set of non-terminal symbols V (referred to as 'categories'</li>
 * <li>A special start symbol (S-dagger) from V</li>
 * <li>A set of terminal symbols T</li>
 * <li>A set of rule productions P mapping from V to (V union T)</li>
 * </ul>
 * 
 * Such grammars are useful in modeling and analyzing the structure of natural language or the
 * (secondary) structure of many biological sequences.
 * 
 * Although branching of arbitrary size is possible in a grammar under this definition, this grammar
 * models grammars which have been factored into binary-branching form, enabling much more efficient
 * computational approaches to be used.
 * 
 * Classes implementing this interface are generally assumed to be data-driven, so they must
 * implement methods returning the number of 'occurrences' of a particular rule found in the corpus
 * as well as methods returning the modeled probability of such rules.
 * 
 * Note that it's still possible to build up simple Grammars programatically by generating
 * pseudo-counts for 'observations'.
 * 
 * Note - there will always be more productions than categories, but all categories except the
 * special S-dagger category are also productions. The index of a category will be the same when
 * used as a production as it is when used as a category.
 * 
 * S-dagger is always represented as category 0 - note that this means that there is no production
 * 0.
 * 
 * TODO: Generalize to grammars which are not in Chomsky-Normal-Form?
 * 
 * @author Aaron Dunlop
 * @since Jul 30, 2008
 * 
 *        $Id$
 */
public interface Grammar
{
    /**
     * Returns the number of occurrences of a specified category.
     * 
     * @param category
     * @param production
     * 
     * @return
     */
    public int occurrences(int category);

    /**
     * Returns the number of occurrences of a specified unary production given a category.
     * 
     * @param category
     * @param production
     * 
     * @return
     */
    public int occurrences(int category, int production);

    /**
     * Returns the number of occurrences of a specified binary production given a category.
     * 
     * @param category
     * @param production1
     * @param production2
     * 
     * @return
     */
    public int occurrences(int category, int production1, int production2);

    /**
     * Returns the probability of a specified unary production given a category.
     * 
     * @param category
     * @param production
     * 
     * @return
     */
    public float probability(int category, int production);

    /**
     * Returns the probability of a specified binary production given a category.
     * 
     * @param category
     * @param production1
     * @param production2
     * 
     * @return
     */
    public float probability(int category, int production1, int production2);

    /**
     * Returns the log probability of a specified unary production given a category.
     * 
     * @param category
     * @param production1
     * @param production2
     * 
     * @return
     */
    public float logProbability(int category, int production);

    /**
     * Returns the log probability of a specified binary production given a category.
     * 
     * @param category
     * @param production1
     * @param production2
     * 
     * @return
     */
    public float logProbability(int category, int production1, int production2);

    /**
     * @return The complete set of categories modeled in this grammar
     */
    public int[] categories();

    /**
     * @return The complete set of productions modeled in this grammar
     */
    public int[] productions();

    /**
     * @return The total number of rules modeled by this grammar
     */
    public int totalRules();

    /**
     * @return The set of productions which occur as production 1 in this grammer (either as a unary
     *         production or as the first production in a binary production)
     */
    public int[] firstProductions();

    /**
     * @return The set of productions which occur as production 2 of binary productions in this
     *         grammar
     */
    public int[] secondProductions();

    /**
     * @return The set of categories which could produce the specified production, either as a unary
     *         production or as the first production in a binary production.
     */
    public int[] possibleCategories(int production1);

    /**
     * @return The set of categories which could produce the specified binary production
     */
    public int[] binaryProductionCategories(int production1, int production2);

    /**
     * @return The set of categories which could produce the specified binary production, and which
     *         are in turn unary productions themselves of the 'TOP' category.
     */
    public int[] validTopCategories(int production1, int production2);

    /**
     * Returns true if the specified production is valid as a unary production
     * 
     * @param production
     * @return true if the specified production is valid as a unary production
     */
    public boolean validUnaryProduction(int production);

    /**
     * Returns true if the specified production is valid as the first production in a binary
     * production
     * 
     * @param production
     * @return true if the specified production is valid as the first production in a binary
     *         production
     */
    public boolean validFirstProduction(int production);

    /**
     * Returns true if the specified production is valid as the second production in a binary
     * production
     * 
     * @param production
     * @return true if the specified production is valid as the second production in a binary
     *         production
     */
    public boolean validSecondProduction(int production);

    /**
     * Returns the set of productions which are valid as the second production in a binary
     * production starting with the specified first production.
     * 
     * @param production1 The first production in a binary production
     * @return Set of valid second productions
     */
    public int[] validSecondProductions(int production1);
}

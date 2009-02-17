package edu.ohsu.cslu.parsing.grammar;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import edu.ohsu.cslu.math.linear.BitVector;

/**
 * Base Grammar implementation. Stores all productions as ints for quick access (note that this
 * limits it to representing ~2 billion unique productions, but that shouldn't be problematic for
 * most usage cases).
 * 
 * TODO: Add a 'fixed-point' probability, representing log probabilities as ints or shorts.
 * Potential speed increase?
 * 
 * @author Aaron Dunlop
 * @since Jul 31, 2008
 * 
 *        $Id$
 */
public abstract class BaseGrammar implements Grammar, Serializable
{
    /**
     * Maps will automatically expand, but we should default to something reasonable
     */
    public final static int DEFAULT_MAP_SIZE = 8;

    private final static float NIL_PROBABILITY = Float.NEGATIVE_INFINITY;

    /*
     * Probabilities and possible productions are used frequently, so they are stored in array form
     * for fast access. Occurrence counts are stored in ArrayLists, since they need to dynamically
     * resize during grammar induction/construction. They're nice to keep around later for
     * debugging, but we don't need fast access to counts.
     */

    /** Indexed by category, maps production -> log probability */
    private Int2FloatOpenHashMap[] unaryProductionLogProbabilities;

    /** Indexed by category, maps production1 -> Map (production2 -> count) */
    private Int2ObjectOpenHashMap<Int2FloatOpenHashMap>[] binaryProductionLogProbabilities;

    /**
     * Occurrence count. Indexed by category, maps production -> count
     */
    protected final ArrayList<Int2IntOpenHashMap> unaryProductionOccurrences = new ArrayList<Int2IntOpenHashMap>();

    /**
     * Occurrence count. Indexed by category, maps production1 -> Map (production2 -> count)
     */
    protected final ArrayList<Int2ObjectOpenHashMap<Int2IntOpenHashMap>> binaryProductionOccurrences = new ArrayList<Int2ObjectOpenHashMap<Int2IntOpenHashMap>>();

    /** Temporary storage of all categories modeled in the grammar. See categories */
    private IntOpenHashSet tmpCategorySet = new IntOpenHashSet();

    /** All categories modeled in the grammar */
    private int[] categories;

    /** Temporary storage while constructing the grammar */
    private final IntArrayList categoryOccurrences = new IntArrayList();

    /** All productions modeled in the grammar. See productions */
    private IntOpenHashSet tmpProductionSet = new IntOpenHashSet();

    /** All productions modeled in the grammar */
    private int[] productions;

    /**
     * Reverse-lookup map from production1 (unary production or first of a binary production) to the
     * set of categories that can produce it
     */
    private int[][] production1Categories;

    /**
     * All the legal first productions (unary productions or first of a binary production)
     * 
     * TODO: Store only first binary productions separately for parse efficiency?
     */
    private int[] firstProductions;

    /**
     * All the legal second productions of binary productions
     */
    private int[] secondProductions;

    /**
     * Boolean flags indicating whether the indexed production is valid as a unary production
     */
    private BitVector validUnaryProductionsBitmap;

    /**
     * Boolean flags indicating whether the indexed production is valid as the first production in a
     * binary production
     */
    private BitVector validFirstProductionsBitmap;

    /**
     * Boolean flags indicating whether the indexed production is valid as the second production in
     * a binary production
     */
    private BitVector validSecondProductionsBitmap;

    /**
     * All the legal categories for a binary production. Indexed by the production1, maps
     * production2 -> int[] (the set of legal categories which could produce these two productions)
     */
    private Int2ObjectOpenHashMap<int[]>[] binaryProductionCategories;

    /**
     * All the legal categories for a binary production, which are unary descendants of the top
     * category
     */
    private Int2ObjectOpenHashMap<int[]>[] validTopCategories;

    private int[][] validSecondProductions;

    /** The total number of rules modeled in this grammar */
    private int totalRules;

    protected BaseGrammar()
    {
        // Add the special start symbol (S-dagger) as the first category
        addCategory(0);
    }

    protected BaseGrammar(final Reader reader, final boolean verbose) throws IOException
    {
        init(reader, verbose);
    }

    protected void init(final Reader reader, final boolean verbose) throws IOException
    {
        // Count all the production occurrences before calculating probabilities
        countOccurrences(reader, verbose);
        init();
    }

    @SuppressWarnings("unchecked")
    protected void init()
    {
        categories = tmpCategorySet.toIntArray();
        tmpCategorySet = null;
        productions = tmpProductionSet.toIntArray();
        tmpProductionSet = null;
        int arraySize = categoryOccurrences.size();

        validUnaryProductionsBitmap = new BitVector(productions.length);
        validFirstProductionsBitmap = new BitVector(productions.length);
        validSecondProductionsBitmap = new BitVector(productions.length);

        // Calculate and store unary production probabilities
        unaryProductionLogProbabilities = new Int2FloatOpenHashMap[arraySize];
        final IntSet[] tmpValidUnaryProductions = new IntSet[productions.length];

        for (int category = 0; category < unaryProductionOccurrences.size(); category++)
        {
            final Int2IntOpenHashMap map = unaryProductionOccurrences.get(category);
            final int currentCategoryOccurrences = categoryOccurrences.getInt(category);

            unaryProductionLogProbabilities[category] = new Int2FloatOpenHashMap(DEFAULT_MAP_SIZE);
            unaryProductionLogProbabilities[category].defaultReturnValue(NIL_PROBABILITY);
            tmpValidUnaryProductions[category] = new IntOpenHashSet();

            for (final int production : map.keySet())
            {
                validUnaryProductionsBitmap.add(production);
                tmpValidUnaryProductions[category].add(production);
                unaryProductionLogProbabilities[category].put(production, (float) Math.log((float) map.get(production)
                    / currentCategoryOccurrences));
            }
        }

        // Calculate and store binary production probabilities
        binaryProductionLogProbabilities = new Int2ObjectOpenHashMap[arraySize];

        final IntSet[] tmpValidSecondProductions = new IntSet[arraySize];
        for (int production1 = 0; production1 < arraySize; production1++)
        {
            tmpValidSecondProductions[production1] = new IntOpenHashSet();
        }

        for (int category = 0; category < binaryProductionOccurrences.size(); category++)
        {
            final int currentCategoryOccurrences = categoryOccurrences.get(category);
            final Int2ObjectOpenHashMap<Int2IntOpenHashMap> map1 = binaryProductionOccurrences.get(category);

            binaryProductionLogProbabilities[category] = new Int2ObjectOpenHashMap<Int2FloatOpenHashMap>(
                DEFAULT_MAP_SIZE);

            for (int production1 : map1.keySet())
            {
                validFirstProductionsBitmap.add(production1);

                Int2IntOpenHashMap map2 = map1.get(production1);

                Int2FloatOpenHashMap logProbabilityMap = new Int2FloatOpenHashMap(DEFAULT_MAP_SIZE);
                logProbabilityMap.defaultReturnValue(NIL_PROBABILITY);
                binaryProductionLogProbabilities[category].put(production1, logProbabilityMap);

                for (int production2 : map2.keySet())
                {
                    tmpValidSecondProductions[production1].add(production2);

                    validSecondProductionsBitmap.add(production2);

                    logProbabilityMap.put(production2, (float) Math.log((float) map2.get(production2)
                        / currentCategoryOccurrences));
                }
            }
        }

        validSecondProductions = new int[arraySize][];
        for (int production1 = 0; production1 < arraySize; production1++)
        {
            validSecondProductions[production1] = tmpValidSecondProductions[production1].toIntArray();
        }

        // Store all the valid categories in a reverse map by the first production that they can
        // produce.
        final IntOpenHashSet[] tmpProduction1Categories = new IntOpenHashSet[productions.length];
        for (int i = 0; i < productions.length; i++)
        {
            // Keep this set small - most productions aren't produced by a large number of
            // categories.
            tmpProduction1Categories[i] = new IntOpenHashSet(4, Hash.FAST_LOAD_FACTOR);
        }

        for (int category = 0; category < unaryProductionOccurrences.size(); category++)
        {
            for (int production1 : unaryProductionOccurrences.get(category).keySet())
            {
                tmpProduction1Categories[production1].add(category);
            }
        }

        // The valid second productions of binary productions
        final IntOpenHashSet tmpSecondProductions = new IntOpenHashSet();

        for (int category = 0; category < binaryProductionOccurrences.size(); category++)
        {
            Int2ObjectOpenHashMap<Int2IntOpenHashMap> map = binaryProductionOccurrences.get(category);
            for (int production1 : map.keySet())
            {
                tmpProduction1Categories[production1].add(category);
                tmpSecondProductions.addAll(map.get(production1).keySet());
            }
        }

        production1Categories = new int[productions.length][];
        for (int i = 0; i < productions.length; i++)
        {
            production1Categories[i] = tmpProduction1Categories[i].toIntArray();
        }

        secondProductions = tmpSecondProductions.toIntArray();

        int i = 0;
        firstProductions = new int[productions.length];
        for (int production = 0; production < productions.length; production++)
        {
            if (!tmpProduction1Categories[production].isEmpty())
            {
                firstProductions[i++] = production;
            }
        }

        // Store all the categories in a reverse map by the binary productions that they can
        // produce.
        final Int2ObjectOpenHashMap<IntSet>[] tmpCategories = new Int2ObjectOpenHashMap[productions.length];
        final Int2ObjectOpenHashMap<IntSet>[] tmpTopCategories = new Int2ObjectOpenHashMap[productions.length];
        for (i = 0; i < tmpCategories.length; i++)
        {
            tmpCategories[i] = new Int2ObjectOpenHashMap<IntSet>(8, Hash.FAST_LOAD_FACTOR);
            tmpTopCategories[i] = new Int2ObjectOpenHashMap<IntSet>(8, Hash.FAST_LOAD_FACTOR);
        }

        for (int category = 0; category < binaryProductionOccurrences.size(); category++)
        {
            for (int production1 : binaryProductionOccurrences.get(category).keySet())
            {
                for (int production2 : binaryProductionOccurrences.get(category).get(production1).keySet())
                {
                    IntSet producingCategories = tmpCategories[production1].get(production2);
                    if (producingCategories == null)
                    {
                        producingCategories = new IntOpenHashSet();
                        tmpCategories[production1].put(production2, producingCategories);
                    }
                    producingCategories.add(category);

                    // Is this category valid as a unary production of 'TOP' ?
                    if (unaryProductions(0).contains(category))
                    {
                        IntSet topProducingCategories = tmpTopCategories[production1].get(production2);
                        if (topProducingCategories == null)
                        {
                            topProducingCategories = new IntOpenHashSet();
                            tmpTopCategories[production1].put(production2, topProducingCategories);
                        }
                        topProducingCategories.add(category);
                    }
                }
            }
        }

        binaryProductionCategories = new Int2ObjectOpenHashMap[productions.length];
        validTopCategories = new Int2ObjectOpenHashMap[productions.length];
        for (i = 0; i < binaryProductionCategories.length; i++)
        {
            binaryProductionCategories[i] = new Int2ObjectOpenHashMap<int[]>(8, Hash.FAST_LOAD_FACTOR);
            validTopCategories[i] = new Int2ObjectOpenHashMap<int[]>(4, Hash.FAST_LOAD_FACTOR);
        }

        for (int production1 = 0; production1 < tmpCategories.length; production1++)
        {
            for (int production2 : tmpCategories[production1].keySet())
            {
                binaryProductionCategories[production1].put(production2, tmpCategories[production1].get(production2)
                    .toIntArray());
            }
        }

        for (int production1 = 0; production1 < tmpTopCategories.length; production1++)
        {
            for (int production2 : tmpTopCategories[production1].keySet())
            {
                validTopCategories[production1].put(production2, tmpTopCategories[production1].get(production2)
                    .toIntArray());
            }
        }

    }

    /**
     * Counts the occurrences of unary and binary productions, calling incrementCount() methods as
     * appropriate
     */
    protected abstract void countOccurrences(final Reader reader, final boolean verbose) throws IOException;

    @Override
    public final int occurrences(final int category)
    {
        return categoryOccurrences.getInt(category);
    }

    @Override
    public final int occurrences(final int category, final int production)
    {
        return unaryProductionOccurrences.get(category).get(production);
    }

    @Override
    public final int occurrences(final int category, final int production1, final int production2)
    {
        Int2IntOpenHashMap map = binaryProductionOccurrences.get(category).get(production1);
        if (map == null)
        {
            return 0;
        }
        return map.get(production2);
    }

    @Override
    public final float probability(final int category, final int production)
    {
        float f = logProbability(category, production);
        if (f == NIL_PROBABILITY)
        {
            return 0;
        }
        return (float) Math.exp(f);
    }

    @Override
    public final float probability(final int category, final int production1, final int production2)
    {
        float f = logProbability(category, production1, production2);
        if (f == NIL_PROBABILITY)
        {
            return 0;
        }
        return (float) Math.exp(f);
    }

    @Override
    public final float logProbability(final int category, final int production)
    {
        return unaryProductionLogProbabilities[category].get(production);
    }

    @Override
    public final float logProbability(final int category, final int production1, final int production2)
    {
        Int2FloatOpenHashMap map = binaryProductionLogProbabilities[category].get(production1);
        if (map == null)
        {
            return NIL_PROBABILITY;
        }
        return map.get(production2);
    }

    /**
     * Increments the count of the specified unary production.
     * 
     * @param category
     * @param production
     */
    protected final void incrementUnaryOccurrenceCount(final int category, final int production)
    {
        addCategory(category);
        addProduction(production);
        categoryOccurrences.set(category, categoryOccurrences.get(category) + 1);

        int currentCount = unaryProductionOccurrences.get(category).get(production);

        if (currentCount == 0)
        {
            totalRules++;
        }
        unaryProductionOccurrences.get(category).put(production, currentCount + 1);

    }

    /**
     * Increments the count of the specified binary production.
     * 
     * @param category
     * @param production1
     * @param production2
     */
    protected final void incrementBinaryOccurrenceCount(final int category, final int production1, final int production2)
    {
        addCategory(category);
        addProduction(production1);
        addProduction(production2);
        categoryOccurrences.set(category, categoryOccurrences.get(category) + 1);

        final Int2ObjectOpenHashMap<Int2IntOpenHashMap> map1 = binaryProductionOccurrences.get(category);

        // Lazy initialization, since it's a VERY sparse data structure
        Int2IntOpenHashMap map2 = map1.get(production1);
        if (map2 == null)
        {
            map2 = new Int2IntOpenHashMap(DEFAULT_MAP_SIZE);
            map1.put(production1, map2);
        }

        int currentCount = map2.get(production2);

        if (currentCount == 0)
        {
            totalRules++;
        }

        map2.put(production2, currentCount + 1);
    }

    /**
     * Increments the count of the specified unary production.
     * 
     * @param category
     * @param production
     * @param occurrences
     */
    protected final void incrementUnaryOccurrenceCount(final int category, final int production, final int occurrences)
    {
        addCategory(category);
        addProduction(production);
        categoryOccurrences.set(category, categoryOccurrences.get(category) + occurrences);

        final Int2IntOpenHashMap map = unaryProductionOccurrences.get(category);
        final int currentCount = map.get(production);

        if (currentCount == 0)
        {
            totalRules++;
        }
        map.put(production, currentCount + occurrences);

    }

    /**
     * Increments the count of the specified binary production.
     * 
     * @param category
     * @param production1
     * @param production2
     * @param occurrences
     */
    protected final void incrementBinaryOccurrenceCount(final int category, final int production1,
        final int production2, int occurrences)
    {
        addCategory(category);
        addProduction(production1);
        addProduction(production2);
        categoryOccurrences.set(category, categoryOccurrences.get(category) + occurrences);

        Int2IntOpenHashMap map = binaryProductionOccurrences.get(category).get(production1);
        // Lazy initialization, since it's a VERY sparse data structure
        if (map == null)
        {
            map = new Int2IntOpenHashMap(DEFAULT_MAP_SIZE);
            binaryProductionOccurrences.get(category).put(production1, map);
        }

        final int currentCount = map.get(production2);

        if (currentCount == 0)
        {
            totalRules++;
        }

        map.put(production2, currentCount + occurrences);
    }

    private void addCategory(final int category)
    {
        tmpCategorySet.add(category);
        // Categories are productions as well...
        tmpProductionSet.add(category);

        while (categoryOccurrences.size() <= category)
        {
            categoryOccurrences.add(0);
            unaryProductionOccurrences
                .add(new Int2IntOpenHashMap(Hash.DEFAULT_INITIAL_SIZE, Hash.VERY_FAST_LOAD_FACTOR));
            binaryProductionOccurrences.add(new Int2ObjectOpenHashMap<Int2IntOpenHashMap>(Hash.DEFAULT_INITIAL_SIZE,
                Hash.VERY_FAST_LOAD_FACTOR));
        }
    }

    private void addProduction(final int production)
    {
        tmpProductionSet.add(production);
    }

    @Override
    public final int[] categories()
    {
        return categories;
    }

    @Override
    public final int[] productions()
    {
        return productions;
    }

    public final int size()
    {
        return productions.length;
    }

    public final int[] intTokens()
    {
        return productions;
    }

    /**
     * Returns the set of legal unary productions given a category
     * 
     * @param category
     * @return Set of legal unary productions
     */
    protected final IntSet unaryProductions(final int category)
    {
        return unaryProductionOccurrences.get(category).keySet();
    }

    /**
     * Returns the set of legal binary productions given a category
     * 
     * @param category
     * @return Set of legal binary productions
     */
    protected final Set<int[]> binaryProductions(final int category)
    {
        Set<int[]> binaryProductions = new HashSet<int[]>();
        Int2ObjectOpenHashMap<Int2IntOpenHashMap> map = binaryProductionOccurrences.get(category);
        for (int production1 : map.keySet())
        {
            for (int production2 : map.get(production1).keySet())
            {
                binaryProductions.add(new int[] {production1, production2});
            }
        }
        return binaryProductions;
    }

    public final int totalRules()
    {
        return totalRules;
    }

    @Override
    public final int[] firstProductions()
    {
        return firstProductions;
    }

    @Override
    public int[] secondProductions()
    {
        return secondProductions;
    }

    @Override
    public final int[] possibleCategories(final int production1)
    {
        return production1Categories[production1];
    }

    @Override
    public int[] binaryProductionCategories(final int production1, final int production2)
    {
        return binaryProductionCategories[production1].get(production2);
    }

    @Override
    public int[] validTopCategories(int production1, int production2)
    {
        return validTopCategories[production1].get(production2);
    }

    @Override
    public final boolean validUnaryProduction(final int production)
    {
        return validUnaryProductionsBitmap.contains(production);
    }

    @Override
    public final boolean validFirstProduction(final int production)
    {
        return validFirstProductionsBitmap.contains(production);
    }

    @Override
    public final boolean validSecondProduction(final int production)
    {
        return validSecondProductionsBitmap.contains(production);
    }

    @Override
    public final int[] validSecondProductions(final int production1)
    {
        return validSecondProductions[production1];
    }
}

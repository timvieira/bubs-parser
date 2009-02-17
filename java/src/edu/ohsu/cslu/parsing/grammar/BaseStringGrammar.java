package edu.ohsu.cslu.parsing.grammar;

import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

/**
 * Base class for grammars modeling categories and productions as strings.
 * 
 * @author Aaron Dunlop
 * @since Jul 30, 2008
 * 
 *        $Id$
 */
public abstract class BaseStringGrammar extends BaseGrammar implements StringGrammar
{
    // We only need one list, since categories are a subset of productions
    private final ArrayList<String> stringProductions = new ArrayList<String>(2048);
    private final Object2IntLinkedOpenHashMap<String> intProductions = new Object2IntLinkedOpenHashMap<String>(2048);

    protected BaseStringGrammar(String startSymbol)
    {
        intProductions.put(startSymbol, 0);
        stringProductions.add(startSymbol);
    }

    protected BaseStringGrammar(final String startSymbol, final Reader reader, final boolean verbose)
        throws IOException
    {
        this(startSymbol);
        init(reader, verbose);
    }

    @Override
    public String startSymbol()
    {
        return mapProduction(0);
    }

    /**
     * Increments the count of the specified unary production.
     * 
     * @param category
     * @param production
     */
    protected final void incrementUnaryOccurrenceCount(final String category, final String production)
    {
        incrementUnaryOccurrenceCount(mapOrCreateProduction(category), mapOrCreateProduction(production));
    }

    /**
     * Increments the count of the specified binary production.
     * 
     * @param category
     * @param production1
     * @param production2
     */
    protected final void incrementBinaryOccurrenceCount(final String category, final String production1,
        final String production2)
    {
        incrementBinaryOccurrenceCount(mapOrCreateProduction(category), mapOrCreateProduction(production1),
            mapOrCreateProduction(production2));
    }

    /**
     * Increments the count of the specified unary production.
     * 
     * @param category
     * @param production
     * @param occurrences
     */
    protected final void incrementUnaryOccurrenceCount(final String category, final String production,
        final int occurrences)
    {
        incrementUnaryOccurrenceCount(mapOrCreateProduction(category), mapOrCreateProduction(production), occurrences);
    }

    /**
     * Increments the count of the specified binary production.
     * 
     * @param category
     * @param production
     * @param occurrences
     */
    protected final void incrementBinaryOccurrenceCount(final String category, final String production1,
        final String production2, int occurrences)
    {
        incrementBinaryOccurrenceCount(mapOrCreateProduction(category), mapOrCreateProduction(production1),
            mapOrCreateProduction(production2), occurrences);
    }

    /**
     * 'Un-factors' a binary-factored parse tree by removing split pseudo-productions.
     * 
     * TODO: This doesn't really belong in a Grammar implementation. Move somewhere else?
     * 
     * @param tree
     * @return Bracketed string representation of the un-factored tree
     */
    public static String unfactor(String tree)
    {
        // Split the sentence up by tags and parentheses.
        // TODO: Walk the string manually to save regex replaceAll calls?
        tree = tree.replaceAll("\\(", "( ").replaceAll("\\)", " )").replaceAll("\\)\\(", ") (");
        String[] split = tree.split(" +");

        // Work backward through the sentence, pushing each element onto a stack
        final Stack<String> stack = new Stack<String>();
        for (int i = split.length - 1; i >= 0; i--)
        {
            stack.push(split[i]);
        }

        final Stack<String> tmpStack = new Stack<String>();
        final StringBuffer sb = new StringBuffer(tree.length());

        while (!stack.isEmpty())
        {
            String s = stack.pop();
            // Skip factored categories
            if (s.charAt(0) != 'W' && s.indexOf('-') > 0 && !s.startsWith("IN-"))
            {
                // Drop the last '('
                sb.deleteCharAt(sb.length() - 1);
                int nesting = 1;
                while (nesting > 0)
                {
                    s = stack.pop();
                    nesting += s.equals("(") ? 1 : (s.equals(")") ? -1 : 0);
                    tmpStack.push(s);
                }
                // Discard the unneeded right-paren
                tmpStack.pop();

                while (!tmpStack.isEmpty())
                {
                    stack.push(tmpStack.pop());
                }
            }
            else
            {
                sb.append(s);

                if (!(s.equals("(") || stack.isEmpty() || stack.peek().equals(")")))
                {
                    sb.append(' ');
                }
            }
        }
        return sb.toString();
    }

    public static String normalizeINAndPP(final String tree)
    {
        return tree.replaceAll("\\(IN-[a-z]*", "(IN").replaceAll("Win-[a-z]*\\)", "Win)").replaceAll("\\(PP[A-Za-z-]*",
            "\\(PP");
    }

    @Override
    public final float logProbability(final String category, final String production1, final String production2)
    {
        return logProbability(mapProduction(category), mapProduction(production1), mapProduction(production2));
    }

    @Override
    public final float logProbability(final String category, final String production)
    {
        return logProbability(mapProduction(category), mapProduction(production));
    }

    @Override
    public final int occurrences(final String category)
    {
        return occurrences(mapProduction(category));
    }

    @Override
    public final int occurrences(final String category, final String production)
    {
        return occurrences(mapProduction(category), mapProduction(production));
    }

    @Override
    public final int occurrences(final String category, final String production1, final String production2)
    {
        return occurrences(mapProduction(category), mapProduction(production1), mapProduction(production2));
    }

    @Override
    public final float probability(final String category, final String production1, final String production2)
    {
        return probability(mapProduction(category), mapProduction(production1), mapProduction(production2));
    }

    @Override
    public final float probability(final String category, final String production)
    {
        return probability(mapProduction(category), mapProduction(production));
    }

    @Override
    public String[] stringCategories()
    {
        return mapProductions(categories());
    }

    @Override
    public String[] stringProductions()
    {
        return mapProductions(productions());
    }

    @Override
    public final String mapProduction(final int production)
    {
        return stringProductions.get(production);
    }

    @Override
    public final int mapProduction(final String production)
    {
        return intProductions.getInt(production);
    }

    @Override
    public final String map(final int label)
    {
        return mapProduction(label);
    }

    @Override
    public final int map(final String label)
    {
        return mapProduction(label);
    }

    @Override
    public final String[] map(final int[] labels)
    {
        return mapProductions(labels);
    }

    @Override
    public final int[] map(final String[] labels)
    {
        if (labels == null)
        {
            return null;
        }

        int[] productions = new int[labels.length];
        for (int i = 0; i < productions.length; i++)
        {
            productions[i] = mapProduction(labels[i]);
        }
        return productions;
    }

    public final String[] mapProductions(final int[] productionIndices)
    {
        if (productionIndices == null)
        {
            return null;
        }

        String[] productions = new String[productionIndices.length];
        for (int i = 0; i < productions.length; i++)
        {
            productions[i] = mapProduction(productionIndices[i]);
        }
        return productions;
    }

    public final Set<String> mapProductions(final IntSet productionIndices)
    {
        if (productionIndices == null)
        {
            return null;
        }

        Set<String> productions = new HashSet<String>();
        for (int i : productionIndices)
        {
            productions.add(mapProduction(i));
        }
        return productions;
    }

    protected final int mapOrCreateProduction(final String production)
    {
        if (intProductions.containsKey(production))
        {
            return intProductions.getInt(production);
        }

        int index = intProductions.size();
        intProductions.put(production, index);
        stringProductions.add(production);
        return index;
    }

    @Override
    public final String[] firstStringProductions()
    {
        return mapProductions(firstProductions());
    }

    @Override
    public final String[] secondStringProductions()
    {
        return mapProductions(secondProductions());
    }

    @Override
    public final String[] possibleCategories(final String production1)
    {
        return mapProductions(possibleCategories(mapProduction(production1)));
    }

    @Override
    public String[] binaryProductionCategories(final String production1, final String production2)
    {
        return mapProductions(binaryProductionCategories(mapProduction(production1), mapProduction(production2)));
    }

    @Override
    public String[] validTopCategories(String production1, String production2)
    {
        return mapProductions(validTopCategories(mapProduction(production1), mapProduction(production2)));
    }

    @Override
    public final boolean validFirstProduction(final String production)
    {
        return validFirstProduction(mapProduction(production));
    }

    @Override
    public final boolean validSecondProduction(final String production)
    {
        return validSecondProduction(mapProduction(production));
    }

    @Override
    public final boolean validUnaryProduction(final String production)
    {
        return validUnaryProduction(mapProduction(production));
    }

    public final String[] validSecondProductions(final String production1)
    {
        return mapProductions(validSecondProductions(mapProduction(production1)));
    }

    public final static boolean isNaryProduction(final String s)
    {
        return (s.indexOf(' ') >= 0);
    }

    @Override
    public String[] tokens()
    {
        return stringProductions.toArray(new String[0]);
    }

    @Override
    public void write(Writer writer)
    {
        throw new UnsupportedOperationException("Not Supported in BaseStringGrammar");
    }

    @Override
    public String toString()
    {
        return toString(30);
    }

    public String toString(int maxCategories)
    {
        if (categories().length > maxCategories)
        {
            return "Too many categories";
        }

        StringBuilder sb = new StringBuilder(256);
        for (int category = 0; category < categories().length; category++)
        {
            String stringCategory = mapProduction(category);
            sb.append(stringCategory + " : " + occurrences(category) + '\n');

            IntSet unaryProductions = unaryProductions(category);
            for (int production : unaryProductions)
            {
                String stringProduction = mapProduction(production);
                sb.append("  " + stringProduction + " : " + occurrences(category, production) + '\n');
            }

            Set<int[]> binaryProductions = binaryProductions(category);
            for (int[] production : binaryProductions)
            {
                String stringProduction = mapProduction(production[0]) + " " + mapProduction(production[1]);
                sb.append("  " + stringProduction + " : " + occurrences(category, production[0], production[1]) + '\n');
            }
        }

        return sb.toString();
    }
}

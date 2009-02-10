package edu.ohsu.cslu.parsing.grammar;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.util.List;
import java.util.Stack;

import edu.ohsu.cslu.util.Strings;


/**
 * Implements induction of a grammar from a corpus and left-factorization of an existing grammar.
 */
public class InducedGrammar extends BaseStringGrammar implements Serializable
{
    private InducedGrammar(String startSymbol)
    {
        super(startSymbol);
    }

    public InducedGrammar(final String startSymbol, final Reader reader, final boolean verbose) throws IOException
    {
        this(startSymbol);
        init(reader, verbose);
    }

    @Override
    protected void countOccurrences(final Reader reader, final boolean verbose) throws IOException
    {
        final Stack<String> stack = new Stack<String>();
        long startTime = System.currentTimeMillis();
        String s;
        int count = 0;
        for (BufferedReader br = new BufferedReader(reader); (s = br.readLine()) != null;)
        {
            // TODO: This could probably be made more efficient by eliminating the actual stack,
            // walking the string backwards, and keeping track of parenthesis nesting manually, but
            // it's already down to ~4 seconds, which is reasonable for now.

            count++;
            final List<String> splitList = Strings.parseTreeTokens(s);

            // Work backward through the sentence, pushing each element onto a
            // stack and adding rules when we come to an open-paren.
            for (int i = splitList.size() - 1; i >= 0; i--)
            {
                final String currentSplit = splitList.get(i);

                if (currentSplit.charAt(0) == '(')
                {
                    final String[] rule = popUntilCloseParen(stack);
                    if (rule[2] == null)
                    {
                        incrementUnaryOccurrenceCount(rule[0], rule[1]);
                    }
                    else
                    {
                        incrementBinaryOccurrenceCount(rule[0], rule[1], rule[2]);
                    }
                    stack.push(rule[0]);
                }
                else
                {
                    stack.push(currentSplit);
                }
            }
        }
        reader.close();

        if (count > 1 && verbose)
        {
            System.out.println("Read " + count + " lines in " + (System.currentTimeMillis() - startTime) + " ms");
        }
    }

    /**
     * Returns the topmost category and production contained on a stack.
     */
    private final String[] popUntilCloseParen(final Stack<String> stack)
    {
        StringBuilder sb = new StringBuilder();
        final String rule0 = stack.pop();
        final String rule1 = stack.pop();

        String s = stack.pop();
        if (s.charAt(0) != ')')
        {
            while (true)
            {
                sb.append(s);
                s = stack.pop();
                if (s.charAt(0) == ')')
                {
                    break;
                }
                sb.append(' ');
            }
        }

        return new String[] {rule0, rule1, sb.length() > 0 ? sb.toString() : null};
    }

    /**
     * Return a left-factored version of this grammar.
     */
    public InducedGrammar leftFactor()
    {
        InducedGrammar lfig = new InducedGrammar(startSymbol());

        long startTime = System.currentTimeMillis();

        // Add all unary productions as-is
        for (int category = 0; category < unaryProductionOccurrences.size(); category++)
        {
            final String categoryString = mapProduction(category);
            Int2IntOpenHashMap production1Map = unaryProductionOccurrences.get(category);
            for (int production1Index : production1Map.keySet())
            {
                final String production1String = mapProduction(production1Index);
                final int occurrences = production1Map.get(production1Index);
                lfig.incrementUnaryOccurrenceCount(categoryString, production1String, occurrences);
            }
        }

        // Binary productions
        for (int category = 0; category < binaryProductionOccurrences.size(); category++)
        {
            final String categoryString = mapProduction(category);
            final Int2ObjectOpenHashMap<Int2IntOpenHashMap> production1Map = binaryProductionOccurrences.get(category);

            for (final int production1Index : production1Map.keySet())
            {
                final String production1String = mapProduction(production1Index);
                final Int2IntOpenHashMap production2Map = production1Map.get(production1Index);
                for (final int production2Index : production2Map.keySet())
                {
                    final String production2String = mapProduction(production2Index);
                    final int occurrences = production2Map.get(production2Index);

                    // Need to know if this production is n-ary
                    if (isNaryProduction(production2String))
                    {
                        String newCategory = mapProduction(category);

                        final String[] split = (production1String + " " + production2String).split(" ");
                        for (int i = 0; i < split.length - 2; i++)
                        {
                            final String prod2 = newCategory + "-" + split[i];
                            final String prod1 = split[i];
                            lfig.incrementBinaryOccurrenceCount(newCategory, prod1, prod2, occurrences);
                            newCategory = prod2;
                        }
                        lfig.incrementBinaryOccurrenceCount(newCategory, split[split.length - 2],
                            split[split.length - 1], occurrences);

                    }
                    else
                    {
                        // TODO: Either of these should work identically, but they don't...

                        lfig.incrementBinaryOccurrenceCount(categoryString, production1String, production2String,
                            occurrences);
                        // lfig.incrementBinaryOccurrenceCount(category, production1Index,
                        // production2Index, occurrences);
                    }
                }
            }
        }

        lfig.init();

        if (lfig.totalRules() > 100)
        {
            System.out.println("Left factored " + lfig.totalRules() + " rules in "
                + (System.currentTimeMillis() - startTime) + " ms");
        }

        return lfig;
    }
}

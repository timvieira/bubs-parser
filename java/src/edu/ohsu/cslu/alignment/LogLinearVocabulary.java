package edu.ohsu.cslu.alignment;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import edu.ohsu.cslu.common.FeatureClass;
import edu.ohsu.cslu.common.Vocabulary;
import edu.ohsu.cslu.util.Strings;

/**
 * {@link Vocabulary} implementation intended to map all features of interest - e.g. words, POS,
 * other labels (as opposed to {@link SimpleVocabulary}, which generally maps only a single class of
 * feature - one instance for words, another for POS, etc.). {@link LogLinearVocabulary} is
 * generally useful for log-linear modeling in which all features are binary.
 * 
 * In addition to the mappings themselves, {@link LogLinearVocabulary} maintains knowledge of
 * 'categories' of labels. It is important when smoothing a model that we be able to shift
 * probability mass between the POS tokens, for example, without shifting POS probability mass to
 * the head verb label.
 * 
 * Tokens within a category are expected to be mapped sequentially (e.g., words 0-25, POS 26-35,
 * _head_verb 36, _head_of_PP 37, etc.).
 * 
 * @author Aaron Dunlop
 * @since Oct 9, 2008
 * 
 *        $Id$
 */
public class LogLinearVocabulary extends SimpleVocabulary
{
    private final int[] categoryBoundaries;

    private LogLinearVocabulary(String[] tokens, int[] categoryBoundaries)
    {
        super(tokens);
        this.categoryBoundaries = categoryBoundaries;
    }

    /**
     * Induces a vocabulary from bracketed input. The gap symbol defaults to '_-'
     * 
     * @param s Bracketed input. Each bracketed element must consist of one or more tokens, but the
     *            token counts need not all be identical. e.g.
     *            "(The DT start) (dog NN) (ran VB head_verb)"
     * @return induced vocabulary
     */
    public static LogLinearVocabulary induce(final String s)
    {
        try
        {
            return induce(new BufferedReader(new StringReader(s)), FeatureClass.FEATURE_GAP);
        }
        catch (IOException e)
        {
            // We shouldn't ever IOException in a StringReader
            return null;
        }
    }

    /**
     * Induces a single vocabulary from bracketed input. Generally useful for log-linear modeling in
     * which all features are binary.
     * 
     * @param s Bracketed input. Each bracketed element must consist of one or more tokens, but the
     *            token counts need not all be identical. e.g.
     *            "(The DT start) (dog NN) (ran VB head_verb)"
     * @param gapSymbol
     * @return induced vocabulary
     */
    public static LogLinearVocabulary induce(final String s, final String gapSymbol)
    {
        try
        {
            return induce(new BufferedReader(new StringReader(s)), gapSymbol);
        }
        catch (IOException e)
        {
            // We shouldn't ever IOException in a StringReader
            return null;
        }
    }

    /**
     * Induces a single vocabulary from bracketed input. Generally useful for log-linear modeling in
     * which all features are binary.
     * 
     * @param reader Bracketed input. Each bracketed element must consist of one or more tokens, but
     *            the token counts need not all be identical. e.g.
     *            "(The DT start) (dog NN) (ran VB head_verb)"
     * @return induced vocabulary
     * @throws IOException
     */
    public static LogLinearVocabulary induce(final BufferedReader reader) throws IOException
    {
        return induce(reader, FeatureClass.FEATURE_GAP);
    }

    /**
     * Induces a single vocabulary from bracketed input. Generally useful for log-linear modeling in
     * which all features are binary.
     * 
     * @param reader Bracketed input. Each bracketed element must consist of one or more tokens, but
     *            the token counts need not all be identical. e.g.
     *            "(The DT start) (dog NN) (ran VB head_verb)"
     * @param gapSymbol
     * @return induced vocabulary
     * @throws IOException
     */
    public static LogLinearVocabulary induce(final BufferedReader reader, final String gapSymbol) throws IOException
    {
        TreeSet<String> featureList = new TreeSet<String>(new TokenComparator());
        featureList.add(gapSymbol);
        featureList.add(FeatureClass.FEATURE_UNKNOWN);

        for (String line = reader.readLine(); line != null; line = reader.readLine())
        {
            String[] features = line.replaceAll("\\(|\\)", " ").split(" +");

            for (String feature : features)
            {
                if (feature.length() > 0)
                {
                    featureList.add(feature);
                }
            }
        }

        // Step through the sorted list of features, and mark a boundary each time we come to a new
        // {@link FeatureClass}
        IntList categoryBoundaryList = new IntArrayList();
        Iterator<String> iter = featureList.iterator();
        // Skip the gap symbol
        iter.next();
        FeatureClass currentClass = FeatureClass.forString(iter.next());
        int i = 1;

        while (iter.hasNext())
        {
            FeatureClass fc = FeatureClass.forString(iter.next());
            i++;
            // Special-case - Unknown is considered part of the Word feature class
            if (currentClass != fc && !(currentClass == FeatureClass.Unknown && fc == FeatureClass.Word))
            {
                currentClass = fc;
                categoryBoundaryList.add(i);
            }
        }

        LogLinearVocabulary vocabulary = new LogLinearVocabulary(featureList.toArray(new String[featureList.size()]),
            categoryBoundaryList.toIntArray());
        return vocabulary;
    }

    public static LogLinearVocabulary read(final Reader reader) throws IOException
    {
        final BufferedReader br = new BufferedReader(reader);
        Map<String, String> attributes = Strings.headerAttributes(br.readLine());

        final int size = Integer.parseInt(attributes.get("size"));

        final String[] stringCategoryBoundaries = attributes.get("categoryboundaries").split(",");
        final int[] categoryBoundaries = new int[stringCategoryBoundaries.length];
        for (int i = 0; i < categoryBoundaries.length; i++)
        {
            categoryBoundaries[i] = Integer.parseInt(stringCategoryBoundaries[i]);
        }

        final String[] tokens = new String[size];
        for (String line = br.readLine(); line != null; line = br.readLine())
        {
            if (line.length() > 0)
            {
                String[] split = line.split(" +: +");
                int index = Integer.parseInt(split[0]);
                tokens[index] = split[1];
            }
        }
        return new LogLinearVocabulary(tokens, categoryBoundaries);
    }

    @Override
    protected void writeHeader(Writer writer) throws IOException
    {
        writer.write(String.format("vocabulary size=%d categoryboundaries=", size()));
        for (int i = 0; i < categoryBoundaries.length; i++)
        {
            writer.write(Integer.toString(categoryBoundaries[i]));
            if (i < (categoryBoundaries.length - 1))
            {
                writer.write(',');
            }
        }
        writer.write('\n');
    }

    @Override
    public int map(String token)
    {
        final int value = super.map(token);
        if (value == Integer.MIN_VALUE && !token.startsWith("_"))
        {
            return map(FeatureClass.FEATURE_UNKNOWN);
        }
        return value;
    }

    public int[] categoryBoundaries()
    {
        return categoryBoundaries;
    }

    /**
     * Sorts tokens by category. Used when inducing a {@link LogLinearVocabulary}.
     * 
     * Token order:
     * <ol>
     * <li>words (any token which does not start with an underscore)</li>
     * <li>labeled POS (pos_NN, pos_</li>
     * <li>Other known labels (_head_verb, _begin_sent, _initial_cap, _all_caps, etc.)
     * <li>Previous words (_word-n_...)</li>
     * <li>Subsequent words (_word+n_...)</li>
     * <li>Previous POS (_pos-n_...)</li>
     * <li>Subsequent POS (_pos_n_...)</li>
     * <li>Other/Unknown labels (_...)</li>
     * </ol>
     */
    private static class TokenComparator implements Comparator<String>
    {

        @Override
        public int compare(String o1, String o2)
        {
            // First sort by feature classes
            int featureComparison = FeatureClass.forString(o1).compareTo(FeatureClass.forString(o2));
            if (featureComparison != 0)
            {
                return featureComparison;
            }

            // Within feature classes, sort alphabetically
            return o1.compareTo(o2);
        }

    }
}

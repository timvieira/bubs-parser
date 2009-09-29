package edu.ohsu.cslu.alignment;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

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

    private LogLinearVocabulary(final String[] tokens, final int[] categoryBoundaries,
        final HashSet<String> rareFeatures)
    {
        super(tokens, rareFeatures);
        this.categoryBoundaries = categoryBoundaries;
    }

    /**
     * Induces a vocabulary from bracketed input. The gap symbol defaults to '_-'
     * 
     * @param s Bracketed input. Each bracketed element must consist of one or more tokens, but the
     *            token counts need not all be identical. e.g.
     *            "(The DT start) (dog NN) (ran VB head_verb)"
     * @param rareTokenCutoff Greatest occurrence count at which a token will be counted as 'rare'
     * @return induced vocabulary
     */
    public static LogLinearVocabulary induce(final String s, final int rareTokenCutoff)
    {
        try
        {
            return induce(new BufferedReader(new StringReader(s)), FeatureClass.FEATURE_GAP, rareTokenCutoff);
        }
        catch (IOException e)
        {
            // We shouldn't ever IOException in a StringReader
            return null;
        }
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
        return induce(s, 0);
    }

    /**
     * Induces a single vocabulary from bracketed input. Generally useful for log-linear modeling in
     * which all features are binary.
     * 
     * @param s Bracketed input. Each bracketed element must consist of one or more tokens, but the
     *            token counts need not all be identical. e.g.
     *            "(The DT start) (dog NN) (ran VB head_verb)"
     * @param gapSymbol
     * @param rareTokenCutoff Greatest occurrence count at which a token will be counted as 'rare'
     * @return induced vocabulary
     */
    public static LogLinearVocabulary induce(final String s, final String gapSymbol, final int rareTokenCutoff)
    {
        try
        {
            return induce(new BufferedReader(new StringReader(s)), gapSymbol, rareTokenCutoff);
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
        return induce(s, gapSymbol, 0);
    }

    /**
     * Induces a single vocabulary from bracketed input. Generally useful for log-linear modeling in
     * which all features are binary.
     * 
     * @param reader Bracketed input. Each bracketed element must consist of one or more tokens, but
     *            the token counts need not all be identical. e.g.
     *            "(The DT start) (dog NN) (ran VB head_verb)"
     * @param rareTokenCutoff Greatest occurrence count at which a token will be counted as 'rare'
     * @param storeKnownWords Maintain a set of lowercase words mapped by this vocabulary
     * @return induced vocabulary
     * @throws IOException
     */
    public static LogLinearVocabulary induce(final BufferedReader reader, final int rareTokenCutoff) throws IOException
    {
        return induce(reader, FeatureClass.FEATURE_GAP, rareTokenCutoff);
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
        return induce(reader, FeatureClass.FEATURE_GAP, 0);
    }

    /**
     * Induces a single vocabulary from bracketed input. Generally useful for log-linear modeling in
     * which all features are binary.
     * 
     * @param reader Bracketed input. Each bracketed element must consist of one or more tokens, but
     *            the token counts need not all be identical. e.g.
     *            "(The DT start) (dog NN) (ran VB head_verb)"
     * @param gapSymbol
     * @param rareTokenCutoff Greatest occurrence count at which a token will be counted as 'rare'
     * @param storeKnownWords Maintain a set of lowercase words mapped by this vocabulary
     * @return induced vocabulary
     * @throws IOException
     */
    public static LogLinearVocabulary induce(final BufferedReader reader, final String gapSymbol,
        final int rareTokenCutoff) throws IOException
    {
        TreeMap<String, Integer> featureMap = new TreeMap<String, Integer>(new TokenComparator());
        for (int i = 0; i < STATIC_SYMBOLS.length; i++)
        {
            featureMap.put(STATIC_SYMBOLS[i], 0);
        }

        for (String line = reader.readLine(); line != null; line = reader.readLine())
        {
            String[] features = line.replaceAll("\\(|\\)", " ").split(" +");

            for (String feature : features)
            {
                if (feature.length() > 0)
                {
                    Integer count = featureMap.get(feature);
                    if (count == null)
                    {
                        featureMap.put(feature, 1);
                    }
                    else
                    {
                        featureMap.put(feature, count.intValue() + 1);
                    }
                }
            }
        }

        final HashSet<String> rareFeatures = new HashSet<String>();

        // Step through the sorted list of features, and mark a boundary each time we come to a new
        // {@link FeatureClass}
        IntList categoryBoundaryList = new IntArrayList();
        Iterator<String> iter = featureMap.keySet().iterator();

        String[] featureArray = new String[featureMap.size()];

        // Don't create boundaries on Static symbols
        int i;
        for (i = 0; i < STATIC_SYMBOLS.length; i++)
        {
            featureArray[i] = iter.next();
        }
        featureArray[i] = iter.next();
        FeatureClass currentClass = FeatureClass.forString(featureArray[i]);
        i++;

        while (iter.hasNext())
        {
            featureArray[i] = iter.next();
            final FeatureClass fc = FeatureClass.forString(featureArray[i]);
            i++;
            // Special-case - Unknown is considered part of the Word feature class
            if (currentClass != fc)
            {
                currentClass = fc;
                categoryBoundaryList.add(i);
            }
        }

        for (final String feature : featureMap.keySet())
        {
            if (featureMap.get(feature) <= rareTokenCutoff)
            {
                rareFeatures.add(feature);
            }
        }

        LogLinearVocabulary vocabulary = new LogLinearVocabulary(featureArray, categoryBoundaryList.toIntArray(),
            rareFeatures);
        return vocabulary;
    }

    /**
     * TODO: It seems like this code should be shared with SimpleVocabulary
     * 
     * @param reader
     * @return Induced Vocabulary
     * @throws IOException
     */
    public static LogLinearVocabulary read(final Reader reader) throws IOException
    {
        final BufferedReader br = new BufferedReader(reader);
        Map<String, String> attributes = Strings.headerAttributes(br.readLine());
        final HashSet<String> rareFeatures = new HashSet<String>();

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
                if (split[2].equals("true"))
                {
                    rareFeatures.add(split[1]);
                }
            }
        }
        return new LogLinearVocabulary(tokens, categoryBoundaries, rareFeatures);
    }

    @Override
    protected void writeHeader(Writer writer) throws IOException
    {
        writer.write(String.format("vocabulary size=%d categoryboundaries=", size()));
        if (categoryBoundaries.length == 0)
        {
            writer.write(String.format("%d\n", size()));
            return;
        }

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
        if (value == Integer.MIN_VALUE && (!token.startsWith("_") || token.startsWith(FeatureClass.PREFIX_STEM)))
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

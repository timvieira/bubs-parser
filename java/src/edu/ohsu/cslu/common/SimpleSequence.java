package edu.ohsu.cslu.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.regex.Pattern;

import edu.ohsu.cslu.datastructs.vectors.Vector;
import edu.ohsu.cslu.util.Strings;

/**
 * Very simple implementation of {@link Sequence} interface.
 * 
 * @author Aaron Dunlop
 * @since Dec 15, 2008
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class SimpleSequence implements Sequence
{
    public final static String BEGIN_SENTENCE = "<s>";
    public final static String END_SENTENCE = "</s>";

    private final String[][] stringFeatures;

    /**
     * These abbreviations should generally not end sentences. The abbreviation list is taken from a
     * sentence splitter by Qiu Long (qiul@comp.nus.edu.sg), who in turn borrowed and modified a
     * list from mmunoz@uiuc.edu.
     */
    private final static Pattern ABBREVIATIONS_PATTERN = Pattern
        .compile("(a|adj|adm|adv|asst|ave|b|bart|bldg|blvd|brig|bros|c|capt|cmdr|col|comdr|con|cpl|d|dr|"
            + "dr|e|ens|f|g|gen|gov|h|hon|hosp|i|insp|j|k|l|ln|lt|m|mm|mr|mrs|ms|maj|messrs|mlle|mme|"
            + "mr|mrs|ms|msgr|mt|n|no|o|op|ord|p|pfc|ph|prof|pvt|q|r|rep|reps|res|rev|rt|s|sen|"
            + "sens|sfc|sgt|sr|st|supt|surg|t|u|v|w|x|y|z|v|vs)\\.");

    public SimpleSequence(final String[][] features)
    {
        this.stringFeatures = features;
    }

    public SimpleSequence(String sequence)
    {
        if (sequence.charAt(0) == '(')
        {
            stringFeatures = Strings.bracketedTags(sequence);
        }
        else if (sequence.charAt(0) == '[')
        {
            stringFeatures = Strings.squareBracketedTags(sequence);
        }
        else
        {
            stringFeatures = Strings.slashDelimitedTags(sequence);
        }
    }

    public SimpleSequence(Reader reader) throws IOException
    {
        StringBuffer sb = new StringBuffer(8192);
        BufferedReader br = new BufferedReader(reader);
        for (String line = br.readLine(); line != null; line = br.readLine())
        {
            sb.append(line);
            sb.append(' ');
        }
        sb.deleteCharAt(sb.length() - 1);

        if (sb.charAt(0) == '(')
        {
            stringFeatures = Strings.bracketedTags(sb.toString());
        }
        else if (sb.charAt(0) == '[')
        {
            stringFeatures = Strings.squareBracketedTags(sb.toString());
        }
        else
        {
            stringFeatures = Strings.slashDelimitedTags(sb.toString());
        }
    }

    @Override
    public int length()
    {
        return stringFeatures.length;
    }

    @Override
    public int featureCount()
    {
        return stringFeatures[0].length;
    }

    @Override
    public Sequence retainFeatures(int... features)
    {
        String[][] newFeatures = new String[length()][features.length];

        for (int i = 0; i < newFeatures.length; i++)
        {
            for (int j = 0; j < features.length; j++)
            {
                newFeatures[i][j] = stringFeatures[i][features[j]];
            }

        }
        return new SimpleSequence(newFeatures);
    }

    /**
     * Returns a new sequence that is a subsequence of this sequence.
     * 
     * @param beginIndex the begin index, inclusive.
     * @param endIndex the end index, exclusive.
     * @return the specified subsequence.
     * 
     * @throws IndexOutOfBoundsException if <tt>beginIndex</tt> or <tt>endIndex</tt> are negative,
     *             if <tt>endIndex</tt> is greater than <tt>length()</tt>, or if <tt>beginIndex</tt>
     *             is greater than <tt>startIndex</tt>
     */
    public Sequence subSequence(int beginIndex, int endIndex)
    {
        String[][] newFeatures = new String[endIndex - beginIndex][stringFeatures[0].length];
        for (int i = 0; i < newFeatures.length; i++)
        {
            for (int j = 0; j < newFeatures[0].length; j++)
            {
                newFeatures[i][j] = stringFeatures[i + beginIndex][j];
            }

        }
        return new SimpleSequence(newFeatures);
    }

    /**
     * Splits the sequence into sentences, based either on begin and end sentence tags (&lt;s&gt;
     * and &lt;/s&gt;) within the sequence itself or on a very simple set of sentence splitting
     * rules.
     * 
     * @return The current sequence split into sentences
     */
    @Override
    public Sequence[] splitIntoSentences()
    {
        for (int i = 0; i < length(); i++)
        {
            if (stringFeature(i, 0).equals(BEGIN_SENTENCE))
            {
                return splitSentencesByMarkers();
            }
        }
        return splitSentencesByHeuristics();
    }

    private Sequence[] splitSentencesByMarkers()
    {
        ArrayList<Sequence> sentences = new ArrayList<Sequence>();

        int lastSentenceBreak = 0;
        for (int i = 0; i < length(); i++)
        {
            // TODO: This implementation takes the easy way out, and assumes that all sentences will
            // include begin and end tags. Obviously we need a better implementation, which will
            // probably turn out to be a non-trivial problem...
            final String word = stringFeature(i, 0);
            if (word.equals(END_SENTENCE))
            {
                sentences.add(subSequence(lastSentenceBreak, i + 1));
                lastSentenceBreak = i + 1;
            }
        }

        return sentences.toArray(new Sequence[sentences.size()]);
    }

    /**
     * A very simple rule-based sentence splitting implementation. Includes regex patterns based on
     * those from a sentence splitter by Qiu Long (qiul@comp.nus.edu.sg).
     * 
     * @return The current sequence split into sentences
     */
    public Sequence[] splitSentencesByHeuristics()
    {
        ArrayList<Sequence> sentences = new ArrayList<Sequence>();

        // Sentence Head pattern matches capitalized words, quotes, and other punctuation
        Pattern beginSentencePattern = Pattern.compile("[.^()\\[\\]<>A-Z\"']+\\w*");
        Pattern endSentencePattern = Pattern.compile("[.?!]");

        // TODO: Consider quotations - don't end a sentence inside an open quote?

        int lastSentenceBreak = 0;
        final int length = length();
        for (int i = 0; i < length; i++)
        {
            String word = stringFeature(i, 0);
            final String nextWord = (i < length - 1) ? stringFeature(i + 1, 0) : null;

            // Don't end the sentence if the current word doesn't contain the 'end-of-sentence'
            // pattern
            if (!endSentencePattern.matcher(word).find())
            {
                continue;
            }

            // If the word is only a punctuation character, we want to consider the previous word
            // instead.
            if (word.length() == 1 && i > 0)
            {
                word = stringFeature(i - 1, 0) + word;
            }

            // We want to end the current sentence if the next word matches the
            // 'beginning-of-sentence' pattern.
            if (nextWord != null && !beginSentencePattern.matcher(nextWord).matches())
            {
                // Don't end a sentence with an abbreviation
                if (ABBREVIATIONS_PATTERN.matcher(word.toLowerCase()).matches())
                {
                    continue;
                }
            }

            sentences.add(subSequence(lastSentenceBreak, i + 1));
            lastSentenceBreak = i + 1;
        }

        // End the current sentence
        if (lastSentenceBreak < length - 1)
        {
            sentences.add(subSequence(lastSentenceBreak, length - 1));
        }

        return sentences.toArray(new Sequence[sentences.size()]);
    }

    @Override
    public String stringFeature(int index, int featureIndex)
    {
        return stringFeatures[index][featureIndex];
    }

    @Override
    public String[] stringFeatures(int index)
    {
        return stringFeatures[index];
    }

    @Override
    public final String toBracketedString()
    {
        final int length = length();
        final int features = featureCount();

        StringBuilder sb = new StringBuilder(length * 20);

        for (int i = 0; i < length; i++)
        {
            sb.append('(');
            for (int j = 0; j < features - 1; j++)
            {
                sb.append(stringFeatures[i][j]);
                sb.append(' ');
            }
            sb.append(stringFeatures[i][features - 1]);
            sb.append(") ");
        }

        // Delete the final space
        sb.deleteCharAt(sb.length() - 1);

        return sb.toString();
    }

    @Override
    public final String toSlashSeparatedString()
    {
        final int length = length();
        final int features = featureCount();

        StringBuilder sb = new StringBuilder(length * 20);

        for (int i = 0; i < length; i++)
        {
            for (int j = 0; j < features - 1; j++)
            {
                sb.append(stringFeatures[i][j]);
                sb.append('/');
            }
            sb.append(stringFeatures[i][features - 1]);
            sb.append(' ');
        }

        // Delete the final space
        sb.deleteCharAt(sb.length() - 1);

        return sb.toString();
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof Sequence))
        {
            return false;
        }

        return toBracketedString().equals(((Sequence) o).toBracketedString());
    }

    @Override
    public String toString()
    {
        return toBracketedString();
    }

    @Override
    public Sequence insertGaps(int[] gapIndices)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Sequence removeAllGaps()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Vector elementAt(int index)
    {
        throw new UnsupportedOperationException("elementAt not implemented in SimpleSequence");
    }
}

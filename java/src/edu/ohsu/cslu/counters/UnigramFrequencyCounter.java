package edu.ohsu.cslu.counters;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.util.HashSet;

import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;

/**
 * Counts unigram word occurrences in a corpus
 * 
 * @author Aaron Dunlop
 * @since Jun 28, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class UnigramFrequencyCounter implements Serializable
{
    private final static TokenizerFactory tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE;

    private int documents = 0;
    private final Object2IntOpenHashMap<String> totalTermCounts = new Object2IntOpenHashMap<String>();
    private final Object2IntOpenHashMap<String> documentTermCounts = new Object2IntOpenHashMap<String>();

    public UnigramFrequencyCounter()
    {
        totalTermCounts.defaultReturnValue(0);
        documentTermCounts.defaultReturnValue(0);
    }

    public void addDocument(String text)
    {
        final HashSet<String> terms = new HashSet<String>();

        // Split the document into terms using LingPipe
        Tokenizer tokenizer = tokenizerFactory.tokenizer(text.toCharArray(), 0, text.length());

        for (final String term : tokenizer)
        {
            // Increment total term counts and add each term to the 'terms' set. We'll use it to
            // increment document term counts (but only once for each term).
            totalTermCounts.put(term, totalTermCounts.getInt(term) + 1);
            terms.add(term);
        }

        for (final String term : terms)
        {
            documentTermCounts.put(term, documentTermCounts.getInt(term) + 1);
        }

        documents++;
    }

    public void addDocument(Reader reader) throws IOException
    {
        StringBuilder sb = new StringBuilder(16384);
        char[] buf = new char[1024];
        for (int i = reader.read(buf); i >= 0; i = reader.read(buf))
        {
            sb.append(buf, 0, i);
        }
        reader.close();

        addDocument(sb.toString());
    }

    public int documents()
    {
        return documents;
    }

    /**
     * @param term
     * @return The total number of occurrences in the corpus
     */
    public int totalCount(String term)
    {
        return totalTermCounts.getInt(term);
    }

    /**
     * @param term
     * @return The number of documents in which the term occurs
     */
    public int documentCount(String term)
    {
        return documentTermCounts.getInt(term);
    }

    public float documentProbability(String term)
    {
        return ((float) documentCount(term)) / documents;
    }

    /**
     * Returns the log of the inverse document frequency for a term. That is, ln (|corpus| / |
     * documents containing term |).
     * 
     * @param term
     * @return Log of the inverse document frequency
     */
    public double logIdf(String term)
    {
        return Math.log(documents / (1.0 + documentCount(term)));
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(documentTermCounts.size() * 20);
        for (String key : documentTermCounts.keySet())
        {
            sb.append(key);
            sb.append(" | ");
            sb.append(totalTermCounts.getInt(key));
            sb.append(" | ");
            sb.append(documentTermCounts.getInt(key));
            sb.append('\n');
        }
        return sb.toString();
    }
}

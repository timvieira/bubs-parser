package edu.ohsu.cslu.counters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * Counts occurrences within a sentence without regard to word order.
 * 
 * @author Aaron Dunlop
 * @since Aug 19, 2008
 * 
 *        $Id$
 */
public class SententialCoocurrenceCounter extends CoocurrenceCounter
{
    public SententialCoocurrenceCounter(Reader reader) throws IOException
    {
        BufferedReader br = new BufferedReader(reader);
        for (String line = br.readLine(); line != null; line = br.readLine())
        {
            countSentence(line.split(" "));
        }
        br.close();

        trim();
    }

    protected void countSentence(String[] wordArray)
    {
        TreeSet<String> words = new TreeSet<String>(Arrays.asList(wordArray));

        for (final String h : words)
        {
            incrementCount(h);

            Iterator<String> i = words.iterator();
            while (!i.next().equals(h))
            {}

            while (i.hasNext())
            {
                final String w = i.next();
                incrementCount(h, w);
            }
        }
    }
}

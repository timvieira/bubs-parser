package edu.ohsu.cslu.counters;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Log likelihood and log odds calculator. Not fully tuned for efficiency, but it's usable
 * 
 * TODO: More documentation, performance tuning.
 * 
 * @author Aaron Dunlop
 * @since Aug 19, 2008
 * 
 *        $Id$
 */
public abstract class CoocurrenceCounter implements Serializable {

    private final Object2IntOpenHashMap<String> singleWordCounts = new Object2IntOpenHashMap<String>();
    private final HashMap<String, Object2IntOpenHashMap<String>> twoWordCounts = new HashMap<String, Object2IntOpenHashMap<String>>();

    /** Total Word Count */
    private int N;

    protected CoocurrenceCounter() {
    }

    protected void trim() {
        singleWordCounts.trim();
        for (Object2IntOpenHashMap<String> map : twoWordCounts.values()) {
            map.trim();
        }
    }

    /**
     * Increments unigram word count for the specified word.
     * 
     * @param w
     *            word
     */
    protected final void incrementCount(final String w) {
        singleWordCounts.put(w, singleWordCounts.getInt(w) + 1);
        N++;
    }

    /**
     * Increments bigram word count for the specified words.
     * 
     * @param h
     *            'history' word
     * @param w
     *            word
     */
    protected final void incrementCount(final String h, final String w) {
        Object2IntOpenHashMap<String> map = twoWordCounts.get(h);
        if (map == null) {
            map = new Object2IntOpenHashMap<String>();
            twoWordCounts.put(h, map);
        }

        map.put(w, map.getInt(w) + 1);
    }

    /**
     * Returns the count of the specified word.
     * 
     * @param w
     *            word
     * @return Number of occurrences of the specified word in the corpus.
     */
    public final int count(String w) {
        return singleWordCounts.getInt(w);
    }

    /**
     * Returns the count of the specified history / word pair.
     * 
     * @param h
     *            'history' word
     * @param w
     *            word
     * @return Number of occurrences of the specified bigram / pair in the corpus.
     */
    public final int count(String h, String w) {
        final Object2IntOpenHashMap<String> map = twoWordCounts.get(h);
        if (map == null) {
            return 0;
        }
        return map.getInt(w);
    }

    /**
     * Returns the log likelihood (G-squared) of a bigram, or -2 * log(lambda).
     * 
     * @param h
     *            'history' word
     * @param w
     *            word
     * @return G-squared statistic for the word pair under consideration. G-squared is always positive; large
     *         values indicate strong dependence between w and h.
     */
    public final float logLikelihoodRatio(String h, String w) {
        final int n = count(h);
        final int c = count(w);
        final int k = count(h, w);

        if (k == 0) {
            // TODO: Is this the right thing to return if we couldn't find a coocurrence?
            return Float.NaN;
        }

        double logLambda = n * Math.log(n);

        if (n != N) {
            logLambda = logLambda + (N - n) * Math.log(N - n);
        }

        logLambda += c * Math.log(c);

        if (c != N) {
            logLambda = logLambda + (N - c) * Math.log(N - c);
        }

        logLambda = logLambda - N * Math.log(N) - k * Math.log(k);

        if (n != k) {
            logLambda = logLambda - (n - k) * Math.log(n - k);
        }

        if (c != k) {
            logLambda = logLambda - (c - k) * Math.log(c - k);
        }

        logLambda = logLambda - (N - n - c + k) * Math.log(N - n - c + k);

        return (float) (-2 * logLambda);
    }

    public float logOddsRatio(String h, String w) {
        final int n = count(h);
        final int c = count(w);
        final int k = count(h, w);

        return (float) (Math.log(k) + Math.log(N - n - c - k) - Math.log(n - k) - Math.log(c - k));
    }
}

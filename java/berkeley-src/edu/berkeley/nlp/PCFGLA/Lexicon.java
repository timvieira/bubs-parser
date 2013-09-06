package edu.berkeley.nlp.PCFGLA;

import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.berkeley.nlp.PCFGLA.GrammarMerger.MergeCandidate;
import edu.berkeley.nlp.PCFGLA.smoothing.Smoother;
import edu.berkeley.nlp.syntax.StateSet;
import edu.berkeley.nlp.syntax.Tree;
import edu.berkeley.nlp.util.Numberer;
import edu.ohsu.cslu.util.IEEEDoubleScaling;

/**
 * Simple default implementation of a lexicon, which scores word, tag pairs with a smoothed estimate of
 * P(tag|word)/P(tag).
 * 
 * for simplicity the lexicon will store words and tags as strings, while the grammar will be using integers ->
 * Numberer()
 */
public class Lexicon implements java.io.Serializable {

    private static final long serialVersionUID = 2L;

    /**
     * Fractional occurrence counts for each observed token/tag combination. Indexed by state. {@link HashMap}s map
     * token to an array of counts for each substate.
     */
    final HashMap<String, double[]>[] observedTokenFractionalCounts;

    /**
     * Fractional occurrence counts for UNK-class/tag combination. Indexed by state. {@link HashMap}s map token to an
     * array of counts for each substate.
     */
    final HashMap<String, double[]>[] unkFractionalCounts;

    double totalTokens = 0.0;
    double totalUnseenTokens = 0.0;
    double totalWords = 0.0;

    /** Counts of tag (state + subState) occurrences. Indexed by state and substate */
    double[][] tagCounter;
    double[][] unseenTagCounter;

    /** The count of how often each word as been seen */
    Object2DoubleOpenHashMap<String> wordCounter = new Object2DoubleOpenHashMap<String>();

    /** The number of substates for each state */
    short[] numSubStates;

    // /** Word-tag pairs that occur less are smoothed. */
    // int smoothingCutoff;

    private Smoother smoother;
    final double threshold;

    /**
     * Cache unknown-word signatures - we'll always return the same signature for a word; the signature for a word
     * differs in sentence-initial position, so we maintain 2 caches
     */
    protected transient HashMap<String, String> cachedSignatures = new HashMap<String, String>();
    protected transient HashMap<String, String> cachedSentenceInitialSignatures = new HashMap<String, String>();
    // TODO We can probably remove this option - it's never changed
    private int unknownLevel = 5; // different modes for unknown words, 5 is
                                  // english specific

    // /**
    // * Unique word count threshold for open-class preterminals. If a preterminal (POS) is observed with more than
    // * {@link #openClassTypesThreshold} unique words, it will be considered open-class. Unknown words will be assgined
    // * to open-class preterminals.
    // */
    // public static int openClassTypesThreshold = 50;

    // TODO Does this threshold ever vary?
    double smoothInUnknownsThreshold = 100;

    final double[] smoothingParams;

    /**
     * Create a blank Lexicon object. Fill it by calling tallyStateSetTree for each training tree, then calling
     * optimize().
     * 
     * @param numSubStates
     * @param smoothParam
     * @param smoother
     * @param learnUnknownWordRules If true, the lexicon will accumulate pseudo-counts for unknown word signatures,
     *            based on observed counts of rare words. If the training corpus incorporates rare-word signatures
     *            directly, these counts are unnecessary.
     * @param threshold
     */
    @SuppressWarnings("unchecked")
    public Lexicon(final short[] numSubStates, final double[] smoothParam, final Smoother smoother,
            final boolean learnUnknownWordRules, final double threshold) {
        this.numSubStates = numSubStates;
        this.smoothingParams = smoothParam;
        this.smoother = smoother;
        this.observedTokenFractionalCounts = new HashMap[numSubStates.length];

        if (learnUnknownWordRules) {
            this.unkFractionalCounts = new HashMap[numSubStates.length];
        } else {
            this.unkFractionalCounts = null;
        }

        this.tagCounter = new double[numSubStates.length][];
        this.unseenTagCounter = new double[numSubStates.length][];

        for (int i = 0; i < numSubStates.length; i++) {
            tagCounter[i] = new double[numSubStates[i]];
            unseenTagCounter[i] = new double[numSubStates[i]];
        }
        this.threshold = threshold;
    }

    /**
     * Split all substates in two, producing a new lexicon. The new Lexicon gives the same scores to words under both
     * split versions of the tag. (Leon says: It may not be okay to use the same scores, but I think that symmetry is
     * sufficiently broken in Grammar.splitAllStates to ignore the randomness here.)
     * 
     * @param counts
     * @param moreSubstatesThanCounts
     * @return A new lexicon, with all states split in 2
     */
    public Lexicon splitAllStates(final int[] counts, final boolean moreSubstatesThanCounts) {

        final short[] newNumSubStates = new short[numSubStates.length];
        newNumSubStates[0] = 1; // never split ROOT
        for (short i = 1; i < numSubStates.length; i++) {
            newNumSubStates[i] = (short) (numSubStates[i] * 2);
        }

        final Lexicon newLexicon = new Lexicon(newNumSubStates, smoothingParams, smoother,
                this.unkFractionalCounts != null, this.threshold);

        // copy and alter all data structures
        for (int tag = 0; tag < observedTokenFractionalCounts.length; tag++) {
            if (observedTokenFractionalCounts[tag] != null) {
                newLexicon.observedTokenFractionalCounts[tag] = new HashMap<String, double[]>();
                for (final String word : observedTokenFractionalCounts[tag].keySet()) {
                    newLexicon.observedTokenFractionalCounts[tag].put(word, new double[newNumSubStates[tag]]);
                    for (int substate = 0; substate < observedTokenFractionalCounts[tag].get(word).length; substate++) {
                        int splitFactor = 2;
                        if (newNumSubStates[tag] == numSubStates[tag]) {
                            splitFactor = 1;
                        }
                        for (int i = 0; i < splitFactor; i++) {
                            newLexicon.observedTokenFractionalCounts[tag].get(word)[substate * splitFactor + i] = (1.f / splitFactor)
                                    * observedTokenFractionalCounts[tag].get(word)[substate];
                        }
                    }
                }
            }
        }

        // Split unknown word
        if (unkFractionalCounts != null) {
            for (int tag = 0; tag < unkFractionalCounts.length; tag++) {
                if (unkFractionalCounts[tag] != null) {
                    newLexicon.unkFractionalCounts[tag] = new HashMap<String, double[]>();
                    for (final String word : unkFractionalCounts[tag].keySet()) {
                        newLexicon.unkFractionalCounts[tag].put(word, new double[newNumSubStates[tag]]);
                        for (int substate = 0; substate < unkFractionalCounts[tag].get(word).length; substate++) {
                            int splitFactor = 2;
                            if (newNumSubStates[tag] == numSubStates[tag]) {
                                splitFactor = 1;
                            }
                            for (int i = 0; i < splitFactor; i++) {
                                newLexicon.unkFractionalCounts[tag].get(word)[substate * splitFactor + i] = (1.f / splitFactor)
                                        * unkFractionalCounts[tag].get(word)[substate];
                            }
                        }
                    }
                }
            }
        }

        newLexicon.totalTokens = totalTokens;
        newLexicon.totalUnseenTokens = totalUnseenTokens;
        newLexicon.totalWords = totalWords;
        newLexicon.smoother = smoother;
        newLexicon.tagCounter = new double[tagCounter.length][];
        newLexicon.unseenTagCounter = new double[unseenTagCounter.length][];

        for (int tag = 0; tag < tagCounter.length; tag++) {
            newLexicon.tagCounter[tag] = new double[newNumSubStates[tag]];
            newLexicon.unseenTagCounter[tag] = new double[newNumSubStates[tag]];

            for (int substate = 0; substate < tagCounter[tag].length; substate++) {
                int splitFactor = 2;
                if (newNumSubStates[tag] == numSubStates[tag]) {
                    splitFactor = 1;
                }

                for (int i = 0; i < splitFactor; i++) {
                    // lexicon.typeTagCounter[tag][substate * splitFactor + i] = (1.f / splitFactor)
                    // * typeTagCounter[tag][substate];
                    newLexicon.tagCounter[tag][substate * splitFactor + i] = (1.f / splitFactor)
                            * tagCounter[tag][substate];
                    newLexicon.unseenTagCounter[tag][substate * splitFactor + i] = (1.f / splitFactor)
                            * unseenTagCounter[tag][substate];
                }
            }
        }

        newLexicon.wordCounter = new Object2DoubleOpenHashMap<String>();

        for (final String word : wordCounter.keySet()) {
            newLexicon.wordCounter.put(word, wordCounter.getDouble(word));
        }

        newLexicon.smoothInUnknownsThreshold = smoothInUnknownsThreshold;

        return newLexicon;
    }

    /**
     * This routine returns a String that is the "signature" of the class of a word. For, example, it might represent
     * whether it is a number of ends in -s. The strings returned by convention match the pattern UNK-.* , which is just
     * assumed to not match any real word. Behavior depends on the unknownLevel (-uwm flag) passed in to the class. The
     * recognized numbers are 1-5: 5 is fairly English-specific; 4, 3, and 2 look for various word features (digits,
     * dashes, etc.) which are only vaguely English-specific; 1 uses the last two characters combined with a simple
     * classification by capitalization.
     * 
     * @param word The word to make a signature for
     * @param sentenceInitial Sentence-initial capitalized words are treated differently
     * @return A String that is its signature (equivalence class)
     */
    public String getSignature(final String word, final boolean sentenceInitial) {
        // int unknownLevel = Options.get().useUnknownWordSignatures;
        final StringBuilder sb = new StringBuilder(12);
        sb.append("UNK");

        if (word.length() == 0) {
            return sb.toString();
        }

        switch (unknownLevel) {

        case 5: {
            // Reformed Mar 2004 (cdm); hopefully much better now.
            // { -CAPS, -INITC ap, -LC lowercase, 0 } +
            // { -KNOWNLC, 0 } + [only for INITC]
            // { -NUM, 0 } +
            // { -DASH, 0 } +
            // { -last lowered char(s) if known discriminating suffix, 0}
            final int wlen = word.length();
            int numCaps = 0;
            boolean hasDigit = false;
            boolean hasDash = false;
            boolean hasLower = false;
            for (int i = 0; i < wlen; i++) {
                final char ch = word.charAt(i);
                if (Character.isDigit(ch)) {
                    hasDigit = true;
                } else if (ch == '-') {
                    hasDash = true;
                } else if (Character.isLetter(ch)) {
                    if (Character.isLowerCase(ch)) {
                        hasLower = true;
                    } else if (Character.isTitleCase(ch)) {
                        hasLower = true;
                        numCaps++;
                    } else {
                        numCaps++;
                    }
                }
            }
            final char ch0 = word.charAt(0);
            final String lowered = word.toLowerCase();
            if (Character.isUpperCase(ch0) || Character.isTitleCase(ch0)) {
                if (sentenceInitial && numCaps == 1) {
                    sb.append("-INITC");
                    if (wordCounter.keySet().contains(lowered)) {
                        sb.append("-KNOWNLC");
                    }
                } else {
                    sb.append("-CAPS");
                }
            } else if (!Character.isLetter(ch0) && numCaps > 0) {
                sb.append("-CAPS");
            } else if (hasLower) { // (Character.isLowerCase(ch0)) {
                sb.append("-LC");
            }
            if (hasDigit) {
                sb.append("-NUM");
            }
            if (hasDash) {
                sb.append("-DASH");
            }
            if (lowered.endsWith("s") && wlen >= 3) {
                // here length 3, so you don't miss out on ones like 80s
                final char ch2 = lowered.charAt(wlen - 2);
                // not -ess suffixes or greek/latin -us, -is
                if (ch2 != 's' && ch2 != 'i' && ch2 != 'u') {
                    sb.append("-s");
                }
            } else if (word.length() >= 5 && !hasDash && !(hasDigit && numCaps > 0)) {
                // don't do for very short words;
                // Implement common discriminating suffixes
                /*
                 * if (Corpus.myLanguage==Corpus.GERMAN){ sb.append(lowered.substring(lowered.length()-1)); }else{
                 */
                if (lowered.endsWith("ed")) {
                    sb.append("-ed");
                } else if (lowered.endsWith("ing")) {
                    sb.append("-ing");
                } else if (lowered.endsWith("ion")) {
                    sb.append("-ion");
                } else if (lowered.endsWith("er")) {
                    sb.append("-er");
                } else if (lowered.endsWith("est")) {
                    sb.append("-est");
                } else if (lowered.endsWith("ly")) {
                    sb.append("-ly");
                } else if (lowered.endsWith("ity")) {
                    sb.append("-ity");
                } else if (lowered.endsWith("y")) {
                    sb.append("-y");
                } else if (lowered.endsWith("al")) {
                    sb.append("-al");
                    // } else if (lowered.endsWith("ble")) {
                    // sb.append("-ble");
                    // } else if (lowered.endsWith("e")) {
                    // sb.append("-e");
                }
            }
            break;
        }

        case 4: {
            boolean hasDigit = false;
            boolean hasNonDigit = false;
            boolean hasLetter = false;
            boolean hasLower = false;
            boolean hasDash = false;
            boolean hasPeriod = false;
            boolean hasComma = false;
            for (int i = 0; i < word.length(); i++) {
                final char ch = word.charAt(i);
                if (Character.isDigit(ch)) {
                    hasDigit = true;
                } else {
                    hasNonDigit = true;
                    if (Character.isLetter(ch)) {
                        hasLetter = true;
                        if (Character.isLowerCase(ch) || Character.isTitleCase(ch)) {
                            hasLower = true;
                        }
                    } else {
                        if (ch == '-') {
                            hasDash = true;
                        } else if (ch == '.') {
                            hasPeriod = true;
                        } else if (ch == ',') {
                            hasComma = true;
                        }
                    }
                }
            }
            // 6 way on letters
            if (Character.isUpperCase(word.charAt(0)) || Character.isTitleCase(word.charAt(0))) {
                if (!hasLower) {
                    sb.append("-AC");
                } else if (sentenceInitial) {
                    sb.append("-SC");
                } else {
                    sb.append("-C");
                }
            } else if (hasLower) {
                sb.append("-L");
            } else if (hasLetter) {
                sb.append("-U");
            } else {
                // no letter
                sb.append("-S");
            }
            // 3 way on number
            if (hasDigit && !hasNonDigit) {
                sb.append("-N");
            } else if (hasDigit) {
                sb.append("-n");
            }
            // binary on period, dash, comma
            if (hasDash) {
                sb.append("-H");
            }
            if (hasPeriod) {
                sb.append("-P");
            }
            if (hasComma) {
                sb.append("-C");
            }
            if (word.length() > 3) {
                // don't do for very short words: "yes" isn't an "-es" word
                // try doing to lower for further densening and skipping digits
                final char ch = word.charAt(word.length() - 1);
                if (Character.isLetter(ch)) {
                    sb.append("-");
                    sb.append(Character.toLowerCase(ch));
                }
            }
            break;
        }

        case 3: {
            // This basically works right, except note that 'S' is applied to
            // all
            // capitalized letters in first word of sentence, not just first....
            sb.append("-");
            char lastClass = '-'; // i.e., nothing
            char newClass;
            int num = 0;
            for (int i = 0; i < word.length(); i++) {
                final char ch = word.charAt(i);
                if (Character.isUpperCase(ch) || Character.isTitleCase(ch)) {
                    if (sentenceInitial) {
                        newClass = 'S';
                    } else {
                        newClass = 'L';
                    }
                } else if (Character.isLetter(ch)) {
                    newClass = 'l';
                } else if (Character.isDigit(ch)) {
                    newClass = 'd';
                } else if (ch == '-') {
                    newClass = 'h';
                } else if (ch == '.') {
                    newClass = 'p';
                } else {
                    newClass = 's';
                }
                if (newClass != lastClass) {
                    lastClass = newClass;
                    sb.append(lastClass);
                    num = 1;
                } else {
                    if (num < 2) {
                        sb.append('+');
                    }
                    num++;
                }
            }
            if (word.length() > 3) {
                // don't do for very short words: "yes" isn't an "-es" word
                // try doing to lower for further densening and skipping digits
                final char ch = Character.toLowerCase(word.charAt(word.length() - 1));
                sb.append('-');
                sb.append(ch);
            }
            break;
        }

        case 2: {
            // {-ALLC, -INIT, -UC, -LC, zero} +
            // {-DASH, zero} +
            // {-NUM, -DIG, zero} +
            // {lowerLastChar, zeroIfShort}
            boolean hasDigit = false;
            boolean hasNonDigit = false;
            boolean hasLower = false;
            for (int i = 0; i < word.length(); i++) {
                final char ch = word.charAt(i);
                if (Character.isDigit(ch)) {
                    hasDigit = true;
                } else {
                    hasNonDigit = true;
                    if (Character.isLetter(ch)) {
                        if (Character.isLowerCase(ch) || Character.isTitleCase(ch)) {
                            hasLower = true;
                        }
                    }
                }
            }
            if (Character.isUpperCase(word.charAt(0)) || Character.isTitleCase(word.charAt(0))) {
                if (!hasLower) {
                    sb.append("-ALLC");
                } else if (sentenceInitial) {
                    sb.append("-INIT");
                } else {
                    sb.append("-UC");
                }
            } else if (hasLower) { // if (Character.isLowerCase(word.charAt(0)))
                                   // {
                sb.append("-LC");
            }
            // no suffix = no (lowercase) letters
            if (word.indexOf('-') >= 0) {
                sb.append("-DASH");
            }
            if (hasDigit) {
                if (!hasNonDigit) {
                    sb.append("-NUM");
                } else {
                    sb.append("-DIG");
                }
            } else if (word.length() > 3) {
                // don't do for very short words: "yes" isn't an "-es" word
                // try doing to lower for further densening and skipping digits
                final char ch = word.charAt(word.length() - 1);
                sb.append(Character.toLowerCase(ch));
            }
            // no suffix = short non-number, non-alphabetic
            break;
        }

        default:
            sb.append("-");
            sb.append(word.substring(Math.max(word.length() - 2, 0), word.length()));
            sb.append("-");
            if (Character.isLowerCase(word.charAt(0))) {
                sb.append("LOWER");
            } else {
                if (Character.isUpperCase(word.charAt(0))) {
                    if (sentenceInitial) {
                        sb.append("INIT");
                    } else {
                        sb.append("UPPER");
                    }
                } else {
                    sb.append("OTHER");
                }
            }
        } // end switch (unknownLevel)
          // System.err.println("Summarized " + word + " to " +
          // sb.toString());
        return sb.toString();
    } // end getSignature()

    public double[] score(final StateSet stateSet, final short tag, final boolean noSmoothing, final boolean isSignature) {
        return score(stateSet.getWord(), tag, stateSet.from, noSmoothing, isSignature);
    }

    /**
     * Get the score of this word with this tag (as an IntTaggedWord) at this loc. (Presumably an estimate of P(word |
     * tag).)
     * <p>
     * <i>Implementation documentation:</i> Seen: c_W = count(W) c_TW = count(T,W) c_T = count(T) c_Tunseen = count(T)
     * among new words in 2nd half total = count(seen words) totalUnseen = count("unseen" words) p_T_U =
     * Pmle(T|"unseen") pb_T_W = P(T|W). If (c_W > smoothInUnknownsThreshold) = c_TW/c_W Else (if not smart mutation)
     * pb_T_W = bayes prior smooth[1] with p_T_U p_T= Pmle(T) p_W = Pmle(W) pb_W_T = pb_T_W * p_W / p_T [Bayes rule]
     * Note that this doesn't really properly reserve mass to unknowns.
     * 
     * Unseen: c_TS = count(T,Sig|Unseen) c_S = count(Sig) c_T = count(T|Unseen) c_U = totalUnseen above p_T_U =
     * Pmle(T|Unseen) pb_T_S = Bayes smooth of Pmle(T|S) with P(T|Unseen) [smooth[0]] pb_W_T = P(W|T) inverted
     * 
     * @param loc The position in the sentence. <i>In the default implementation this is used only for unknown words to
     *            change their probability distribution when sentence initial
     * @return A double valued score, usually P(word|tag)
     */
    public double[] score(final String word, final short tag, final int loc, final boolean noSmoothing,
            final boolean isSignature) {

        final double c_W = wordCounter.getDouble(word);
        final boolean seen = (c_W > 0.0);

        final double[] tagStateCounter = tagCounter[tag];
        final double[] unseenTagStateCounter = unseenTagCounter[tag];
        final HashMap<String, double[]> wordTagCounter = observedTokenFractionalCounts[tag];

        double[] pb_W_T;
        if (!isSignature && (seen || noSmoothing)) {
            pb_W_T = scoreObservedWord(word, tag, noSmoothing, c_W, tagStateCounter, unseenTagStateCounter,
                    wordTagCounter);
        } else {
            pb_W_T = scoreUnobservedWord(word, tag, loc, isSignature);
        }

        // give very low scores when needed, but try to avoid -Infinity
        // NOT sure whether this is a good idea - slav
        for (int i = 0; i < pb_W_T.length; i++) {
            if (pb_W_T[i] == 0) {
                pb_W_T[i] = 1e-87;
            }

        }
        smoother.smooth(tag, pb_W_T);

        return pb_W_T;
    }

    private double[] scoreObservedWord(final String word, final short tag, final boolean noSmoothing, final double c_W,
            final double[] tagStateCounter, final double[] unseenTagStateCounter,
            final HashMap<String, double[]> wordTagCounter) {

        final double[] pb_W_T = new double[numSubStates[tag]];
        for (int substate = 0; substate < pb_W_T.length; substate++) {
            // known word model for P(T|W)
            final double c_tag = tagStateCounter[substate];
            final double c_T = c_tag;// seenCounter.getCount(iTW);
            if (c_T == 0)
                continue;

            double c_TW = 0;
            if (wordTagCounter != null) {
                final double[] c = wordTagCounter.get(word);
                if (c != null) {
                    c_TW = c[substate];
                }
            }

            final double c_Tunseen = unseenTagStateCounter[substate];
            final double totalUnseen = totalUnseenTokens;

            final double p_T_U = (totalUnseen == 0) ? 1 : c_Tunseen / totalUnseen;
            double pb_T_W;

            if (c_W > smoothInUnknownsThreshold || noSmoothing) {
                // we've seen the word enough times to have confidence in its tagging
                if (noSmoothing && c_W == 0) {
                    pb_T_W = c_TW / 1;
                } else {
                    pb_T_W = (c_TW + 0.0001 * p_T_U) / (c_W + 0.0001);
                }
            } else {
                // we haven't seen the word enough times to have confidence in its tagging
                pb_T_W = (c_TW + smoothingParams[1] * p_T_U) / (c_W + smoothingParams[1]);
            }

            // Sometimes we run up against unknown tags. This should only happen when we're calculating the
            // likelihood for a given tree, not when we're parsing. In that case, return a LL of 0.

            final double p_T = (c_T / totalTokens);
            final double p_W = (c_W / totalTokens);
            pb_W_T[substate] = pb_T_W * p_W / p_T;

            // give very low scores when needed, but try to avoid -Infinity
            if (pb_W_T[substate] == 0) {// NOT sure whether this is a good idea - slav
                pb_W_T[substate] = 1e-87;
            }
        }
        return pb_W_T;
    }

    private double[] scoreUnobservedWord(final String word, final short tag, final int loc, final boolean isSignature) {
        double pb_W_T;
        final double[] resultArray = new double[numSubStates[tag]];
        for (int substate = 0; substate < resultArray.length; substate++) {

            // unknown word model for P(T|S)
            final String sig = (isSignature) ? word : getCachedSignature(word, loc);

            // iTW.word = sig;
            // double c_TS = unSeenCounter.getCount(iTW);
            double c_TS = 0;
            if (unkFractionalCounts[tag] != null && unkFractionalCounts[tag].get(sig) != null) {
                c_TS = unkFractionalCounts[tag].get(sig)[substate];
            }
            // if (c_TS == 0) continue;

            // how often did we see this signature
            double c_S = wordCounter.getDouble(sig);
            final double c_U = totalUnseenTokens;
            final double total = totalTokens; // seenCounter.getCount(iTW);
            final double c_T = unseenTagCounter[tag][substate];// unSeenCounter.getCount(iTW);
            final double c_Tseen = tagCounter[tag][substate]; // seenCounter.getCount(iTW);
            final double p_T_U = c_T / c_U;

            if (unknownLevel == 0) {
                c_TS = 0;
                c_S = 0;
            }
            // System.out.println(" sig " + sig
            // +" c_TS "+c_TS+" p_T_U "+p_T_U+" c_S "+c_S);
            // smooth[0]=10;
            final double pb_T_S = (c_TS + smoothingParams[0] * p_T_U) / (c_S + smoothingParams[0]);

            final double p_T = (c_Tseen / total);
            final double p_W = 1.0 / total;
            pb_W_T = pb_T_S * p_W / p_T;

            resultArray[substate] = pb_W_T;
        }
        return resultArray;
    }

    public void tieRareWordStats(final int rareWordThreshold) {
        // ni = unsplit non-terminal index
        for (int ni = 0; ni < numSubStates.length; ni++) {
            double unseenTagTokens = 0;
            // si = split index
            for (int si = 0; si < numSubStates[ni]; si++) {
                // unseenTagCounter = 2-d array, indexed by base NT, split index, containing counts of occurrences with
                // unseen words
                unseenTagTokens += unseenTagCounter[ni][si];
            }
            // unseenTagTokens = total count of unseen word occurrences with the base NT
            if (unseenTagTokens == 0) {
                continue;
            }
            // wordToTagCounters = 1-d array of maps, indexed by base NT. Each map maps word -> 1-d array of counts,
            // indexed by split index
            for (final Map.Entry<String, double[]> wordToTagEntry : observedTokenFractionalCounts[ni].entrySet()) {
                final String word = wordToTagEntry.getKey();
                final double[] substateCounter = wordToTagEntry.getValue();
                if (wordCounter.getDouble(word) < rareWordThreshold + 0.5) {
                    // c(w|base tag)
                    double wordTagTokens = 0;
                    for (int si = 0; si < numSubStates[ni]; si++) {
                        wordTagTokens += substateCounter[si];
                    }
                    for (int si = 0; si < numSubStates[ni]; si++) {
                        // c(unseen words|T) * c(w|base tag) / c(unseen words|base tag)
                        substateCounter[si] = unseenTagCounter[ni][si] * wordTagTokens / unseenTagTokens;
                    }
                }
            }
        }
    }

    /**
     * Trains this lexicon on the Collection of trees.
     */
    public void trainTree(final Tree<StateSet> trainTree, final double randomness, final Lexicon oldLexicon,
            final boolean noSmoothing, final int rareWordThreshold) {

        // scan data
        // for all substates that the word's preterminal tag has
        double sentenceScore = 0;
        if (randomness == -1) {
            sentenceScore = trainTree.label().insideScore(0);
            if (sentenceScore == 0) {
                System.out.println("Something is wrong with this tree. I will skip it.");
                return;
            }
        }
        final int sentenceScale = trainTree.label().insideScoreScale();

        final List<StateSet> words = trainTree.leafLabels();
        final List<StateSet> tags = trainTree.preterminalLabels();

        // for all words in sentence
        for (int position = 0; position < words.size(); position++) {
            totalWords++;
            final String word = words.get(position).getWord();
            final short state = tags.get(position).getState();
            final int nSubStates = tags.get(position).numSubStates();

            final String sig = getCachedSignature(word, position);

            if (unkFractionalCounts != null) {
                if (unkFractionalCounts[state] == null) {
                    unkFractionalCounts[state] = new HashMap<String, double[]>();
                }
                if (!unkFractionalCounts[state].containsKey(sig)) {
                    unkFractionalCounts[state].put(sig, new double[numSubStates[state]]);
                }
            }
            final double[] unseenWordSubstateCounter = unkFractionalCounts != null ? unkFractionalCounts[state]
                    .get(sig) : null;

            // guarantee that the wordToTagCounter element exists so we can
            // tally the combination
            if (observedTokenFractionalCounts[state] == null) {
                observedTokenFractionalCounts[state] = new HashMap<String, double[]>();
            }
            double[] substateCounter = observedTokenFractionalCounts[state].get(word);
            if (substateCounter == null) {
                substateCounter = new double[numSubStates[state]];
                observedTokenFractionalCounts[state].put(word, substateCounter);
            }

            double[] oldLexiconScores = null;
            if (randomness == -1) {
                oldLexiconScores = oldLexicon.score(word, state, position, noSmoothing, false);
            }

            final StateSet currentState = tags.get(position);
            final double scalingMultiplier = IEEEDoubleScaling.scalingMultiplier(currentState.outsideScoreScale()
                    - sentenceScale)
                    / sentenceScore;

            for (short substate = 0; substate < nSubStates; substate++) {

                double weight;

                if (oldLexiconScores != null) {
                    // weight by the probability of seeing the tag and word together, given the sentence
                    weight = currentState.outsideScore(substate) * oldLexiconScores[substate] * scalingMultiplier;
                    if (weight == 0) {
                        continue;
                    }

                } else {
                    // add a bit of randomness
                    weight = GrammarTrainer.RANDOM.nextDouble() * randomness / 100.0 + 1.0;
                }

                // tally in the tag with the given weight
                substateCounter[substate] += weight;
                // update the counters
                tagCounter[state][substate] += weight;
                wordCounter.add(word, weight);
                totalTokens += weight;

                if (oldLexicon != null && oldLexicon.wordCounter.getDouble(word) < rareWordThreshold + 0.5) {
                    wordCounter.add(sig, weight);
                    if (unseenWordSubstateCounter != null) {
                        unseenWordSubstateCounter[substate] += weight;
                    }
                    unseenTagCounter[state][substate] += weight;
                    totalUnseenTokens += weight;
                }
            }

            if (Double.isNaN(totalTokens)) {
                throw new IllegalArgumentException("totalTokens is NaN");
            }
        }
    }

    /**
     * Returns a String representation of unknown-word features, retrieved from {@link #cachedSignatures} or
     * {@link #cachedSentenceInitialSignatures} if possible.
     * 
     * @return a String representation of unknown-word features
     */
    private String getCachedSignature(final String word, final int sentencePosition) {

        if (sentencePosition == 0) {
            String signature = cachedSentenceInitialSignatures.get(word);
            if (signature == null) {
                signature = getSignature(word, true);
                cachedSentenceInitialSignatures.put(word, signature);
            }
            return signature;
        }

        String signature = cachedSignatures.get(word);
        if (signature == null) {
            signature = getSignature(word, false);
            cachedSignatures.put(word, signature);
        }
        return signature;
    }

    /**
     * Merge states, combining information about words we have seen. THIS DOES NOT UPDATE INFORMATION FOR UNSEEN WORDS!
     * For that, retrain the Lexicon!
     * 
     * @param mergeThesePairs
     * @param substateConditionalProbabilities
     */
    public void mergeStates(final boolean[][][] mergeThesePairs, final double[][] substateConditionalProbabilities) {
        final short[] newNumSubStates = new short[numSubStates.length];
        final short[][] mapping = new short[numSubStates.length][];
        // invariant: if partners[state][substate][0] == substate, it's the 1st
        // one
        final short[][][] partners = new short[numSubStates.length][][];
        Grammar.calculateMergeArrays(mergeThesePairs, newNumSubStates, mapping, partners, numSubStates);

        for (int tag = 0; tag < mergeThesePairs.length; tag++) {
            // update wordToTagCounters
            if (observedTokenFractionalCounts[tag] != null) {
                for (final String word : observedTokenFractionalCounts[tag].keySet()) {
                    final double[] scores = observedTokenFractionalCounts[tag].get(word);
                    final double[] newScores = new double[newNumSubStates[tag]];
                    for (int i = 0; i < numSubStates[tag]; i++) {
                        final short nSplit = (short) partners[tag][i].length;
                        if (nSplit == 2) {
                            newScores[mapping[tag][i]] = scores[partners[tag][i][0]] + scores[partners[tag][i][1]];
                        } else {
                            newScores[mapping[tag][i]] = scores[i];
                        }
                    }
                    observedTokenFractionalCounts[tag].put(word, newScores);
                }
            }
            // update tag counter
            final double[] newTagCounter = new double[newNumSubStates[tag]];
            for (int i = 0; i < numSubStates[tag]; i++) {
                if (partners[tag][i].length == 2) {
                    newTagCounter[mapping[tag][i]] = tagCounter[tag][partners[tag][i][0]]
                            + tagCounter[tag][partners[tag][i][1]];
                } else {
                    newTagCounter[mapping[tag][i]] = tagCounter[tag][i];
                }
            }
            tagCounter[tag] = newTagCounter;
        }

        numSubStates = newNumSubStates;
    }

    /**
     * Merges a single pair of state splits
     * 
     * @param mergeCandidate
     * @return A new {@link Lexicon}, merging the state-split of the specified {@link MergeCandidate} and including
     *         counts derived from this {@link Lexicon}
     */
    public Lexicon merge(final MergeCandidate mergeCandidate) {

        final short[] newNumSubStates = numSubStates.clone();
        newNumSubStates[mergeCandidate.state]--;

        final Lexicon newLexicon = new Lexicon(newNumSubStates, smoothingParams, smoother,
                this.unkFractionalCounts != null, this.threshold);
        newLexicon.totalTokens = totalTokens;
        newLexicon.totalUnseenTokens = totalUnseenTokens;
        newLexicon.totalWords = totalWords;
        newLexicon.smoother = smoother;
        newLexicon.tagCounter = new double[tagCounter.length][];
        newLexicon.unseenTagCounter = new double[unseenTagCounter.length][];
        newLexicon.wordCounter = wordCounter.clone();
        newLexicon.smoothInUnknownsThreshold = smoothInUnknownsThreshold;

        // Copy word-to-tag counters and sum counts from the merge candidate

        for (int state = 0; state < observedTokenFractionalCounts.length; state++) {
            if (observedTokenFractionalCounts[state] == null) {
                continue;
            }

            newLexicon.observedTokenFractionalCounts[state] = new HashMap<String, double[]>();

            for (final String word : observedTokenFractionalCounts[state].keySet()) {

                if (state != mergeCandidate.state) {
                    // Just clone the existing count array
                    newLexicon.observedTokenFractionalCounts[state].put(word,
                            observedTokenFractionalCounts[state].get(word).clone());
                } else {
                    // Special-case for merging the substates of the MergeCandidate
                    newLexicon.observedTokenFractionalCounts[state].put(word,
                            mergeCounts(observedTokenFractionalCounts[state].get(word), mergeCandidate));
                }
            }
        }

        // Copy unseen-word counters (again summing counts for the merge candidate)

        if (unkFractionalCounts != null) {
            for (int state = 0; state < unkFractionalCounts.length; state++) {
                if (unkFractionalCounts[state] == null) {
                    continue;
                }

                newLexicon.unkFractionalCounts[state] = new HashMap<String, double[]>();

                for (final String word : unkFractionalCounts[state].keySet()) {

                    if (state != mergeCandidate.state) {
                        // Just clone the existing count array
                        newLexicon.unkFractionalCounts[state].put(word, unkFractionalCounts[state].get(word).clone());

                    } else {
                        // Special-case for merging the substates of the MergeCandidate
                        newLexicon.unkFractionalCounts[state].put(word,
                                mergeCounts(unkFractionalCounts[state].get(word), mergeCandidate));
                    }
                }
            }
        }

        // Merge tag counts
        for (int state = 0; state < tagCounter.length; state++) {
            if (state != mergeCandidate.state) {
                // Just clone the existing count arrays
                newLexicon.tagCounter[state] = tagCounter[state].clone();
                newLexicon.unseenTagCounter[state] = unseenTagCounter[state].clone();
            } else {
                newLexicon.tagCounter[state] = mergeCounts(tagCounter[state], mergeCandidate);
                newLexicon.unseenTagCounter[state] = mergeCounts(unseenTagCounter[state], mergeCandidate);
            }
        }

        return newLexicon;
    }

    private double[] mergeCounts(final double[] oldCounts, final MergeCandidate mergeCandidate) {
        final double[] mergedCounts = new double[oldCounts.length - 1];

        // Copy entries from the old count array into the merged count array
        System.arraycopy(oldCounts, 0, mergedCounts, 0, mergeCandidate.substate2);
        System.arraycopy(oldCounts, mergeCandidate.substate2 + 1, mergedCounts, mergeCandidate.substate2,
                mergedCounts.length - mergeCandidate.substate2);
        // And add counts from the 2nd merged substate to those for the 1st
        mergedCounts[mergeCandidate.substate1] += oldCounts[mergeCandidate.substate2];

        return mergedCounts;
    }

    public void removeUnlikelyTags(final double filteringThreshold, final double exponent) {

        for (int tag = 0; tag < numSubStates.length; tag++) {
            double[] c_TW;
            if (observedTokenFractionalCounts[tag] != null) {
                for (final String word : observedTokenFractionalCounts[tag].keySet()) {
                    c_TW = observedTokenFractionalCounts[tag].get(word);
                    for (int substate = 0; substate < numSubStates[tag]; substate++) {
                        if (c_TW[substate] < filteringThreshold) {
                            c_TW[substate] = 0;
                        }
                    }
                }
            }
        }
    }

    public int totalRules(final double minimumRuleProbability) {
        int count = 0;

        for (int tag = 0; tag < observedTokenFractionalCounts.length; tag++) {
            if (observedTokenFractionalCounts[tag] != null) {
                for (final String word : observedTokenFractionalCounts[tag].keySet()) {
                    final double[] scores = score(word, (short) tag, 0, false, false);
                    for (int split = 0; split < scores.length; split++) {
                        if (scores[split] > minimumRuleProbability) {
                            count++;
                        }
                    }
                }
            }

            if (unkFractionalCounts != null && unkFractionalCounts[tag] != null) {
                for (final String word : unkFractionalCounts[tag].keySet()) {

                    final double[] scores = score(word, (short) tag, 0, false, true);
                    for (int split = 0; split < scores.length; split++) {
                        if (scores[split] > minimumRuleProbability) {
                            count++;
                        }
                    }
                }
            }
        }
        return count;
    }

    public void setSmoother(final Smoother smoother) {
        this.smoother = smoother;
    }

    public Smoother getSmoother() {
        return smoother;
    }

    public double[] getSmoothingParams() {
        return smoothingParams;
    }

    public double getPruningThreshold() {
        return threshold;
    }

    @Override
    public String toString() {
        return toString(0);
    }

    /**
     * Returns the reduction in lexical rule-count if each pair of non-terminals is merged. These counts are exact for
     * each possible merge (e.g., NN_1 into NN_0), but do not account for the interactions between multiple simultaneous
     * non-terminal merges. I.e., these counts will always be an overestimate of the rule-count savings when multiple
     * NTs are merged en-masse.
     * 
     * @return Array of rule-count savings, indexed by state, split/substate
     */
    public int[][] estimatedMergeRuleCountDelta(final Grammar grammar) {
        final int[][] ruleCountDelta = new int[grammar.numStates][];
        for (int state = 0; state < grammar.numStates; state++) {
            ruleCountDelta[state] = new int[numSubStates[state]];
        }

        // Lots of copy-and-paste from toString()
        for (int tag = 0; tag < observedTokenFractionalCounts.length; tag++) {

            if (observedTokenFractionalCounts[tag] != null) {
                for (final String word : observedTokenFractionalCounts[tag].keySet()) {

                    final double[] scores = score(word, (short) tag, 0, false, false);
                    for (int split = 1; split < scores.length; split += 2) {
                        if (scores[split] > 0 && scores[split - 1] > 0) {
                            ruleCountDelta[tag][split]--;
                        }
                    }
                }
            }

            if (unkFractionalCounts != null && unkFractionalCounts[tag] != null) {
                for (final String word : unkFractionalCounts[tag].keySet()) {

                    final double[] scores = score(word, (short) tag, 0, false, true);
                    for (int split = 1; split < scores.length; split += 2) {
                        if (scores[split] > 0 && scores[split - 1] > 0) {
                            ruleCountDelta[tag][split]--;
                        }
                    }
                }
            }
        }

        return ruleCountDelta;
    }

    public String toString(final double minimumRuleProbability) {

        final Numberer n = Numberer.getGlobalNumberer("tags");
        final StringBuilder sb = new StringBuilder(1024 * 1024);

        for (int tag = 0; tag < observedTokenFractionalCounts.length; tag++) {
            final String tagState = n.symbol(tag);

            if (observedTokenFractionalCounts[tag] != null) {
                for (final String word : observedTokenFractionalCounts[tag].keySet()) {

                    final double[] scores = score(word, (short) tag, 0, false, false);
                    for (int split = 0; split < scores.length; split++) {
                        if (scores[split] > minimumRuleProbability) {
                            sb.append(String.format("%s_%d -> %s %.10f\n", tagState, split, word,
                                    Math.log(scores[split])));
                        }
                    }
                }
            }

            if (unkFractionalCounts != null && unkFractionalCounts[tag] != null) {
                for (final String word : unkFractionalCounts[tag].keySet()) {

                    final double[] scores = score(word, (short) tag, 0, false, true);
                    for (int split = 0; split < scores.length; split++) {
                        if (scores[split] > minimumRuleProbability) {
                            sb.append(String.format("%s_%d -> %s %.10f\n", tagState, split, word,
                                    Math.log(scores[split])));
                        }
                    }
                }
            }
        }

        /*
         * sb.append("--------------------------------------------------\n");
         * sb.append("UNSEEN-TAG-AND-SIGNATURE-COUNTER (c_TW):\n"); for (int tag=0; tag<unseenWordToTagCounters.length;
         * tag++){ if (unseenWordToTagCounters[tag]==null) continue; String tagState = (String)n.object(tag); for
         * (String word : unseenWordToTagCounters[tag].keySet()){ sb.append(tagState+" "+word+" "
         * +Arrays.toString(unseenWordToTagCounters[tag].get(word))+"\n"); } }
         */

        return sb.toString();
    }

    class ChineseLexicon implements Serializable {
        private static final long serialVersionUID = 1L;

        /*
         * These strings are stored in ascii-stype Unicode encoding. To edit them, either use the Unicode codes or use
         * native2ascii or a similar program to convert the file into a Chinese encoding, then convert back.
         */
        public static final String dateMatch = ".*[\u5e74\u6708\u65e5\u53f7]$";
        public static final String numberMatch = ".*[\uff10\uff11\uff12\uff13\uff14\uff15\uff16\uff17\uff18\uff19\uff11\u4e00\u4e8c\u4e09\u56db\u4e94\u516d\u4e03\u516b\u4e5d\u5341\u767e\u5343\u4e07\u4ebf].*";
        public static final String ordinalMatch = "^\u7b2c.*";
        public static final String properNameMatch = ".*\u00b7.*";
    }

}

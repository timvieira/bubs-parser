package edu.berkeley.nlp.PCFGLA;

import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /** A count of strings with tags. Indexed by state, word, and substate. */
    HashMap<String, double[]>[] wordToTagCounters = null;
    HashMap<String, double[]>[] unseenWordToTagCounters = null;
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

    /** Word-tag pairs that occur less are smoothed. */
    int smoothingCutoff;

    /** The default smoothing cutoff. */
    public static int DEFAULT_SMOOTHING_CUTOFF = 10;

    /** Add X smoothing for P(word) */
    double addXSmoothing = 1.0;

    Smoother smoother;
    double threshold;

    Numberer wordNumberer;

    // additions from the stanford parser which are needed for a better
    // unknown word model...
    /**
     * Cache unknown-word signatures - we'll always return the same signature for a word; the signature for a word
     * differs in sentence-initial position, so we maintain 2 caches
     */
    protected transient HashMap<String, String> cachedSignatures = new HashMap<String, String>();
    protected transient HashMap<String, String> cachedSentenceInitialSignatures = new HashMap<String, String>();
    private int unknownLevel = 5; // different modes for unknown words, 5 is
                                  // english specific

    /**
     * Unique word count threshold for open-class preterminals. If a preterminal (POS) is observed with more than
     * {@link #openClassTypesThreshold} unique words, it will be considered open-class. Unknown words will be assgined
     * to open-class preterminals.
     */
    public static int openClassTypesThreshold = 50;

    double smoothInUnknownsThreshold = 100;
    double[] smooth = null;

    /**
     * Create a blank Lexicon object. Fill it by calling tallyStateSetTree for each training tree, then calling
     * optimize().
     * 
     * @param numSubStates
     */
    @SuppressWarnings("unchecked")
    public Lexicon(final short[] numSubStates, final int smoothingCutoff, final double[] smoothParam,
            final Smoother smoother, final double threshold) {
        this.numSubStates = numSubStates;
        this.smoothingCutoff = smoothingCutoff;
        this.smooth = smoothParam;
        this.smoother = smoother;
        wordToTagCounters = new HashMap[numSubStates.length];
        unseenWordToTagCounters = new HashMap[numSubStates.length];
        tagCounter = new double[numSubStates.length][];
        unseenTagCounter = new double[numSubStates.length][];

        for (int i = 0; i < numSubStates.length; i++) {
            tagCounter[i] = new double[numSubStates[i]];
            unseenTagCounter[i] = new double[numSubStates[i]];
        }
        this.threshold = threshold;
        this.wordNumberer = Numberer.getGlobalNumberer("words");
    }

    /** Get the nonterminal tags */
    private boolean isKnown(final String word) {
        return wordCounter.keySet().contains(word);
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
    @SuppressWarnings("unchecked")
    public Lexicon splitAllStates(final int[] counts, final boolean moreSubstatesThanCounts) {

        final short[] newNumSubStates = new short[numSubStates.length];
        newNumSubStates[0] = 1; // never split ROOT
        for (short i = 1; i < numSubStates.length; i++) {
            newNumSubStates[i] = (short) (numSubStates[i] * 2);
        }

        final Lexicon lexicon = new Lexicon(newNumSubStates, this.smoothingCutoff, smooth, smoother, this.threshold);

        // copy and alter all data structures
        lexicon.wordToTagCounters = new HashMap[numSubStates.length];
        lexicon.unseenWordToTagCounters = new HashMap[numSubStates.length];
        for (int tag = 0; tag < wordToTagCounters.length; tag++) {
            if (wordToTagCounters[tag] != null) {
                lexicon.wordToTagCounters[tag] = new HashMap<String, double[]>();
                for (final String word : wordToTagCounters[tag].keySet()) {
                    lexicon.wordToTagCounters[tag].put(word, new double[newNumSubStates[tag]]);
                    for (int substate = 0; substate < wordToTagCounters[tag].get(word).length; substate++) {
                        int splitFactor = 2;
                        if (newNumSubStates[tag] == numSubStates[tag]) {
                            splitFactor = 1;
                        }
                        for (int i = 0; i < splitFactor; i++) {
                            lexicon.wordToTagCounters[tag].get(word)[substate * splitFactor + i] = (1.f / splitFactor)
                                    * wordToTagCounters[tag].get(word)[substate];
                        }
                    }
                }
            }
        }

        for (int tag = 0; tag < unseenWordToTagCounters.length; tag++) {
            if (unseenWordToTagCounters[tag] != null) {
                lexicon.unseenWordToTagCounters[tag] = new HashMap<String, double[]>();
                for (final String word : unseenWordToTagCounters[tag].keySet()) {
                    lexicon.unseenWordToTagCounters[tag].put(word, new double[newNumSubStates[tag]]);
                    for (int substate = 0; substate < unseenWordToTagCounters[tag].get(word).length; substate++) {
                        int splitFactor = 2;
                        if (newNumSubStates[tag] == numSubStates[tag]) {
                            splitFactor = 1;
                        }
                        for (int i = 0; i < splitFactor; i++) {
                            lexicon.unseenWordToTagCounters[tag].get(word)[substate * splitFactor + i] = (1.f / splitFactor)
                                    * unseenWordToTagCounters[tag].get(word)[substate];
                        }
                    }
                }
            }
        }

        lexicon.totalTokens = totalTokens;
        lexicon.totalUnseenTokens = totalUnseenTokens;
        lexicon.totalWords = totalWords;
        lexicon.smoother = smoother;
        lexicon.tagCounter = new double[tagCounter.length][];
        lexicon.unseenTagCounter = new double[unseenTagCounter.length][];

        for (int tag = 0; tag < tagCounter.length; tag++) {
            lexicon.tagCounter[tag] = new double[newNumSubStates[tag]];
            lexicon.unseenTagCounter[tag] = new double[newNumSubStates[tag]];

            for (int substate = 0; substate < tagCounter[tag].length; substate++) {
                int splitFactor = 2;
                if (newNumSubStates[tag] == numSubStates[tag]) {
                    splitFactor = 1;
                }

                for (int i = 0; i < splitFactor; i++) {
                    // lexicon.typeTagCounter[tag][substate * splitFactor + i] = (1.f / splitFactor)
                    // * typeTagCounter[tag][substate];
                    lexicon.tagCounter[tag][substate * splitFactor + i] = (1.f / splitFactor)
                            * tagCounter[tag][substate];
                    lexicon.unseenTagCounter[tag][substate * splitFactor + i] = (1.f / splitFactor)
                            * unseenTagCounter[tag][substate];
                }
            }
        }

        lexicon.wordCounter = new Object2DoubleOpenHashMap<String>();

        for (final String word : wordCounter.keySet()) {
            lexicon.wordCounter.put(word, wordCounter.getDouble(word));
        }

        lexicon.smoothingCutoff = smoothingCutoff;
        lexicon.addXSmoothing = addXSmoothing;
        lexicon.smoothInUnknownsThreshold = smoothInUnknownsThreshold;
        lexicon.wordNumberer = wordNumberer;

        return lexicon;
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
                    if (isKnown(lowered)) {
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
        final HashMap<String, double[]> wordTagCounter = wordToTagCounters[tag];

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
                pb_T_W = (c_TW + smooth[1] * p_T_U) / (c_W + smooth[1]);
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
            if (unseenWordToTagCounters[tag] != null && unseenWordToTagCounters[tag].get(sig) != null) {
                c_TS = unseenWordToTagCounters[tag].get(sig)[substate];
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
            final double pb_T_S = (c_TS + smooth[0] * p_T_U) / (c_S + smooth[0]);

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
            for (final Map.Entry<String, double[]> wordToTagEntry : wordToTagCounters[ni].entrySet()) {
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

            if (unseenWordToTagCounters[state] == null) {
                unseenWordToTagCounters[state] = new HashMap<String, double[]>();
            }
            double[] substateCounter2 = unseenWordToTagCounters[state].get(sig);
            if (substateCounter2 == null) {
                // System.out.print("Sig "+sig+" word "+ word+" pos "+position);
                substateCounter2 = new double[numSubStates[state]];
                unseenWordToTagCounters[state].put(sig, substateCounter2);
            }

            // guarantee that the wordToTagCounter element exists so we can
            // tally the combination
            if (wordToTagCounters[state] == null) {
                wordToTagCounters[state] = new HashMap<String, double[]>();
            }
            double[] substateCounter = wordToTagCounters[state].get(word);
            if (substateCounter == null) {
                substateCounter = new double[numSubStates[state]];
                wordToTagCounters[state].put(word, substateCounter);
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
                    substateCounter2[substate] += weight;
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
     * @param mergeWeights
     */
    public void mergeStates(final boolean[][][] mergeThesePairs, final double[][] mergeWeights) {
        final short[] newNumSubStates = new short[numSubStates.length];
        final short[][] mapping = new short[numSubStates.length][];
        // invariant: if partners[state][substate][0] == substate, it's the 1st
        // one
        final short[][][] partners = new short[numSubStates.length][][];
        Grammar.calculateMergeArrays(mergeThesePairs, newNumSubStates, mapping, partners, numSubStates);

        for (int tag = 0; tag < mergeThesePairs.length; tag++) {
            // update wordToTagCounters
            if (wordToTagCounters[tag] != null) {
                for (final String word : wordToTagCounters[tag].keySet()) {
                    final double[] scores = wordToTagCounters[tag].get(word);
                    final double[] newScores = new double[newNumSubStates[tag]];
                    for (int i = 0; i < numSubStates[tag]; i++) {
                        final short nSplit = (short) partners[tag][i].length;
                        if (nSplit == 2) {
                            newScores[mapping[tag][i]] = scores[partners[tag][i][0]] + scores[partners[tag][i][1]];
                        } else {
                            newScores[mapping[tag][i]] = scores[i];
                        }
                    }
                    wordToTagCounters[tag].put(word, newScores);
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

    public void removeUnlikelyTags(final double filteringThreshold, final double exponent) {

        for (int tag = 0; tag < numSubStates.length; tag++) {
            double[] c_TW;
            if (wordToTagCounters[tag] != null) {
                for (final String word : wordToTagCounters[tag].keySet()) {
                    c_TW = wordToTagCounters[tag].get(word);
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

        for (int tag = 0; tag < wordToTagCounters.length; tag++) {
            if (wordToTagCounters[tag] != null) {
                for (final String word : wordToTagCounters[tag].keySet()) {
                    final double[] scores = score(word, (short) tag, 0, false, false);
                    for (int split = 0; split < scores.length; split++) {
                        if (scores[split] > minimumRuleProbability) {
                            count++;
                        }
                    }
                }
            }

            if (unseenWordToTagCounters[tag] != null) {
                for (final String word : unseenWordToTagCounters[tag].keySet()) {

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
        return smooth;
    }

    public double getPruningThreshold() {
        return threshold;
    }

    @Override
    public String toString() {
        return toString(0);
    }

    public String toString(final double minimumRuleProbability) {

        final Numberer n = Numberer.getGlobalNumberer("tags");
        final StringBuilder sb = new StringBuilder(1024 * 1024);

        for (int tag = 0; tag < wordToTagCounters.length; tag++) {
            final String tagState = n.symbol(tag);

            if (wordToTagCounters[tag] != null) {
                for (final String word : wordToTagCounters[tag].keySet()) {

                    final double[] scores = score(word, (short) tag, 0, false, false);
                    for (int split = 0; split < scores.length; split++) {
                        if (scores[split] > minimumRuleProbability) {
                            sb.append(String.format("%s_%d -> %s %.10f\n", tagState, split, word,
                                    Math.log(scores[split])));
                        }
                    }
                }
            }

            if (unseenWordToTagCounters[tag] != null) {
                for (final String word : unseenWordToTagCounters[tag].keySet()) {

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

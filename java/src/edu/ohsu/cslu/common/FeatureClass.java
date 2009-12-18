/**
 * FeatureClass.java
 *   Copyright 2009 Aaron Dunlop
 */
package edu.ohsu.cslu.common;

import java.util.HashMap;

import edu.ohsu.cslu.tools.SelectFeatures;

/**
 * Enumeration of the various types of feature tokens produced by {@link SelectFeatures} and handled by
 * log-linear modeling tools.
 * 
 * @author Aaron Dunlop
 * @since Mar 27, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public enum FeatureClass implements Comparable<FeatureClass> {
    /** Gap token */
    Gap,

    /** Unknown-word token */
    Unknown,

    /** Any token which does not start with an underscore */
    Word,

    /** _pos_NN, _pos_VBN, etc. */
    Pos,

    /** Labels: _head_verb, _begin_sent, _initial_cap, etc. */
    HeadVerb, FirstVerb, BeforeHead, AfterHead, BeginSentence, Capitalized, AllCaps, Hyphenated, Numeric, InitialNumeric, StartWord, EndWord,

    EndsWithLy, EndsWithIng, EndsWithEd,

    /** _stem_... */
    Stem,

    /** _word-1_..., _word-2_... */
    PreviousWord,

    /** _word+1_..., _word+2_... */
    SubsequentWord,

    /** _pos-1_..., _pos-2_... */
    PreviousPos,

    /** _pos+1_..., _pos+2_... */
    SubsequentPos,

    /** Length classes */
    Length1, Length2, Length3, Length4, Length5to6, Length7to8, Length9to12, Length13to18, LengthGreaterThan18,

    /** Any token starting with an underscore which does not match other patterns */
    Other;

    public final static String FEATURE_GAP = "_-";
    public final static String FEATURE_UNKNOWN = "-unk-";

    public final static String PREFIX_POS = "_pos_";
    public final static String PREFIX_STEM = "_stem_";

    public final static String PREFIX_PREVIOUS_WORD = "_word-";
    public final static String PREFIX_SUBSEQUENT_WORD = "_word+";
    public final static String PREFIX_PREVIOUS_POS = "_pos-";
    public final static String PREFIX_SUBSEQUENT_POS = "_pos+";

    public final static String FEATURE_FIRST_VERB = "_first_verb";
    public final static String FEATURE_BEFORE_HEAD = "_before_head";
    public final static String FEATURE_HEAD_VERB = "_head_verb";
    public final static String FEATURE_AFTER_HEAD = "_after_head";
    public final static String FEATURE_BEGIN_SENTENCE = "_begin_sent";
    public final static String FEATURE_CAPITALIZED = "_capitalized";
    public final static String FEATURE_ALL_CAPS = "_all_caps";
    public final static String FEATURE_HYPHENATED = "_hyphenated";
    public final static String FEATURE_INITIAL_NUMERIC = "_initial_numeric";
    public final static String FEATURE_NUMERIC = "_numeric";
    public final static String FEATURE_START_WORD = "_start_word";
    public final static String FEATURE_END_WORD = "_end_word";

    public final static String FEATURE_ENDSWITH_LY = "_endswith_ly";
    public final static String FEATURE_ENDSWITH_ING = "_endswith_ing";
    public final static String FEATURE_ENDSWITH_ED = "_endswith_ed";

    public final static String FEATURE_LENGTH_1 = "_length_1";
    public final static String FEATURE_LENGTH_2 = "_length_2";
    public final static String FEATURE_LENGTH_3 = "_length_3";
    public final static String FEATURE_LENGTH_4 = "_length_4";
    public final static String FEATURE_LENGTH_5_TO_6 = "_length_5_to_6";
    public final static String FEATURE_LENGTH_7_TO_8 = "_length_7_to_8";
    public final static String FEATURE_LENGTH_9_TO_12 = "_length_9_to_12";
    public final static String FEATURE_LENGTH_13_TO_18 = "_length_13_to_18";
    public final static String FEATURE_LENGTH_GREATER_THAN_18 = "_length_greater_than_18";

    public final static String NEGATION = "_not";

    private static HashMap<String, FeatureClass> knownLabels = new HashMap<String, FeatureClass>();
    static {
        knownLabels.put(FEATURE_BEFORE_HEAD, BeforeHead);
        knownLabels.put(FEATURE_HEAD_VERB, HeadVerb);
        knownLabels.put(FEATURE_AFTER_HEAD, AfterHead);

        knownLabels.put(FEATURE_FIRST_VERB, FirstVerb);
        knownLabels.put(NEGATION + FEATURE_FIRST_VERB, FirstVerb);
        knownLabels.put(FEATURE_BEGIN_SENTENCE, BeginSentence);
        knownLabels.put(FEATURE_CAPITALIZED, Capitalized);
        knownLabels.put(NEGATION + FEATURE_CAPITALIZED, Capitalized);
        knownLabels.put(FEATURE_ALL_CAPS, AllCaps);
        knownLabels.put(NEGATION + FEATURE_ALL_CAPS, AllCaps);
        knownLabels.put(FEATURE_HYPHENATED, Hyphenated);
        knownLabels.put(NEGATION + FEATURE_HYPHENATED, Hyphenated);
        knownLabels.put(FEATURE_LENGTH_1, Length1);
        knownLabels.put(FEATURE_LENGTH_2, Length2);
        knownLabels.put(FEATURE_LENGTH_3, Length3);
        knownLabels.put(FEATURE_LENGTH_4, Length4);
        knownLabels.put(FEATURE_LENGTH_5_TO_6, Length5to6);
        knownLabels.put(FEATURE_LENGTH_7_TO_8, Length7to8);
        knownLabels.put(FEATURE_LENGTH_9_TO_12, Length9to12);
        knownLabels.put(FEATURE_LENGTH_13_TO_18, Length13to18);
        knownLabels.put(FEATURE_LENGTH_GREATER_THAN_18, LengthGreaterThan18);
        knownLabels.put(FEATURE_NUMERIC, Numeric);
        knownLabels.put(NEGATION + FEATURE_NUMERIC, Numeric);
        knownLabels.put(FEATURE_INITIAL_NUMERIC, InitialNumeric);
        knownLabels.put(NEGATION + FEATURE_INITIAL_NUMERIC, InitialNumeric);
        knownLabels.put(FEATURE_START_WORD, StartWord);
        knownLabels.put(NEGATION + FEATURE_START_WORD, StartWord);
        knownLabels.put(FEATURE_END_WORD, EndWord);
        knownLabels.put(NEGATION + FEATURE_END_WORD, EndWord);
        knownLabels.put(FEATURE_ENDSWITH_LY, EndsWithLy);
        knownLabels.put(NEGATION + FEATURE_ENDSWITH_LY, EndsWithLy);
        knownLabels.put(FEATURE_ENDSWITH_ING, EndsWithIng);
        knownLabels.put(NEGATION + FEATURE_ENDSWITH_ING, EndsWithIng);
        knownLabels.put(FEATURE_ENDSWITH_ED, EndsWithEd);
        knownLabels.put(NEGATION + FEATURE_ENDSWITH_ED, EndsWithEd);
    }

    public static FeatureClass forString(String s) {
        // Feature class for negations is the same as for the equivalent 'normal' feature
        if (s.startsWith(NEGATION)) {
            s = s.substring(4);
        }

        if (knownLabels.containsKey(s)) {
            return knownLabels.get(s);
        }

        if (s.equals(FEATURE_GAP)) {
            return Gap;
        }

        if (s.equals(FEATURE_UNKNOWN)) {
            return Unknown;
        }

        if (!s.startsWith("_")) {
            return Word;
        }

        if (s.startsWith(PREFIX_STEM)) {
            return Stem;
        }

        if (s.startsWith(PREFIX_POS)) {
            return Pos;
        }

        if (s.startsWith(PREFIX_PREVIOUS_WORD)) {
            return PreviousWord;
        }

        if (s.startsWith(PREFIX_SUBSEQUENT_WORD)) {
            return SubsequentWord;
        }

        if (s.startsWith(PREFIX_PREVIOUS_POS)) {
            return PreviousPos;
        }

        if (s.startsWith(PREFIX_SUBSEQUENT_POS)) {
            return SubsequentPos;
        }

        return Other;
    }

    @Override
    public String toString() {
        switch (this) {
        case Gap:
            return FEATURE_GAP;
        case Unknown:
            return FEATURE_UNKNOWN;
        case HeadVerb:
            return FEATURE_HEAD_VERB;
        case FirstVerb:
            return FEATURE_FIRST_VERB;
        case BeforeHead:
            return FEATURE_BEFORE_HEAD;
        case AfterHead:
            return FEATURE_AFTER_HEAD;
        case BeginSentence:
            return FEATURE_BEGIN_SENTENCE;
        case Capitalized:
            return FEATURE_CAPITALIZED;
        case AllCaps:
            return FEATURE_ALL_CAPS;
        case Hyphenated:
            return FEATURE_HYPHENATED;
        case Numeric:
            return FEATURE_NUMERIC;
        case InitialNumeric:
            return FEATURE_INITIAL_NUMERIC;
        case StartWord:
            return FEATURE_START_WORD;
        case EndWord:
            return FEATURE_END_WORD;
        case Length1:
            return FEATURE_LENGTH_1;
        case Length2:
            return FEATURE_LENGTH_2;
        case Length3:
            return FEATURE_LENGTH_3;
        case Length4:
            return FEATURE_LENGTH_4;
        case Length5to6:
            return FEATURE_LENGTH_5_TO_6;
        case Length7to8:
            return FEATURE_LENGTH_7_TO_8;
        case Length9to12:
            return FEATURE_LENGTH_9_TO_12;
        case Length13to18:
            return FEATURE_LENGTH_13_TO_18;
        case LengthGreaterThan18:
            return FEATURE_LENGTH_GREATER_THAN_18;

        default:
            return super.toString();
        }
    }
}
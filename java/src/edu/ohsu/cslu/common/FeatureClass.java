/**
 * FeatureClass.java
 *   Copyright 2009 Aaron Dunlop
 */
package edu.ohsu.cslu.common;

import java.util.HashMap;

import edu.ohsu.cslu.tools.SelectFeatures;

/**
 * Enumeration of the various types of feature tokens produced by {@link SelectFeatures} and handled
 * by log-linear modeling tools.
 * 
 * @author Aaron Dunlop
 * @since Mar 27, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public enum FeatureClass implements Comparable<FeatureClass>
{
    /** Gap token */
    Gap,

    /** Unknown-word token */
    Unknown,

    /** Any token which does not start with an underscore */
    Word,

    /** _pos_NN, _pos_VBN, etc. */
    Pos,

    /** Labels: _head_verb, _begin_sent, _initial_cap, etc. */
    HeadVerb, NotFirstVerb, BeforeHead, AfterHead, BeginSentence, Capitalized, AllCaps, Hyphenated, StartsWithDigit,

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
    Length1, Length2to5, Length6to10, LengthGreaterThan10,

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

    public final static String FEATURE_NOT_FIRST_VERB = "_not_first_verb";
    public final static String FEATURE_BEFORE_HEAD = "_before_head";
    public final static String FEATURE_HEAD_VERB = "_head_verb";
    public final static String FEATURE_AFTER_HEAD = "_after_head";
    public final static String FEATURE_BEGIN_SENTENCE = "_begin_sent";
    public final static String FEATURE_CAPITALIZED = "_capitalized";
    public final static String FEATURE_ALL_CAPS = "_all_caps";
    public final static String FEATURE_HYPHENATED = "_hyphenated";

    public final static String FEATURE_LENGTH_1 = "_length_1";
    public final static String FEATURE_LENGTH_2_TO_5 = "_length_2_to_5";
    public final static String FEATURE_LENGTH_6_TO_10 = "_length_6_to_10";
    public final static String FEATURE_LENGTH_GREATER_THAN_10 = "_length_greater_than_10";

    public final static String FEATURE_STARTS_WITH_DIGIT = "_starts_with_digit";

    private static HashMap<String, FeatureClass> knownLabels = new HashMap<String, FeatureClass>();
    static
    {
        knownLabels.put(FEATURE_NOT_FIRST_VERB, NotFirstVerb);
        knownLabels.put(FEATURE_BEFORE_HEAD, BeforeHead);
        knownLabels.put(FEATURE_HEAD_VERB, HeadVerb);
        knownLabels.put(FEATURE_AFTER_HEAD, AfterHead);
        knownLabels.put(FEATURE_BEGIN_SENTENCE, BeginSentence);
        knownLabels.put(FEATURE_CAPITALIZED, Capitalized);
        knownLabels.put(FEATURE_ALL_CAPS, AllCaps);
        knownLabels.put(FEATURE_HYPHENATED, Hyphenated);
        knownLabels.put(FEATURE_LENGTH_1, Length1);
        knownLabels.put(FEATURE_LENGTH_2_TO_5, Length2to5);
        knownLabels.put(FEATURE_LENGTH_6_TO_10, Length6to10);
        knownLabels.put(FEATURE_LENGTH_GREATER_THAN_10, LengthGreaterThan10);
        knownLabels.put(FEATURE_STARTS_WITH_DIGIT, StartsWithDigit);
    }

    public static FeatureClass forString(String s)
    {
        if (knownLabels.containsKey(s))
        {
            return knownLabels.get(s);
        }

        if (s.equals(FEATURE_GAP))
        {
            return Gap;
        }

        if (s.equals(FEATURE_UNKNOWN))
        {
            return Unknown;
        }

        if (!s.startsWith("_"))
        {
            return Word;
        }

        if (s.startsWith(PREFIX_STEM))
        {
            return Stem;
        }

        if (s.startsWith(PREFIX_POS))
        {
            return Pos;
        }

        if (s.startsWith(PREFIX_PREVIOUS_WORD))
        {
            return PreviousWord;
        }

        if (s.startsWith(PREFIX_SUBSEQUENT_WORD))
        {
            return SubsequentWord;
        }

        if (s.startsWith(PREFIX_PREVIOUS_POS))
        {
            return PreviousPos;
        }

        if (s.startsWith(PREFIX_SUBSEQUENT_POS))
        {
            return SubsequentPos;
        }

        return Other;
    }

    @Override
    public String toString()
    {
        switch (this)
        {
            case Gap :
                return FEATURE_GAP;
            case Unknown :
                return FEATURE_UNKNOWN;
            case HeadVerb :
                return FEATURE_HEAD_VERB;
            case NotFirstVerb :
                return FEATURE_NOT_FIRST_VERB;
            case BeforeHead :
                return FEATURE_BEFORE_HEAD;
            case AfterHead :
                return FEATURE_AFTER_HEAD;
            case BeginSentence :
                return FEATURE_BEGIN_SENTENCE;
            case Capitalized :
                return FEATURE_CAPITALIZED;
            case AllCaps :
                return FEATURE_ALL_CAPS;
            case Hyphenated :
                return FEATURE_HYPHENATED;
            case StartsWithDigit :
                return FEATURE_STARTS_WITH_DIGIT;
            case Length1 :
                return FEATURE_LENGTH_1;
            case Length2to5 :
                return FEATURE_LENGTH_2_TO_5;
            case Length6to10 :
                return FEATURE_LENGTH_6_TO_10;
            case LengthGreaterThan10 :
                return FEATURE_LENGTH_GREATER_THAN_10;

            default :
                return super.toString();
        }
    }
}
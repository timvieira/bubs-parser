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

    /** Any token which does not start with an underscore */
    Word,

    /** _pos_NN, _pos_VBN, etc. */
    Pos,

    /** Labels: _head_verb, _begin_sent, _initial_cap, etc. */
    HeadVerb, NotFirstVerb, BeforeHead, AfterHead, BeginSentence, InitialCap, AllCaps,

    /** _word-1_..., _word-2_... */
    PreviousWord,

    /** _word+1_..., _word+2_... */
    SubsequentWord,

    /** _pos-1_..., _pos-2_... */
    PreviousPos,

    /** _pos+1_..., _pos+2_... */
    SubsequentPos,

    /** Any token starting with an underscore which does not match other patterns */
    Other;

    public final static String GAP = "_-";

    public final static String PREFIX_POS = "_pos_";
    public final static String PREFIX_PREVIOUS_WORD = "_word-";
    public final static String PREFIX_SUBSEQUENT_WORD = "_word+";
    public final static String PREFIX_PREVIOUS_POS = "_pos-";
    public final static String PREFIX_SUBSEQUENT_POS = "_pos+";

    public final static String FEATURE_NOT_FIRST_VERB = "_not_first_verb";
    public final static String FEATURE_BEFORE_HEAD = "_before_head";
    public final static String FEATURE_HEAD_VERB = "_head_verb";
    public final static String FEATURE_AFTER_HEAD = "_after_head";
    public final static String FEATURE_BEGIN_SENTENCE = "_begin_sent";
    public final static String FEATURE_INITIAL_CAP = "_initial_cap";
    public final static String FEATURE_ALL_CAPS = "_all_caps";

    private static HashMap<String, FeatureClass> knownLabels = new HashMap<String, FeatureClass>();
    static
    {
        knownLabels.put(FEATURE_NOT_FIRST_VERB, NotFirstVerb);
        knownLabels.put(FEATURE_BEFORE_HEAD, BeforeHead);
        knownLabels.put(FEATURE_HEAD_VERB, HeadVerb);
        knownLabels.put(FEATURE_AFTER_HEAD, AfterHead);
        knownLabels.put(FEATURE_BEGIN_SENTENCE, BeginSentence);
        knownLabels.put(FEATURE_INITIAL_CAP, InitialCap);
        knownLabels.put(FEATURE_ALL_CAPS, AllCaps);
    }

    public static FeatureClass forString(String s)
    {
        if (knownLabels.containsKey(s))
        {
            return knownLabels.get(s);
        }

        if (s.equals(GAP))
        {
            return Gap;
        }

        if (!s.startsWith("_"))
        {
            return Word;
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
}
/**
 * FeatureClass.java
 *   Copyright 2009 Aaron Dunlop
 */
package edu.ohsu.cslu.common;

import java.util.HashMap;

import edu.ohsu.cslu.tools.SelectFeatures;

/**
 * Enumeration of the various types of feature tokens produced by {@link SelectFeatures} and handled by log-linear modeling tools.
 * 
 * @author Aaron Dunlop
 * @since Mar 27, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public enum FeatureClass implements Comparable<FeatureClass> {

    /** Gap token */
    Gap("_-"),

    /** Unknown-word token */
    Unknown("-unk-"),

    /** Any token which does not start with an underscore */
    Word(""),

    /** _pos_NN, _pos_VBN, etc. */
    Pos("_pos_"),

    /** Labels: _head_verb, _begin_sent, _initial_cap, etc. */
    HeadVerb("_head_verb"), FirstVerb("_first_verb"), BeforeHead("_before_head"), AfterHead("_after_head"), BeginSentence("_begin_sent"),

    Capitalized("_capitalized"), AllCaps("_all_caps"), Hyphenated("_hyphenated"), Numeric("_numeric"), InitialNumeric("_initial_numeric"), StartWord("_start_word"), EndWord(
            "_end_word"),

    EndsWithAl("_endswith_al"), EndsWithBle("_endswith_ble"), EndsWithEd("_endswith_ed"), EndsWithEr("_endswith_er"), EndsWithEst("_endswith_est"), EndsWithIc("_endswith_ic"), EndsWithIng(
            "_endswith_ing"), EndsWithIve("_endswith_ive"), EndsWithLy("_endswith_ly"),

    /** _stem_... */
    Stem("_stem_"),

    /** _word-1_..., _word-2_... */
    PreviousWord("_word-"),

    /** _word+1_..., _word+2_... */
    SubsequentWord("_word+"),

    /** _pos-1_..., _pos-2_... */
    PreviousPos("_pos-"),

    /** _pos+1_..., _pos+2_... */
    SubsequentPos("_pos+"),

    /** Length classes */
    Length1("_length_1"), Length2("_length_2"), Length3("_length_3"), Length4("_length_4"), Length5to6("_length_5_to_6"), Length7to8("_length_7_to_8"), Length9to12(
            "_length_9_to_12"), Length13to18("_length_13_to_18"), LengthGreaterThan18("_length_greater_than_18"),

    /** Any token starting with an underscore which does not match other patterns */
    Other("_");

    public final static String NEGATION = "_not";

    public static FeatureClass forString(String s) {
        // Feature class for negations is the same as for the equivalent 'normal' feature
        if (s.startsWith(NEGATION)) {
            s = s.substring(4);
        }

        final FeatureClass mappedClass = FeatureClassMap.singleton.get(s);
        if (mappedClass != null) {
            return mappedClass;
        }

        if (!s.startsWith("_")) {
            return Word;
        }

        if (s.startsWith(Stem.labelOrPrefix)) {
            return Stem;
        }

        if (s.startsWith(Pos.labelOrPrefix)) {
            return Pos;
        }

        if (s.startsWith(PreviousWord.labelOrPrefix)) {
            return PreviousWord;
        }

        if (s.startsWith(SubsequentWord.labelOrPrefix)) {
            return SubsequentWord;
        }

        if (s.startsWith(PreviousPos.labelOrPrefix)) {
            return PreviousPos;
        }

        if (s.startsWith(SubsequentPos.labelOrPrefix)) {
            return SubsequentPos;
        }

        return Other;
    }

    @Override
    public String toString() {
        return labelOrPrefix;
    }

    private String labelOrPrefix;

    private FeatureClass(final String label) {
        this.labelOrPrefix = label;
        FeatureClassMap.singleton.put(label, this);
    }

    private final static class FeatureClassMap extends HashMap<String, FeatureClass> {
        private static FeatureClassMap singleton = new FeatureClassMap();
    }
}
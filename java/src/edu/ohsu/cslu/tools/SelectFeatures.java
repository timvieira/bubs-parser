/*
 * Copyright 2010-2014, Oregon Health & Science University
 * 
 * This file is part of the BUBS Parser.
 * 
 * The BUBS Parser is free software: you can redistribute it and/or 
 * modify  it under the terms of the GNU Affero General Public License 
 * as published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * The BUBS Parser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Further documentation and contact information is available at
 *   https://code.google.com/p/bubs-parser/ 
 */
package edu.ohsu.cslu.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import cltool4j.LinewiseCommandlineTool;
import cltool4j.Threadable;
import cltool4j.args4j.CmdLineException;
import cltool4j.args4j.EnumAliasMap;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.datastructs.narytree.HeadPercolationRuleset;
import edu.ohsu.cslu.datastructs.narytree.MsaHeadPercolationRuleset;
import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.util.PorterStemmer;
import edu.ohsu.cslu.util.Strings;

/**
 * Selects and formats features from a variously formatted sentences (including Penn-Treebank parse trees,
 * parenthesis-bracketed flat structures, and Stanford's slash-delimited tagged representation).
 * 
 * Outputs flat structures in bracketed or Stanford formats.
 * 
 * Supported features from parse trees:
 * <ul>
 * <li>Word</li>
 * <li>Part-of-Speech</li>
 * <li>before_head / head_verb / after_head</li>
 * </ul>
 * 
 * From flat bracketed input, arbitrary features can be selected in any order (e.g. features 1, 6, and 3 output in that
 * order).
 * 
 * @author Aaron Dunlop
 * @since Nov 17, 2008
 * 
 * @version $Revision$ $Date$ $Author$
 */
@Threadable(defaultThreads = 1)
public class SelectFeatures extends LinewiseCommandlineTool<String> {

    @Option(name = "-i", aliases = { "--input-format" }, metaVar = "format (tree|bracketed|square-bracketed|stanford)", usage = "Input format.")
    private FileFormat inputFormat = FileFormat.BracketedTree;

    @Option(name = "-o", aliases = { "--output-format" }, metaVar = "format (tree|bracketed|square-bracketed|stanford)", usage = "Output format.")
    private FileFormat outputFormat = FileFormat.Stanford;

    /** TreeBank POS prefixed with 'pos_' e.g. pos_DT, pos_. */
    @Option(name = "-p", aliases = { "--pos", "--part-of-speech" }, usage = "Include POS feature (_pos_...)")
    private boolean pos;

    /** TreeBank POS without a prefix e.g. DT, NN */
    @Option(name = "-ppos", aliases = { "--plain-pos" }, usage = "Include plain POS feature (without _pos_ prefix)")
    private boolean plainPos;

    @Option(name = "-w", aliases = { "--word" }, usage = "Include token")
    private boolean includeWord;

    @Option(name = "-lcw", aliases = { "--lowercase-word" }, usage = "Include lowercased token")
    private boolean includeLowercaseWord;

    @Option(name = "-stem", usage = "Include word stem (_stem_...)")
    private boolean includeStem;

    @Option(name = "-h", aliases = { "--head-verb" }, usage = "Include _head_verb feature")
    private boolean headVerb;

    @Option(name = "-fv", aliases = { "--first-verb" }, separator = ",", metaVar = "Parts-of-speech", usage = "Include _first_verb feature")
    private Set<String> firstVerbPos;

    @Option(name = "-bh", aliases = { "--before-head" }, usage = "Include _before_head_verb feature")
    private boolean beforeHead;

    @Option(name = "-ah", aliases = { "--after-head" }, usage = "Include _after_head_verb feature")
    private boolean afterHead;

    @Option(name = "-cap", aliases = { "--capitalized" }, usage = "Include capitalization feature")
    private boolean capitalized;

    @Option(name = "-allcaps", aliases = { "--all-caps" }, usage = "Include all-capitalized feature")
    private boolean allcaps;

    @Option(name = "-hyphen", aliases = { "--hyphenated" }, usage = "Include hyphenization feature")
    private boolean hyphenated;

    @Option(name = "-init-num", aliases = { "--initial-numeric" }, usage = "Include initial-numeric feature (starts with a numeral)")
    private boolean initialNumeric;

    @Option(name = "-num", aliases = { "--numeric" }, usage = "Include numeric feature (all numerals)")
    private boolean numeric;

    // TODO Support this for tree input
    @Option(name = "-start", aliases = { "--start-word" }, usage = "Include start-word feature")
    private boolean startWord;

    // TODO Support this for tree input
    @Option(name = "-end", aliases = { "--end-word" }, usage = "Include end-word feature")
    private boolean endWord;

    @Option(name = "-endly", aliases = { "--ends-with-ly" }, usage = "Include ends-with-ly feature")
    private boolean endsWithLy;

    @Option(name = "-ending", aliases = { "--ends-with-ing" }, usage = "Include ends-with-ing feature")
    private boolean endsWithIng;

    @Option(name = "-ended", aliases = { "--ends-with-ed" }, usage = "Include ends-with-ed feature")
    private boolean endsWithEd;

    @Option(name = "-ender", aliases = { "--ends-with-er" }, usage = "Include ends-with-er feature")
    private boolean endsWithEr;

    @Option(name = "-endest", aliases = { "--ends-with-est" }, usage = "Include ends-with-est feature")
    private boolean endsWithEst;

    @Option(name = "-endble", aliases = { "--ends-with-ble" }, usage = "Include ends-with-ble feature")
    private boolean endsWithBle;

    @Option(name = "-endive", aliases = { "--ends-with-ive" }, usage = "Include ends-with-ive feature")
    private boolean endsWithIve;

    @Option(name = "-endic", aliases = { "--ends-with-ic" }, usage = "Include ends-with-ic feature")
    private boolean endsWithIc;

    @Option(name = "-endal", aliases = { "--ends-with-al" }, usage = "Include ends-with-al feature")
    private boolean endsWithAl;

    @Option(name = "-length", aliases = { "--word-length" }, usage = "Include length features")
    private boolean length;

    @Option(name = "-prevword", aliases = { "--previous-words" }, metaVar = "count", usage = "Include previous words")
    private int previousWords;

    @Option(name = "-subword", aliases = { "--subsequent-words" }, metaVar = "count", usage = "Include subsequent words")
    private int subsequentWords;

    @Option(name = "-prevpos", aliases = { "--previous-pos" }, metaVar = "count", usage = "Include pos for previous words")
    private int previousPos;

    @Option(name = "-subpos", aliases = { "--subsequent-pos" }, metaVar = "count", usage = "Include pos for subsequent words")
    private int subsequentPos;

    @Option(name = "-f", aliases = { "--features" }, separator = ",", metaVar = "index", usage = "Feature index (in bracketed input) of token to treat as the word (starting with 1) Default = 1")
    private int[] selectedFeatures;

    @Option(name = "-n", aliases = { "--negation", "--label-indicator-negation" }, usage = "Include labels for negation of indicator features (e.g. _not_numeric")
    private boolean labelIndicatorNegations;

    private String beginBracket;
    private String endBracket;
    private String featureDelimiter;

    /** Ignored for tree and pos-tagged formats */
    @Option(name = "-wi", aliases = { "--word-index" }, metaVar = "index", usage = "Feature index (in bracketed input) of token to treat as the word (starting with 1) Default = 1")
    private int wordIndex = 1;

    // TODO: Allow other head-percolation rulesets?
    private final HeadPercolationRuleset ruleset = new MsaHeadPercolationRuleset();

    /** Features only supported when input is in tree format */
    static HashSet<String> TREE_FEATURES = new HashSet<String>();

    static {
        TREE_FEATURES.add("-p"); // pos

        TREE_FEATURES.add("-h"); // head_verb
        TREE_FEATURES.add("-bh"); // before_head
        TREE_FEATURES.add("-ah"); // after_head

        TREE_FEATURES.add("-prevpos"); // previous pos (pos-n_NNP)
        TREE_FEATURES.add("-subpos"); // subsequent pos (pos+n_NN)
    }

    public static void main(final String[] args) {
        run(args);
    }

    @Override
    public void setup() throws CmdLineException {
        // Some input formats do not support all features
        if (pos || previousPos != 0 || subsequentPos != 0) {
            switch (inputFormat) {
            case BracketedTree:
            case SquareBracketedTree:
            case PosTagged:
            case SquarePosTagged:
                break;
            default:
                throw new CmdLineException("POS features not supported for un-formatted flat input");
            }
        }

        if ((includeWord || previousWords != 0 || subsequentWords != 0) && wordIndex <= 0) {
            switch (inputFormat) {
            case BracketedTree:
            case SquareBracketedTree:
            case PosTagged:
            case SquarePosTagged:
                break;
            default:
                throw new CmdLineException("Word features not supported for un-formatted flat input");
            }
        }

        if (headVerb || beforeHead || afterHead) {
            switch (inputFormat) {
            case BracketedTree:
            case SquareBracketedTree:
                break;
            default:
                throw new CmdLineException("Head verb features not supported for flat input");
            }
        }

        if (selectedFeatures != null && selectedFeatures.length > 0 && wordIndex != 1) {
            throw new CmdLineException("Cannot select both feature indices and word index");
        }

        // And 'f' option when selecting from a flat format
        if (selectedFeatures != null && selectedFeatures.length > 0 && !inputFormat.isFlatFormat()) {
            throw new CmdLineException("Field selection is only supported for flat input formats");
        }

        switch (outputFormat) {
        case Bracketed:
            beginBracket = "(";
            endBracket = ")";
            featureDelimiter = " ";
            break;
        case SquareBracketed:
            beginBracket = "[";
            endBracket = "]";
            featureDelimiter = " ";
            break;
        case Stanford:
            beginBracket = "";
            endBracket = "";
            featureDelimiter = "/";
            break;
        default:
            throw new CmdLineException("Unsuppported output format: " + outputFormat);
        }

        if (selectedFeatures != null) {
            for (int i = 0; i < selectedFeatures.length; i++) {
                // Command-line feature argument starts indexing with 1, but we want to index
                // starting with 0
                selectedFeatures[i]--;
            }
        }
    }

    private String selectTreeFeatures(final String parsedSentence) {
        final NaryTree<String> tree = NaryTree.read(parsedSentence, String.class);
        final StringBuilder sb = new StringBuilder(parsedSentence.length());

        // Start with before_head
        int headPosition = 0;
        boolean foundFirstVerb = false;

        // Construct a flattened representation
        final ArrayList<String> wordList = new ArrayList<String>(64);
        final ArrayList<String> posList = new ArrayList<String>(64);
        for (final NaryTree<String> node : tree.inOrderTraversal()) {
            if (node.isLeaf()) {
                wordList.add(node.label().toLowerCase());
                posList.add(node.parent().label());
            }
        }

        int i = 0;
        for (final NaryTree<String> node : tree.inOrderTraversal()) {
            final String posLabel = node.parent() != null ? node.parent().label() : null;

            if (node.isLeaf()) {
                sb.append(beginBracket);

                appendWord(sb, node.label());
                appendPos(sb, posLabel);
                appendWordFeatures(sb, node.label());

                // Previous words
                for (int j = 1; ((j <= previousWords) && (i - j >= 0)); j++) {
                    sb.append(String.format("%s%d_%s%s", FeatureClass.PreviousWord, j, wordList.get(i - j),
                            featureDelimiter));
                }

                // Subsequent words
                for (int j = 1; ((j <= subsequentWords) && (i + j < wordList.size())); j++) {
                    sb.append(String.format("%s%d_%s%s", FeatureClass.SubsequentWord, j, wordList.get(i + j),
                            featureDelimiter));
                }

                // Previous POS
                for (int j = 1; ((j <= previousPos) && (i - j >= 0)); j++) {
                    sb.append(String.format("%s%d_%s%s", FeatureClass.PreviousPos, j, posList.get(i - j),
                            featureDelimiter));
                }

                // Subsequent POS
                for (int j = 1; ((j <= subsequentPos) && (i + j < posList.size())); j++) {
                    sb.append(String.format("%s%d_%s%s", FeatureClass.SubsequentPos, j, posList.get(i + j),
                            featureDelimiter));
                }

                try {
                    if (node.isHeadOfTreeRoot(ruleset)) {
                        if (headVerb) {
                            sb.append(FeatureClass.HeadVerb);
                            sb.append(featureDelimiter);
                        }
                        // Switch feature tag to after_head
                        headPosition = 1;
                    } else if (beforeHead && headPosition == 0) {
                        sb.append(FeatureClass.BeforeHead);
                        sb.append(featureDelimiter);
                    } else if (afterHead && headPosition == 1) {
                        sb.append(FeatureClass.AfterHead);
                        sb.append(featureDelimiter);
                    }
                } catch (final IllegalArgumentException ignore) {
                    // If we don't have a ruleset for this language, skip these features
                }

                if (firstVerbPos != null) {
                    if (posLabel != null && !foundFirstVerb && firstVerbPos.contains(posLabel)) {
                        foundFirstVerb = true;
                        sb.append(FeatureClass.FirstVerb);
                        sb.append(featureDelimiter);
                    } else if (labelIndicatorNegations) {
                        sb.append(FeatureClass.NEGATION + FeatureClass.FirstVerb);
                        sb.append(featureDelimiter);
                    }
                }

                // Delete the final feature delimiter
                sb.delete(sb.length() - featureDelimiter.length(), sb.length());

                // End the bracket and token
                sb.append(endBracket);
                sb.append(' ');

                i++;
            }
        }
        // Delete the final trailing space
        sb.deleteCharAt(sb.length() - 1);

        return sb.toString();
    }

    private void appendIndicatorFeature(final StringBuilder sb, final boolean enabled, final boolean fired,
            final FeatureClass featureClass) {
        if (enabled) {

            if (fired) {
                sb.append(featureClass.toString());
                sb.append(featureDelimiter);
            } else if (labelIndicatorNegations) {
                sb.append(FeatureClass.NEGATION + featureClass.toString());
                sb.append(featureDelimiter);
            }
        }
    }

    private void appendWordFeatures(final StringBuilder sb, final String label) {
        final char initialChar = label.charAt(0);

        appendIndicatorFeature(sb, capitalized, Character.isUpperCase(initialChar), FeatureClass.Capitalized);
        appendIndicatorFeature(sb, allcaps, Character.isUpperCase(initialChar) && label.equals(label.toUpperCase()),
                FeatureClass.AllCaps);
        appendIndicatorFeature(sb, hyphenated, label.indexOf('-') >= 0, FeatureClass.Hyphenated);
        appendIndicatorFeature(sb, initialNumeric, Character.isDigit(initialChar), FeatureClass.InitialNumeric);

        if (numeric) {
            if (Character.isDigit(initialChar)) {
                try {
                    Integer.parseInt(label);
                    sb.append(FeatureClass.Numeric);
                    sb.append(featureDelimiter);
                } catch (final NumberFormatException ignore) {
                    if (labelIndicatorNegations) {
                        sb.append(FeatureClass.NEGATION + FeatureClass.Numeric);
                        sb.append(featureDelimiter);
                    }
                }
            } else if (labelIndicatorNegations) {
                sb.append(FeatureClass.NEGATION + FeatureClass.Numeric);
                sb.append(featureDelimiter);
            }
        }

        appendIndicatorFeature(sb, endsWithLy, label.endsWith("ly"), FeatureClass.EndsWithLy);
        appendIndicatorFeature(sb, endsWithIng, label.endsWith("ing"), FeatureClass.EndsWithIng);
        appendIndicatorFeature(sb, endsWithEd, label.endsWith("ed"), FeatureClass.EndsWithEd);
        appendIndicatorFeature(sb, endsWithEr, label.endsWith("er"), FeatureClass.EndsWithEr);
        appendIndicatorFeature(sb, endsWithEst, label.endsWith("est"), FeatureClass.EndsWithEst);
        appendIndicatorFeature(sb, endsWithBle, label.endsWith("ble"), FeatureClass.EndsWithBle);
        appendIndicatorFeature(sb, endsWithIve, label.endsWith("ive"), FeatureClass.EndsWithIve);
        appendIndicatorFeature(sb, endsWithIc, label.endsWith("ic"), FeatureClass.EndsWithIc);
        appendIndicatorFeature(sb, endsWithAl, label.endsWith("al"), FeatureClass.EndsWithAl);

        if (length) {
            switch (label.length()) {
            case 1:
                sb.append(FeatureClass.Length1);
                break;
            case 2:
                sb.append(FeatureClass.Length2);
                break;
            case 3:
                sb.append(FeatureClass.Length3);
                break;
            case 4:
                sb.append(FeatureClass.Length4);
                break;
            case 5:
            case 6:
                sb.append(FeatureClass.Length5to6);
                break;
            case 7:
            case 8:
                sb.append(FeatureClass.Length7to8);
                break;
            case 9:
            case 10:
            case 11:
            case 12:
                sb.append(FeatureClass.Length9to12);
                break;
            case 13:
            case 14:
            case 15:
            case 16:
            case 17:
            case 18:
                sb.append(FeatureClass.Length13to18);
                break;
            default:
                sb.append(FeatureClass.LengthGreaterThan18);

            }
            sb.append(featureDelimiter);
        }
    }

    private void appendWord(final StringBuilder sb, final String label) {
        if (includeWord) {
            sb.append(label);
            sb.append(featureDelimiter);
        }

        if (includeLowercaseWord) {
            sb.append(label.toLowerCase());
            sb.append(featureDelimiter);
        }

        if (includeStem) {
            sb.append(FeatureClass.Stem);
            // TODO Make porterStemmer an instance variable, after making PorterStemmer thread-safe
            final PorterStemmer porterStemmer = new PorterStemmer();
            sb.append(porterStemmer.stemWord(label.toLowerCase()));
            sb.append(featureDelimiter);
        }
    }

    private void appendPos(final StringBuilder sb, final String posLabel) {
        if (pos) {
            sb.append(FeatureClass.Pos);
            sb.append(posLabel);
            sb.append(featureDelimiter);
        }

        if (plainPos) {
            sb.append(posLabel);
            sb.append(featureDelimiter);
        }
    }

    /**
     * Select features out of a flat structure.
     * 
     * @param line
     * @return Feature string
     */
    private String selectFlatFeatures(final String line) {
        final StringBuilder sb = new StringBuilder(512);
        String[][] features;

        int posIndex = -1;
        switch (inputFormat) {
        case Bracketed:
            features = Strings.bracketedTags(line);
            break;
        case SquareBracketed:
            features = Strings.squareBracketedTags(line);
            break;
        case PosTagged:
            features = Strings.bracketedTags(line);
            wordIndex = 2;
            posIndex = 1;
            break;
        case SquarePosTagged:
            features = Strings.squareBracketedTags(line);
            wordIndex = 2;
            posIndex = 1;
            break;
        case Stanford:
            // Remove leading and trailing whitespace and split by spaces
            final String[] split = line.trim().split(" +");

            features = new String[split.length][];
            for (int i = 0; i < split.length; i++) {
                features[i] = split[i].split("/");
            }
            break;
        default:
            throw new RuntimeException("Unexpected inputFormat: " + inputFormat);
        }

        for (int i = 0; i < features.length; i++) {
            // Selecting by the specified 'word'
            final String word = features[i][wordIndex - 1];
            final String posLabel = posIndex > 0 ? features[i][posIndex - 1] : null;

            sb.append(beginBracket);
            appendWord(sb, word);
            appendPos(sb, posLabel);
            appendWordFeatures(sb, word);

            if (startWord && i == 0) {
                sb.append(String.format("%s%s", FeatureClass.StartWord, featureDelimiter));
            }

            if (endWord && i == features.length - 1) {
                sb.append(String.format("%s%s", FeatureClass.EndWord, featureDelimiter));
            }

            // Previous words
            for (int j = 1; ((j <= previousWords) && (i - j >= 0)); j++) {
                sb.append(String.format("%s%d_%s%s", FeatureClass.PreviousWord, j, features[i - j][wordIndex - 1],
                        featureDelimiter));
            }

            // Subsequent words
            for (int j = 1; ((j <= subsequentWords) && (i + j < features.length)); j++) {
                sb.append(String.format("%s%d_%s%s", FeatureClass.SubsequentWord, j, features[i + j][wordIndex - 1],
                        featureDelimiter));
            }

            // Previous POS
            for (int j = 1; ((j <= previousPos) && (i - j >= 0)); j++) {
                sb.append(String.format("%s%d_%s%s", FeatureClass.PreviousPos, j, features[i - j][posIndex - 1],
                        featureDelimiter));
            }

            // Subsequent POS
            for (int j = 1; ((j <= subsequentPos) && (i + j < features.length)); j++) {
                sb.append(String.format("%s%d_%s%s", FeatureClass.SubsequentPos, j, features[i + j][posIndex - 1],
                        featureDelimiter));
            }

            if (selectedFeatures != null) {
                for (int j = 0; j < selectedFeatures.length; j++) {
                    sb.append(features[i][selectedFeatures[j]]);
                    sb.append(featureDelimiter);
                }
            }

            // Remove the final (extra) delimiter
            sb.delete(sb.length() - featureDelimiter.length(), sb.length());

            // End the bracket and token
            sb.append(endBracket);
            sb.append(' ');
        }

        // Delete the final trailing space
        sb.deleteCharAt(sb.length() - 1);

        return sb.toString();
    }

    @Override
    protected FutureTask<String> lineTask(final String line) {
        return new FutureTask<String>(new Callable<String>() {

            @Override
            public String call() {
                if (inputFormat == FileFormat.BracketedTree) {
                    return selectTreeFeatures(line);
                } else if (inputFormat == FileFormat.SquareBracketedTree) {
                    return selectTreeFeatures(line.replaceAll("\\[", "(").replaceAll("\\]", ")"));
                } else {
                    return selectFlatFeatures(line);
                }
            }
        });
    }

    public enum FileFormat {

        /** Penn Treebank format parenthesis-bracketed hierarchical tree */
        BracketedTree("tree", "bracketed-tree"),

        /** Square-bracketed tree format (same as {@link #BracketedTree} but using square brackets) */
        SquareBracketedTree("square-bracketed-tree"),

        /** Parenthesis-bracketed flat format: (feature1 feature2 feature3...) (feature1 feature2 feature3...)... */
        Bracketed("bracketed"),

        /** Square-bracketed flat format (same as {@link #Bracketed} but using square brackets) */
        SquareBracketed("square-bracketed"),

        /** Parenthesis-bracketed flat format: (POS word feature3 ...) (POS word feature3 ...) ... */
        PosTagged("pos-tagged"),

        /** Parenthesis-bracketed flat format: (same as {@link #PosTagged} but using square brackets) */
        SquarePosTagged("square-pos-tagged"),

        /** Slash-delimited flat format: feature1/feature2/feature3 feature1/feature2/feature3 ... */
        Stanford("stanford");

        private FileFormat(final String... aliases) {
            EnumAliasMap.singleton().addAliases(this, aliases);
        }

        public boolean isTreeFormat() {
            return this == BracketedTree || this == SquareBracketedTree;
        }

        public boolean isFlatFormat() {
            return this == Bracketed || this == SquareBracketed || this == PosTagged || this == SquarePosTagged
                    || this == Stanford;
        }
    }

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
        HeadVerb("_head_verb"),
        FirstVerb("_first_verb"),
        BeforeHead("_before_head"),
        AfterHead("_after_head"),
        BeginSentence("_begin_sent"),

        Capitalized("_capitalized"),
        AllCaps("_all_caps"),
        Hyphenated("_hyphenated"),
        Numeric("_numeric"),
        InitialNumeric("_initial_numeric"),
        StartWord("_start_word"),
        EndWord("_end_word"),

        EndsWithAl("_endswith_al"),
        EndsWithBle("_endswith_ble"),
        EndsWithEd("_endswith_ed"),
        EndsWithEr("_endswith_er"),
        EndsWithEst("_endswith_est"),
        EndsWithIc("_endswith_ic"),
        EndsWithIng("_endswith_ing"),
        EndsWithIve("_endswith_ive"),
        EndsWithLy("_endswith_ly"),

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
        Length1("_length_1"),
        Length2("_length_2"),
        Length3("_length_3"),
        Length4("_length_4"),
        Length5to6("_length_5_to_6"),
        Length7to8("_length_7_to_8"),
        Length9to12("_length_9_to_12"),
        Length13to18("_length_13_to_18"),
        LengthGreaterThan18("_length_greater_than_18"),

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
            private static final long serialVersionUID = 1L;

            private static FeatureClassMap singleton = new FeatureClassMap();
        }
    }
}

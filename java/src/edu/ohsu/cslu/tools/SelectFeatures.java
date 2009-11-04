package edu.ohsu.cslu.tools;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import cltool.LinewiseCommandlineTool;
import edu.ohsu.cslu.common.FeatureClass;
import edu.ohsu.cslu.common.PorterStemmer;
import edu.ohsu.cslu.datastructs.narytree.HeadPercolationRuleset;
import edu.ohsu.cslu.datastructs.narytree.MsaHeadPercolationRuleset;
import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.datastructs.narytree.StringNaryTree;
import edu.ohsu.cslu.util.Strings;
import static edu.ohsu.cslu.tools.LinguisticToolOptions.*;

/**
 * Selects and formats features from a variously formatted sentences (including Penn-Treebank parse
 * trees, parenthesis-bracketed flat structures, and Stanford's slash-delimited tagged
 * representation).
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
 * From flat bracketed input, arbitrary features can be selected in any order (e.g. features 1, 6,
 * and 3 output in that order).
 * 
 * TODO: Split out flat 'reordering' version and implement w, p, pw, sw, pp, sp for flat text.
 * 
 * @author Aaron Dunlop
 * @since Nov 17, 2008
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class SelectFeatures extends LinewiseCommandlineTool
{
    @Option(name = "-i", aliases = {"--input-format"}, metaVar = "format (tree|bracketed|square-bracketed|stanford)", usage = "Input format. Default = bracketed.")
    private FileFormat inputFormat = FileFormat.Bracketed;

    @Option(name = "-o", aliases = {"--output-format"}, metaVar = "format (tree|bracketed|square-bracketed|stanford)", usage = "Output format. Default = bracketed.")
    private FileFormat outputFormat = FileFormat.Bracketed;

    /** TreeBank POS prefixed with 'pos_' e.g. pos_DT, pos_. */
    @Option(name = "-p", aliases = {"--pos", "--part-of-speech"}, usage = "Include POS feature (_pos_...)")
    private boolean pos;

    /** TreeBank POS without a prefix e.g. DT, NN */
    @Option(name = "-ppos", aliases = {"--plain-pos"}, usage = "Include plain POS feature (without _pos_ prefix)")
    private boolean plainPos;

    @Option(name = "-w", aliases = {"--word"}, usage = "Include token")
    private boolean includeWord;

    @Option(name = "-lcw", aliases = {"--lowercase-word"}, usage = "Include lowercased token")
    private boolean includeLowercaseWord;

    @Option(name = "-stem", usage = "Include word stem (_stem_...)")
    private boolean includeStem;

    @Option(name = "-h", aliases = {"--head-verb"}, usage = "Include _head_verb feature")
    private boolean headVerb;

    // TODO This should really be a Set for efficiency
    @Option(name = "-nfv", aliases = {"--not-first-verb"}, usage = "Include _first_verb feature")
    private List<String> firstVerbPos;

    @Option(name = "-bh", aliases = {"--before-head"}, usage = "Include _before_head_verb feature")
    private boolean beforeHead;

    @Option(name = "-ah", aliases = {"--after-head"}, usage = "Include _after_head_verb feature")
    private boolean afterHead;

    @Option(name = "-cap", aliases = {"--capitalized"}, usage = "Include capitalization feature")
    private boolean capitalized;

    @Option(name = "-allcaps", aliases = {"--all-caps"}, usage = "Include all-capitalized feature")
    private boolean allcaps;

    @Option(name = "-hyphen", aliases = {"--hyphenated"}, usage = "Include hyphenization feature")
    private boolean hyphenated;

    @Option(name = "-length", aliases = {"--word-length"}, usage = "Include length features")
    private boolean length;

    @Option(name = "-prevword", aliases = {"--previous-words"}, metaVar = "count", usage = "Include previous words")
    private int previousWords;

    @Option(name = "-subword", aliases = {"--subsequent-words"}, metaVar = "count", usage = "Include subsequent words")
    private int subsequentWords;

    @Option(name = "-prevpos", aliases = {"--previous-pos"}, metaVar = "count", usage = "Include pos for previous words")
    private int previousPos;

    @Option(name = "-subpos", aliases = {"--subsequent-pos"}, metaVar = "count", usage = "Include pos for subsequent words")
    private int subsequentPos;

    @Option(name = "-f", aliases = {"--features"}, separator = ",", metaVar = "index", usage = "Feature index (in bracketed input) of token to treat as the word (starting with 1) Default = 1")
    private int[] selectedFeatures;

    private String beginBracket;
    private String endBracket;
    private String featureDelimiter;

    @Option(name = "-wi", aliases = {"--word-index"}, metaVar = "index", usage = "Feature index (in bracketed input) of token to treat as the word (starting with 1) Default = 1")
    private int wordIndex = 1;

    // TODO: Allow other head-percolation rulesets?
    private final HeadPercolationRuleset ruleset = new MsaHeadPercolationRuleset();

    private final PorterStemmer porterStemmer = new PorterStemmer();

    /** Features only supported when input is in tree format */
    static HashSet<String> TREE_FEATURES = new HashSet<String>();

    static
    {
        TREE_FEATURES.add(OPTION_POS); // pos

        TREE_FEATURES.add(OPTION_HEAD_VERB); // head_verb
        TREE_FEATURES.add(OPTION_BEFORE_HEAD); // before_head
        TREE_FEATURES.add(OPTION_AFTER_HEAD); // after_head

        TREE_FEATURES.add(OPTION_PREVIOUS_WORD); // previous word (word-n_foo)
        TREE_FEATURES.add(OPTION_SUBSEQUENT_WORD); // subsequent word (word+n_bar)
        TREE_FEATURES.add(OPTION_PREVIOUS_POS); // previous pos (pos-n_NNP)
        TREE_FEATURES.add(OPTION_SUBSEQUENT_POS); // subsequent pos (pos+n_NN)
    }

    public static void main(final String[] args)
    {
        run(args);
    }

    @Override
    public void setup(final CmdLineParser parser) throws CmdLineException
    {
        // // If input is not in tree format, we cannot extract certain features
        // if (inputFormat != FileFormat.BracketedTree && inputFormat !=
        // FileFormat.SquareBracketedTree)
        // {
        // for (Option o : commandLine.getOptions())
        // {
        // if (TREE_FEATURES.contains(o.getOpt()))
        // {
        // throw new ParseException(o.getOpt() + " option only supported for tree input formats");
        // }
        // }
        // }

        if (selectedFeatures != null && selectedFeatures.length > 0 && wordIndex != 1)
        {
            throw new CmdLineException(parser, "Cannot select both feature indices and word index");
        }

        // And 'f' option when selecting from a flat format
        if (inputFormat != FileFormat.Bracketed && inputFormat != FileFormat.SquareBracketed
            && inputFormat != FileFormat.Stanford)
        {
            if (selectedFeatures != null && selectedFeatures.length > 0)
            {
                throw new CmdLineException(parser, "Field selection is only supported for flat input formats");
            }
        }

        switch (outputFormat)
        {
            case Bracketed :
                beginBracket = "(";
                endBracket = ")";
                featureDelimiter = " ";
                break;
            case SquareBracketed :
                beginBracket = "[";
                endBracket = "]";
                featureDelimiter = " ";
                break;
            case Stanford :
                beginBracket = "";
                endBracket = "";
                featureDelimiter = "/";
                break;
        }

        if (selectedFeatures != null)
        {
            for (int i = 0; i < selectedFeatures.length; i++)
            {
                // Command-line feature argument starts indexing with 1, but we want to index
                // starting with 0
                selectedFeatures[i]--;
            }
        }
    }

    private String selectTreeFeatures(final String parsedSentence)
    {
        final StringNaryTree tree = StringNaryTree.read(parsedSentence);
        final StringBuilder sb = new StringBuilder(parsedSentence.length());

        // Start with before_head
        int headPosition = 0;
        boolean foundFirstVerb = false;

        // Construct a flattened representation
        final ArrayList<String> wordList = new ArrayList<String>(64);
        final ArrayList<String> posList = new ArrayList<String>(64);
        for (final Iterator<NaryTree<String>> iter = tree.inOrderIterator(); iter.hasNext();)
        {
            final StringNaryTree node = (StringNaryTree) iter.next();

            if (node.isLeaf())
            {
                wordList.add(node.stringLabel().toLowerCase());
                posList.add(node.parent().stringLabel());
            }
        }

        int i = 0;
        for (final Iterator<NaryTree<String>> iter = tree.inOrderIterator(); iter.hasNext();)
        {
            final StringNaryTree node = (StringNaryTree) iter.next();
            final String posLabel = node.parent() != null ? node.parent().stringLabel() : null;

            if (node.isLeaf())
            {
                sb.append(beginBracket);

                final String label = node.stringLabel();
                appendWord(sb, label);

                if (pos)
                {
                    sb.append(FeatureClass.PREFIX_POS);
                    sb.append(posLabel);
                    sb.append(featureDelimiter);
                }

                if (plainPos)
                {
                    sb.append(node.parent().stringLabel());
                    sb.append(featureDelimiter);
                }

                appendWordFeatures(sb, label);

                // Previous words
                for (int j = 1; ((j <= previousWords) && (i - j >= 0)); j++)
                {
                    sb.append(String.format("%s%d_%s%s", FeatureClass.PREFIX_PREVIOUS_WORD, j, wordList.get(i - j),
                        featureDelimiter));
                }

                // Subsequent words
                for (int j = 1; ((j <= subsequentWords) && (i + j < wordList.size())); j++)
                {
                    sb.append(String.format("%s%d_%s%s", FeatureClass.PREFIX_SUBSEQUENT_WORD, j, wordList.get(i + j),
                        featureDelimiter));
                }

                // Previous POS
                for (int j = 1; ((j <= previousPos) && (i - j >= 0)); j++)
                {
                    sb.append(String.format("%s%d_%s%s", FeatureClass.PREFIX_PREVIOUS_POS, j, posList.get(i - j),
                        featureDelimiter));
                }

                // Subsequent POS
                for (int j = 1; ((j <= subsequentPos) && (i + j < posList.size())); j++)
                {
                    sb.append(String.format("%s%d_%s%s", FeatureClass.PREFIX_SUBSEQUENT_POS, j, posList.get(i + j),
                        featureDelimiter));
                }

                if (node.isHeadOfTreeRoot(ruleset))
                {
                    if (headVerb)
                    {
                        sb.append(FeatureClass.FEATURE_HEAD_VERB);
                        sb.append(featureDelimiter);
                    }
                    // Switch feature tag to after_head
                    headPosition = 1;
                }
                else if (beforeHead && headPosition == 0)
                {
                    sb.append(FeatureClass.FEATURE_BEFORE_HEAD);
                    sb.append(featureDelimiter);
                }
                else if (afterHead && headPosition == 1)
                {
                    sb.append(FeatureClass.FEATURE_AFTER_HEAD);
                    sb.append(featureDelimiter);
                }

                if (firstVerbPos != null)
                {
                    if (posLabel != null && !foundFirstVerb && firstVerbPos.contains(posLabel))
                    {
                        foundFirstVerb = true;
                    }
                    else
                    {
                        sb.append(FeatureClass.FEATURE_NOT_FIRST_VERB);
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

    private void appendWordFeatures(final StringBuilder sb, final String label)
    {
        if (capitalized && Character.isUpperCase(label.charAt(0)))
        {
            sb.append(FeatureClass.FEATURE_CAPITALIZED);
            sb.append(featureDelimiter);
        }

        if (allcaps && Character.isUpperCase(label.charAt(0)) && label.equals(label.toUpperCase()))
        {
            sb.append(FeatureClass.FEATURE_ALL_CAPS);
            sb.append(featureDelimiter);
        }

        if (hyphenated && label.indexOf('-') >= 0)
        {
            sb.append(FeatureClass.FEATURE_HYPHENATED);
            sb.append(featureDelimiter);
        }

        if (length)
        {
            final int l = label.length();
            if (l == 1)
            {
                sb.append(FeatureClass.FEATURE_LENGTH_1);
            }
            else if (l <= 5)
            {
                sb.append(FeatureClass.FEATURE_LENGTH_2_TO_5);
            }
            else if (l <= 10)
            {
                sb.append(FeatureClass.FEATURE_LENGTH_6_TO_10);
            }
            else
            {
                sb.append(FeatureClass.FEATURE_LENGTH_GREATER_THAN_10);
            }
            sb.append(featureDelimiter);
        }
    }

    private void appendWord(final StringBuilder sb, final String label)
    {
        if (includeWord)
        {
            sb.append(label);
            sb.append(featureDelimiter);
        }

        if (includeLowercaseWord)
        {
            sb.append(label.toLowerCase());
            sb.append(featureDelimiter);
        }

        if (includeStem)
        {
            sb.append(FeatureClass.PREFIX_STEM);
            sb.append(porterStemmer.stemWord(label.toLowerCase()));
            sb.append(featureDelimiter);
        }
    }

    /**
     * Select features out of a flat structure.
     * 
     * @param line
     * @return Feature string
     */
    private String selectFlatFeatures(final String line)
    {
        final StringBuilder sb = new StringBuilder(line.length());
        String[][] features;

        switch (inputFormat)
        {
            case Bracketed :
                features = Strings.bracketedTags(line);
                break;
            case SquareBracketed :
                features = Strings.squareBracketedTags(line);
                break;
            case Stanford :
                // Remove leading and trailing whitespace and split by spaces
                final String[] split = line.trim().split(" +");

                features = new String[split.length][];
                for (int i = 0; i < split.length; i++)
                {
                    features[i] = split[i].split("/");
                }
                break;
            default :
                throw new RuntimeException("Unexpected inputFormat: " + inputFormat);
        }

        if (selectedFeatures != null)
        {
            for (int i = 0; i < features.length; i++)
            {
                sb.append(beginBracket);
                for (int j = 0; j < selectedFeatures.length; j++)
                {
                    sb.append(features[i][selectedFeatures[j]]);
                    sb.append(featureDelimiter);
                }

                // Remove the final (extra) delimiter
                sb.delete(sb.length() - featureDelimiter.length(), sb.length());

                // End the bracket and token
                sb.append(endBracket);
                sb.append(' ');
            }
        }
        else
        {
            for (int i = 0; i < features.length; i++)
            {
                // Selecting by the specified 'word'
                final String word = features[i][wordIndex - 1];

                sb.append(beginBracket);
                appendWord(sb, word);
                appendWordFeatures(sb, word);

                // Remove the final (extra) delimiter
                sb.delete(sb.length() - featureDelimiter.length(), sb.length());

                // End the bracket and token
                sb.append(endBracket);
                sb.append(' ');
            }
        }
        // Delete the final trailing space
        sb.deleteCharAt(sb.length() - 1);

        return sb.toString();
    }

    @Override
    protected Callable<String> lineTask(final String line)
    {
        return new Callable<String>()
        {
            @Override
            public String call()
            {
                if (inputFormat == FileFormat.BracketedTree)
                {
                    return selectTreeFeatures(line);
                }
                else if (inputFormat == FileFormat.SquareBracketedTree)
                {
                    return selectTreeFeatures(line.replaceAll("\\[", "(").replaceAll("\\]", ")"));
                }
                else
                {
                    return selectFlatFeatures(line);
                }
            }
        };
    }
}

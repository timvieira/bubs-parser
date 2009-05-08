package edu.ohsu.cslu.tools;

import static edu.ohsu.cslu.tools.LinguisticToolOptions.OPTION_AFTER_HEAD;
import static edu.ohsu.cslu.tools.LinguisticToolOptions.OPTION_BEFORE_HEAD;
import static edu.ohsu.cslu.tools.LinguisticToolOptions.OPTION_CAP;
import static edu.ohsu.cslu.tools.LinguisticToolOptions.OPTION_HEAD_VERB;
import static edu.ohsu.cslu.tools.LinguisticToolOptions.OPTION_PLAIN_POS;
import static edu.ohsu.cslu.tools.LinguisticToolOptions.OPTION_POS;
import static edu.ohsu.cslu.tools.LinguisticToolOptions.OPTION_PREVIOUS_POS;
import static edu.ohsu.cslu.tools.LinguisticToolOptions.OPTION_PREVIOUS_WORD;
import static edu.ohsu.cslu.tools.LinguisticToolOptions.OPTION_SUBSEQUENT_POS;
import static edu.ohsu.cslu.tools.LinguisticToolOptions.OPTION_SUBSEQUENT_WORD;
import static edu.ohsu.cslu.tools.LinguisticToolOptions.OPTION_WORD;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import edu.ohsu.cslu.common.FeatureClass;
import edu.ohsu.cslu.common.tools.LinewiseCommandlineTool;
import edu.ohsu.cslu.datastructs.narytree.HeadPercolationRuleset;
import edu.ohsu.cslu.datastructs.narytree.MsaHeadPercolationRuleset;
import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.datastructs.narytree.StringNaryTree;
import edu.ohsu.cslu.util.Strings;

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
    private FileFormat inputFormat;
    private FileFormat outputFormat;

    /** TreeBank POS prefixed with 'pos_' e.g. pos_DT, pos_. */
    private boolean pos;

    /** TreeBank POS without a prefix e.g. DT, NN */
    private boolean plainPos;

    private boolean headVerb;
    private Set<String> firstVerbPos;
    private boolean beforeHead;
    private boolean afterHead;
    private boolean word;
    private boolean retainCapitalization;
    private int previousWords;
    private int subsequentWords;
    private int previousPos;
    private int subsequentPos;

    private int[] selectedFeatures;

    private String beginBracket;
    private String endBracket;
    private String featureDelimiter;

    // TODO: Allow other head-percolation rulesets?
    private final HeadPercolationRuleset ruleset = new MsaHeadPercolationRuleset();

    private final static String OPTION_INPUT_FORMAT = "i";
    private final static String OPTION_OUTPUT_FORMAT = "o";
    private final static String OPTION_NOT_FIRST_VERB = "nfv";

    /** Features only supported when input is in tree format */
    static HashSet<String> TREE_FEATURES = new HashSet<String>();

    static
    {
        TREE_FEATURES.add(OPTION_WORD); // word
        TREE_FEATURES.add(OPTION_CAP); // retain capitalization
        TREE_FEATURES.add(OPTION_POS); // pos

        TREE_FEATURES.add(OPTION_HEAD_VERB); // head_verb
        TREE_FEATURES.add(OPTION_BEFORE_HEAD); // before_head
        TREE_FEATURES.add(OPTION_AFTER_HEAD); // after_head

        TREE_FEATURES.add(OPTION_PREVIOUS_WORD); // previous word (word-n_foo)
        TREE_FEATURES.add(OPTION_SUBSEQUENT_WORD); // subsequent word (word+n_bar)
        TREE_FEATURES.add(OPTION_PREVIOUS_POS); // previous pos (pos-n_NNP)
        TREE_FEATURES.add(OPTION_SUBSEQUENT_POS); // subsequent pos (pos+n_NN)
    }

    public static void main(String[] args)
    {
        run(args);
    }

    @Override
    @SuppressWarnings("static-access")
    protected Options options() throws Exception
    {
        Options options = basicOptions();

        options.addOption(OptionBuilder.hasArg().withDescription(
            "Input format (tree,bracketed,square-bracketed,stanford; default=bracketed)").withLongOpt("input-format")
            .create(OPTION_INPUT_FORMAT));
        options.addOption(OptionBuilder.hasArg().withDescription(
            "Output format (bracketed, square-bracketed, stanford; default=bracketed)").withLongOpt("output-format")
            .create(OPTION_OUTPUT_FORMAT));

        options.addOption(OptionBuilder.withDescription("Extract prefixed POS feature (_pos_...)").create(OPTION_POS));
        options.addOption(OptionBuilder.withDescription("Extract plain POS feature (without prefix)").create(
            OPTION_PLAIN_POS));
        options.addOption(OptionBuilder.withDescription("Extract _head_verb feature").create(OPTION_HEAD_VERB));
        options.addOption(OptionBuilder.hasArgs().withValueSeparator(',').withArgName("verbs").withDescription(
            "Extract _first_verb feature").create(OPTION_NOT_FIRST_VERB));
        options
            .addOption(OptionBuilder.withDescription("Extract _before_head_verb feature").create(OPTION_BEFORE_HEAD));
        options.addOption(OptionBuilder.withDescription("Extract _after_head_verb feature").create(OPTION_AFTER_HEAD));
        options.addOption(OptionBuilder.withDescription("Extract word feature").create(OPTION_WORD));
        options.addOption(OptionBuilder.withDescription("Retain capitalization (default is all-lowercase)").create(
            OPTION_CAP));

        options.addOption(OptionBuilder.hasArg().withArgName("count").withDescription("Previous words").create(
            OPTION_PREVIOUS_WORD));
        options.addOption(OptionBuilder.hasArg().withArgName("count").withDescription("Subsequent words").create(
            OPTION_SUBSEQUENT_WORD));
        options.addOption(OptionBuilder.hasArg().withArgName("count").withDescription("Previous POS").create(
            OPTION_PREVIOUS_POS));
        options.addOption(OptionBuilder.hasArg().withArgName("count").withDescription("Subsequent POS").create(
            OPTION_SUBSEQUENT_POS));

        options.addOption(OptionBuilder.hasArgs().withValueSeparator(',').withArgName("features").withDescription(
            "list (1..n, comma-separated)").create('f'));

        return options;
    }

    @Override
    public void setToolOptions(CommandLine commandLine) throws ParseException
    {
        inputFormat = commandLine.hasOption(OPTION_INPUT_FORMAT) ? FileFormat.forString(commandLine
            .getOptionValue(OPTION_INPUT_FORMAT)) : FileFormat.Bracketed;
        outputFormat = commandLine.hasOption(OPTION_OUTPUT_FORMAT) ? FileFormat.forString(commandLine
            .getOptionValue(OPTION_OUTPUT_FORMAT)) : FileFormat.Bracketed;

        // If input is not in tree format, we cannot extract certain features
        if (inputFormat != FileFormat.BracketedTree && inputFormat != FileFormat.SquareBracketedTree)
        {
            for (Option o : commandLine.getOptions())
            {
                if (TREE_FEATURES.contains(o.getOpt()))
                {
                    throw new ParseException(o.getOpt() + " option only supported for tree input formats");
                }
            }
        }

        // And 'f' option when selecting from a flat format
        if (inputFormat != FileFormat.Bracketed && inputFormat != FileFormat.SquareBracketed
            && inputFormat != FileFormat.Stanford && commandLine.hasOption('f'))
        {
            throw new ParseException("Feature option (-f) only supported for flat input formats");
        }

        pos = commandLine.hasOption(OPTION_POS);
        plainPos = commandLine.hasOption(OPTION_PLAIN_POS);
        headVerb = commandLine.hasOption(OPTION_HEAD_VERB);

        if (commandLine.hasOption(OPTION_NOT_FIRST_VERB))
        {
            firstVerbPos = new HashSet<String>();
            for (final String verbLabel : commandLine.getOptionValues(OPTION_NOT_FIRST_VERB))
            {
                firstVerbPos.add(verbLabel);
            }
        }

        beforeHead = commandLine.hasOption(OPTION_BEFORE_HEAD);
        afterHead = commandLine.hasOption(OPTION_AFTER_HEAD);
        word = commandLine.hasOption(OPTION_WORD);

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
            default :
                throw new ParseException("Unknown output format: " + commandLine.getOptionValue('o'));
        }

        if (commandLine.hasOption('f'))
        {
            final String[] features = commandLine.getOptionValues('f');
            selectedFeatures = new int[features.length];
            for (int i = 0; i < selectedFeatures.length; i++)
            {
                // Command-line feature argument starts indexing with 1, but we want to index
                // starting with 0
                selectedFeatures[i] = Integer.parseInt(features[i]) - 1;
            }
        }

        retainCapitalization = commandLine.hasOption(OPTION_CAP);

        // Previous and subsequent words / POS
        if (commandLine.hasOption(OPTION_PREVIOUS_WORD))
        {
            previousWords = Integer.parseInt(commandLine.getOptionValue(OPTION_PREVIOUS_WORD));
        }
        if (commandLine.hasOption(OPTION_SUBSEQUENT_WORD))
        {
            subsequentWords = Integer.parseInt(commandLine.getOptionValue(OPTION_SUBSEQUENT_WORD));
        }
        if (commandLine.hasOption(OPTION_PREVIOUS_POS))
        {
            previousPos = Integer.parseInt(commandLine.getOptionValue(OPTION_PREVIOUS_POS));
        }
        if (commandLine.hasOption(OPTION_SUBSEQUENT_POS))
        {
            subsequentPos = Integer.parseInt(commandLine.getOptionValue(OPTION_SUBSEQUENT_POS));
        }
    }

    @Override
    protected String usageArguments() throws Exception
    {
        return "[filename]";
    }

    private String selectTreeFeatures(String parsedSentence)
    {
        StringNaryTree tree = StringNaryTree.read(parsedSentence);
        StringBuilder sb = new StringBuilder(parsedSentence.length());

        // Start with before_head
        int headPosition = 0;
        boolean foundFirstVerb = false;

        // Construct a flattened representation
        ArrayList<String> wordList = new ArrayList<String>(64);
        ArrayList<String> posList = new ArrayList<String>(64);
        for (Iterator<NaryTree<String>> iter = tree.inOrderIterator(); iter.hasNext();)
        {
            StringNaryTree node = (StringNaryTree) iter.next();

            if (node.isLeaf())
            {
                wordList.add(node.stringLabel().toLowerCase());
                posList.add(node.parent().stringLabel());
            }
        }

        int i = 0;
        for (Iterator<NaryTree<String>> iter = tree.inOrderIterator(); iter.hasNext();)
        {
            StringNaryTree node = (StringNaryTree) iter.next();
            final String posLabel = node.parent() != null ? node.parent().stringLabel() : null;

            if (node.isLeaf())
            {
                sb.append(beginBracket);

                if (word)
                {
                    sb.append(retainCapitalization ? node.stringLabel() : node.stringLabel().toLowerCase());
                    sb.append(featureDelimiter);
                }

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

    /**
     * Select features out of a flat structure.
     * 
     * @param line
     * @return Feature string
     */
    private String selectFlatFeatures(String line)
    {
        StringBuilder sb = new StringBuilder(line.length());
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
                String[] split = line.trim().split(" +");

                features = new String[split.length][];
                for (int i = 0; i < split.length; i++)
                {
                    features[i] = split[i].split("/");
                }
                break;
            default :
                throw new RuntimeException("Unexpected inputFormat: " + inputFormat);
        }

        for (int i = 0; i < features.length; i++)
        {
            sb.append(beginBracket);
            for (int j = 0; j < selectedFeatures.length - 1; j++)
            {
                sb.append(features[i][selectedFeatures[j]]);
                sb.append(featureDelimiter);
            }
            sb.append(features[i][selectedFeatures[selectedFeatures.length - 1]]);

            // End the bracket and token
            sb.append(endBracket);
            sb.append(' ');
        }

        // Delete the final trailing space
        sb.deleteCharAt(sb.length() - 1);

        return sb.toString();
    }

    @Override
    protected LineTask lineTask(String line)
    {
        return new LineTask(line)
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

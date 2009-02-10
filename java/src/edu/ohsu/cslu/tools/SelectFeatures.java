package edu.ohsu.cslu.tools;

import java.util.Iterator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import edu.ohsu.cslu.common.tools.LinewiseCommandlineTool;
import edu.ohsu.cslu.parsing.trees.HeadPercolationRuleset;
import edu.ohsu.cslu.parsing.trees.MsaHeadPercolationRuleset;
import edu.ohsu.cslu.parsing.trees.NaryTree;
import edu.ohsu.cslu.parsing.trees.StringNaryTree;
import edu.ohsu.cslu.util.Strings;

/**
 * Selects and formats features from a variously formatted sentences (including Penn-Treebank trees,
 * parenthesis-bracketed flat structures, and Stanford's slash-delimited tagged representation.
 * 
 * Outputs flat structures in bracketed or Stanford-formats.
 * 
 * @author Aaron Dunlop
 * @since Nov 17, 2008
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class SelectFeatures extends LinewiseCommandlineTool
{
    private Format inputFormat;
    private Format outputFormat;
    private boolean pos;
    private boolean head;
    private boolean word;
    private int[] selectedFeatures;

    private String beginBracket;
    private String endBracket;
    private String featureDelimiter;

    private final static String HEAD_FEATURE_BEFORE = "BEFORE";
    private final static String HEAD_FEATURE_HEAD = "HEAD";
    private final static String HEAD_FEATURE_AFTER = "AFTER";

    // TODO: Allow other optional rulesets?
    private final HeadPercolationRuleset ruleset = new MsaHeadPercolationRuleset();

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
            .create('i'));
        options.addOption(OptionBuilder.hasArg().withDescription(
            "Output format (bracketed, square-bracketed, stanford; default=bracketed)").withLongOpt("output-format")
            .create('o'));

        options.addOption(OptionBuilder.withDescription("Extract POS feature").create('p'));
        options.addOption(OptionBuilder.withDescription("Extract BEFORE/HEAD/AFTER sentence-head feature").create('h'));
        options.addOption(OptionBuilder.withDescription("Extract word").create('w'));

        options.addOption(OptionBuilder.hasArgs().withValueSeparator(',').withArgName("features").withDescription(
            "list (1..n, comma-separated)").create('f'));

        return options;
    }

    @Override
    public void setToolOptions(CommandLine commandLine) throws ParseException
    {
        inputFormat = commandLine.hasOption('i') ? Format.inputFormat(commandLine.getOptionValue('i'))
            : Format.Bracketed;
        outputFormat = commandLine.hasOption('o') ? Format.inputFormat(commandLine.getOptionValue('o'))
            : Format.Bracketed;

        // We only handle 'p', 'head', and 'word' options when extracting from trees
        if (inputFormat != Format.BracketedTree && inputFormat != Format.SquareBracketedTree
            && (commandLine.hasOption('p') || commandLine.hasOption('h') || commandLine.hasOption('w')))
        {
            throw new ParseException("POS, head, and word options only supported for tree input formats");
        }

        // And 'f' option when selecting from a flat format
        if (inputFormat != Format.Bracketed && inputFormat != Format.SquareBracketed && inputFormat != Format.Stanford
            && commandLine.hasOption('f'))
        {
            throw new ParseException("Feature option (-f) only supported for flat input formats");
        }

        pos = commandLine.hasOption('p');
        head = commandLine.hasOption('h');
        word = commandLine.hasOption('w');
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
                throw new ParseException("Unknown output format");
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

        // Start with BEFORE
        String headFeature = HEAD_FEATURE_BEFORE;

        for (Iterator<NaryTree<String>> i = tree.inOrderIterator(); i.hasNext();)
        {
            StringNaryTree node = (StringNaryTree) i.next();

            if (node.isLeaf())
            {
                sb.append(beginBracket);

                if (word)
                {
                    sb.append(node.stringLabel());
                    sb.append(featureDelimiter);
                }

                if (pos)
                {
                    sb.append(node.parent().stringLabel());
                    sb.append(featureDelimiter);
                }

                if (head)
                {
                    if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '/')
                    {
                        // Replace the final trailing '/' with '-'
                        sb.deleteCharAt(sb.length() - 1);
                        sb.append('-');
                    }

                    // Switch feature tag to HEAD when appropriate, and then to AFTER on the
                    // next iteration
                    if (node.isHeadOfTreeRoot(ruleset))
                    {
                        headFeature = HEAD_FEATURE_HEAD;
                    }
                    else if (headFeature.equals(HEAD_FEATURE_HEAD))
                    {
                        headFeature = HEAD_FEATURE_AFTER;
                    }

                    sb.append(headFeature);
                    sb.append(featureDelimiter);
                }

                // Delete the final feature delimiter
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
                if (inputFormat == Format.BracketedTree)
                {
                    return selectTreeFeatures(line);
                }
                else if (inputFormat == Format.SquareBracketedTree)
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

    private enum Format
    {
        /** Penn Treebank parenthesis-bracketed format */
        BracketedTree,
        /** Square-bracketed tree format */
        SquareBracketedTree,
        /** Parenthesis-bracketed flat format */
        Bracketed,
        /** Square-bracketed flat format */
        SquareBracketed,
        /** Slash-delimited flat format */
        Stanford;

        public boolean isTreeFormat()
        {
            return this == BracketedTree || this == SquareBracketedTree;
        }

        public static Format inputFormat(String s)
        {
            if ("bracketed-tree".equalsIgnoreCase(s) || "tree".equalsIgnoreCase(s))
            {
                return BracketedTree;
            }
            if ("square-bracketed-tree".equalsIgnoreCase(s))
            {
                return SquareBracketedTree;
            }
            else if ("bracketed".equalsIgnoreCase(s))
            {
                return Bracketed;
            }
            else if ("square-bracketed".equalsIgnoreCase(s))
            {
                return SquareBracketed;
            }
            else if ("stanford".equalsIgnoreCase(s))
            {
                return Stanford;
            }
            else
            {
                throw new IllegalArgumentException("Unknown input format: " + s);
            }
        }

        public static Format outputFormat(String s)
        {
            if ("bracketed".equalsIgnoreCase(s))
            {
                return Bracketed;
            }
            else if ("square-bracketed".equalsIgnoreCase(s))
            {
                return SquareBracketed;
            }
            else if ("stanford".equalsIgnoreCase(s))
            {
                return Stanford;
            }
            else
            {
                throw new IllegalArgumentException("Unknown or illegal output format: " + s);
            }
        }
    }
}

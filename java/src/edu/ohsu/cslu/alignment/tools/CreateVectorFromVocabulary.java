package edu.ohsu.cslu.alignment.tools;

import java.io.InputStreamReader;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import edu.ohsu.cslu.alignment.LogLinearVocabulary;
import edu.ohsu.cslu.common.FeatureClass;
import edu.ohsu.cslu.common.tools.BaseCommandlineTool;
import edu.ohsu.cslu.math.linear.FloatVector;
import edu.ohsu.cslu.tools.LinguisticToolOptions;

public class CreateVectorFromVocabulary extends BaseCommandlineTool
{
    private final static String OPTION_NOT_FIRST_VERB = "nfv";

    private float gap;
    private float word;
    private float pos;
    private float notFirstVerb;
    private float headVerb;
    private float beforeHead;
    private float afterHead;
    private float previousWords;
    private float subsequentWords;
    private float previousPos;
    private float subsequentPos;

    public static void main(String[] args)
    {
        run(args);
    }

    @Override
    public void execute() throws Exception
    {
        LogLinearVocabulary vocabulary = LogLinearVocabulary.read(new InputStreamReader(System.in));
        FloatVector vector = new FloatVector(vocabulary.size());

        for (int i = 0; i < vector.length(); i++)
        {
            String mapping = vocabulary.map(i);
            switch (FeatureClass.forString(mapping))
            {
                case Gap :
                    setVectorValue(vector, i, gap, mapping);
                    break;

                case Word :
                    setVectorValue(vector, i, word, mapping);
                    break;

                case Pos :
                    setVectorValue(vector, i, pos, mapping);
                    break;

                case NotFirstVerb :
                    setVectorValue(vector, i, notFirstVerb, mapping);
                    break;

                case BeforeHead :
                    setVectorValue(vector, i, beforeHead, mapping);
                    break;

                case HeadVerb :
                    setVectorValue(vector, i, headVerb, mapping);
                    break;

                case AfterHead :
                    setVectorValue(vector, i, afterHead, mapping);
                    break;

                // case BeginSentence :
                // setVectorValue(vector, i, beginSentence, mapping);
                // break;
                // case InitialCap :
                // setVectorValue(vector, i, initialCap, mapping);
                // break;
                // case AllCaps :
                // setVectorValue(vector, i, allCaps, mapping);
                // break;

                case PreviousWord :
                    setVectorValue(vector, i, previousWords, mapping);
                    break;

                case SubsequentWord :
                    setVectorValue(vector, i, subsequentWords, mapping);
                    break;

                case PreviousPos :
                    setVectorValue(vector, i, previousPos, mapping);
                    break;

                case SubsequentPos :
                    setVectorValue(vector, i, subsequentPos, mapping);
                    break;

                default :
                    throw new IllegalArgumentException("Unknown feature class: " + mapping);
            }
        }
        System.out.print(vector.toString());
    }

    /**
     * Set the specified vector value
     * 
     * @param vector
     * @param index
     * @param value
     * @param mapping
     * @throws IllegalArgumentException if the value is unset (-1)
     */
    private void setVectorValue(FloatVector vector, int index, float value, String mapping)
    {
        if (value == -1)
        {
            throw new IllegalArgumentException("Unexpected vocabulary element: " + mapping);
        }
        vector.set(index, value);
    }

    @Override
    @SuppressWarnings("static-access")
    protected Options options() throws Exception
    {
        Options options = basicOptions();

        options.addOption(OptionBuilder.hasArg().withArgName("value").withDescription("Gap feature").create(
            LinguisticToolOptions.OPTION_GAP));
        options.addOption(OptionBuilder.hasArg().withArgName("value").withDescription("Word feature").create(
            LinguisticToolOptions.OPTION_WORD));
        options.addOption(OptionBuilder.hasArg().withArgName("value").withDescription("POS feature (_pos_...)").create(
            LinguisticToolOptions.OPTION_POS));
        options.addOption(OptionBuilder.hasArg().withArgName("value").withDescription("_first_verb feature").create(
            OPTION_NOT_FIRST_VERB));
        options.addOption(OptionBuilder.hasArg().withArgName("value").withDescription("_head_verb feature").create(
            LinguisticToolOptions.OPTION_HEAD_VERB));
        options.addOption(OptionBuilder.hasArg().withArgName("value").withDescription("_before_head_verb feature")
            .create(LinguisticToolOptions.OPTION_BEFORE_HEAD));
        options.addOption(OptionBuilder.hasArg().withArgName("value").withDescription("_after_head_verb feature")
            .create(LinguisticToolOptions.OPTION_AFTER_HEAD));

        options.addOption(OptionBuilder.hasArg().withArgName("value").withDescription("Previous word(s) features")
            .create(LinguisticToolOptions.OPTION_PREVIOUS_WORD));
        options.addOption(OptionBuilder.hasArg().withArgName("value").withDescription("Subsequent word(s) features")
            .create(LinguisticToolOptions.OPTION_SUBSEQUENT_WORD));
        options.addOption(OptionBuilder.hasArg().withArgName("value").withDescription("Previous POS features").create(
            LinguisticToolOptions.OPTION_PREVIOUS_POS));
        options.addOption(OptionBuilder.hasArg().withArgName("value").withDescription("Subsequent POS features")
            .create(LinguisticToolOptions.OPTION_SUBSEQUENT_POS));

        return options;
    }

    @Override
    public void setToolOptions(CommandLine commandLine)
    {
        gap = Float.parseFloat(commandLine.getOptionValue(LinguisticToolOptions.OPTION_GAP, "-1"));
        word = Float.parseFloat(commandLine.getOptionValue(LinguisticToolOptions.OPTION_WORD, "-1"));
        pos = Float.parseFloat(commandLine.getOptionValue(LinguisticToolOptions.OPTION_POS, "-1"));
        notFirstVerb = Float.parseFloat(commandLine.getOptionValue(OPTION_NOT_FIRST_VERB, "-1"));
        headVerb = Float.parseFloat(commandLine.getOptionValue(LinguisticToolOptions.OPTION_HEAD_VERB, "-1"));
        beforeHead = Float.parseFloat(commandLine.getOptionValue(LinguisticToolOptions.OPTION_BEFORE_HEAD, "-1"));
        afterHead = Float.parseFloat(commandLine.getOptionValue(LinguisticToolOptions.OPTION_AFTER_HEAD, "-1"));
        previousWords = Float.parseFloat(commandLine.getOptionValue(LinguisticToolOptions.OPTION_PREVIOUS_WORD, "-1"));
        subsequentWords = Float.parseFloat(commandLine.getOptionValue(LinguisticToolOptions.OPTION_SUBSEQUENT_WORD,
            "-1"));
        previousPos = Float.parseFloat(commandLine.getOptionValue(LinguisticToolOptions.OPTION_PREVIOUS_POS, "-1"));
        subsequentPos = Float.parseFloat(commandLine.getOptionValue(LinguisticToolOptions.OPTION_SUBSEQUENT_POS, "-1"));
    }

    @Override
    protected String usageArguments() throws Exception
    {
        return "[vocabulary-file]";
    }
}

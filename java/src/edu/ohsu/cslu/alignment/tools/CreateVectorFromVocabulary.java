package edu.ohsu.cslu.alignment.tools;

import java.io.InputStreamReader;

import org.kohsuke.args4j.Option;

import cltool.BaseCommandlineTool;
import edu.ohsu.cslu.alignment.LogLinearVocabulary;
import edu.ohsu.cslu.common.FeatureClass;
import edu.ohsu.cslu.datastructs.vectors.FloatVector;

public class CreateVectorFromVocabulary extends BaseCommandlineTool
{
    private final static String OPTION_NOT_FIRST_VERB = "nfv";

    @Option(name = "-g", metaVar = "weight", usage = "Gap feature weight")
    private final float gap = -1;
    @Option(name = "-w", metaVar = "weight", usage = "Word feature")
    private final float word = -1;
    @Option(name = "-s", metaVar = "weight", usage = "Stem feature")
    private final float stem = -1;
    @Option(name = "-p", metaVar = "weight", usage = "POS feature (_pos_...)")
    private final float pos = -1;
    @Option(name = "-nfv", metaVar = "weight", usage = "'Not first verb' feature")
    private final float notFirstVerb = -1;
    @Option(name = "-h", metaVar = "weight", usage = "Head verb feature (_head_verb)")
    private final float headVerb = -1;
    @Option(name = "-bh", metaVar = "weight", usage = "Before head verb feature (_before_head_verb)")
    private final float beforeHead = -1;
    @Option(name = "-ah", metaVar = "weight", usage = "After head verb feature (_after_head_verb)")
    private final float afterHead = -1;
    @Option(name = "-prevword", metaVar = "weight", usage = "Previous word(s) features")
    private final float previousWords = -1;
    @Option(name = "-subword", metaVar = "weight", usage = "Subsequent word(s) features")
    private final float subsequentWords = -1;
    @Option(name = "-prevpos", metaVar = "weight", usage = "Previous POS features")
    private final float previousPos = -1;
    @Option(name = "-subpos", metaVar = "weight", usage = "Subsequent POS features")
    private final float subsequentPos = -1;
    @Option(name = "-cap", metaVar = "weight", usage = "Capitalized word feature")
    private final float capitalized = -1;
    @Option(name = "-allcaps", metaVar = "weight", usage = "All-cap word feature")
    private final float allCaps = -1;
    @Option(name = "-hyphen", metaVar = "weight", usage = "Hyphenated word feature")
    private final float hyphenated = -1;
    @Option(name = "-length", metaVar = "weight", usage = "Word length features")
    private final float length = -1;

    public static void main(String[] args)
    {
        run(args);
    }

    @Override
    public void run() throws Exception
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
                case Unknown :
                    setVectorValue(vector, i, word, mapping);
                    break;

                case Stem :
                    setVectorValue(vector, i, stem, mapping);
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

                case Capitalized :
                    setVectorValue(vector, i, capitalized, mapping);
                    break;

                case AllCaps :
                    setVectorValue(vector, i, allCaps, mapping);
                    break;

                case Hyphenated :
                    setVectorValue(vector, i, hyphenated, mapping);
                    break;

                case Length1 :
                case Length2to5 :
                case Length6to10 :
                case LengthGreaterThan10 :
                    setVectorValue(vector, i, length, mapping);
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
}

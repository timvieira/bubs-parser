package edu.ohsu.cslu.tools;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import jsr166y.forkjoin.ForkJoinPool;
import jsr166y.forkjoin.RecursiveAction;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import edu.ohsu.cslu.alignment.SimpleVocabulary;
import edu.ohsu.cslu.common.Vocabulary;
import edu.ohsu.cslu.common.tools.BaseCommandlineTool;
import edu.ohsu.cslu.math.linear.FixedPointShortMatrix;
import edu.ohsu.cslu.math.linear.IntMatrix;
import edu.ohsu.cslu.math.linear.Matrix;
import edu.ohsu.cslu.narytree.BaseNaryTree;
import edu.ohsu.cslu.narytree.ParseTree;
import edu.ohsu.cslu.narytree.BaseNaryTree.PqgramProfile;
import edu.ohsu.cslu.parsing.grammar.InducedGrammar;
import edu.ohsu.cslu.util.Math;
import edu.ohsu.cslu.util.MultipleBufferedReader;

/**
 * Implements (currently) Levenshtein and pq-gram distance calculations.
 * 
 * Reads from a file or from STDIN. Each element to be compared should be on its own line.
 * 
 * @author Aaron Dunlop
 * @since Oct 16, 2008
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class CalculateDistances extends BaseCommandlineTool
{
    private String grammarFilename = null;
    private String filename = null;

    private CalculationMethod calculationMethod;
    private String parameters;
    private int maxThreads;

    @Override
    public void execute() throws Exception
    {
        DistanceCalculator calculator = null;

        // In order to induce a vocabulary and process the same input-stream, we read it into memory
        // up-front and reuse it.
        StringBuilder sb = new StringBuilder(65536);
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        for (String line = br.readLine(); line != null; line = br.readLine())
        {
            sb.append(line);
            sb.append('\n');
        }
        br.close();
        String input = sb.toString();

        MultipleBufferedReader reader = new MultipleBufferedReader(fileAsReader(filename));
        switch (calculationMethod)
        {
            case Levenshtein :
                calculator = new LevenshteinDistanceCalculator();
                break;

            case Pqgram :
                Vocabulary vocabulary = null;
                if (grammarFilename != null)
                {
                    // TODO: Change this to a vocabulary instead - we don't need a full grammar
                    vocabulary = new InducedGrammar("TOP", fileAsReader(grammarFilename), false);
                }
                else
                {
                    vocabulary = SimpleVocabulary.induce(new BufferedReader(new StringReader(input)));
                }

                calculator = new PqgramDistanceCalculator(parameters, vocabulary, maxThreads);
                break;

            default :
                throw new IllegalArgumentException("Unknown calculation method");
        }

        // Read in all lines
        br = new BufferedReader(new StringReader(input));
        for (String line = reader.readLine(); line != null; line = reader.readLine())
        {
            calculator.addElement(line);
        }
        reader.close();

        Matrix matrix = calculator.distance();
        System.out.println(matrix.toString());
    }

    @Override
    @SuppressWarnings("static-access")
    protected Options options() throws Exception
    {
        Options options = basicOptions();

        options.addOption(OptionBuilder.hasArg().withArgName("method").withDescription(
            "Distance Calculation Method (pqgram, levenshtein) (default levenshtein)").create('m'));
        options.addOption(OptionBuilder.hasArg().withArgName("grammar").withDescription("Grammar file").create('g'));
        options.addOption(OptionBuilder.hasArg().withArgName("parameters").withDescription("parameters").create('p'));
        options
            .addOption(OptionBuilder.hasArg().withArgName("threads").withDescription("Maximum Threads").create("xt"));

        return options;
    }

    @Override
    public void setToolOptions(CommandLine commandLine)
    {
        calculationMethod = CalculationMethod.forString(commandLine.getOptionValue('m'));
        grammarFilename = commandLine.getOptionValue('g');
        parameters = commandLine.hasOption('p') ? commandLine.getOptionValue('p') : null;
        maxThreads = commandLine.hasOption("xt") ? Integer.parseInt(commandLine.getOptionValue("xt")) : Runtime
            .getRuntime().availableProcessors();

        if (commandLine.getArgs().length > 0)
        {
            filename = commandLine.getArgs()[0];
        }
    }

    @Override
    protected String usageArguments() throws Exception
    {
        return "[filename]";
    }

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        run(args);
    }

    public static interface DistanceCalculator
    {
        public void addElement(String element);

        public Matrix distance();
    }

    private enum CalculationMethod
    {
        Pqgram, Levenshtein;

        private static HashMap<String, CalculationMethod> enumeration = new HashMap<String, CalculationMethod>();

        static
        {
            enumeration.put("pqgram", Pqgram);
            enumeration.put("levenshtein", Levenshtein);
        }

        public static CalculationMethod forString(String element)
        {
            CalculationMethod m = enumeration.get(element);
            if (m == null)
            {
                // Default to Levenshtein
                m = Levenshtein;
            }
            return m;
        }
    }

    public static class PqgramDistanceCalculator implements DistanceCalculator
    {
        private final int p;
        private final int q;
        private final ArrayList<BaseNaryTree<?>> trees = new ArrayList<BaseNaryTree<?>>();
        private final Vocabulary vocabulary;
        private Matrix matrix;
        private BaseNaryTree.PqgramProfile[] profiles;
        private final int maxThreads;

        public PqgramDistanceCalculator(final String parameters, final Vocabulary vocabulary, final int maxThreads)
        {
            String[] s = parameters.split(",");
            this.p = Integer.parseInt(s[0]);
            this.q = Integer.parseInt(s[1]);
            this.vocabulary = vocabulary;
            this.maxThreads = maxThreads;
        }

        public PqgramDistanceCalculator(int p, int q, Vocabulary vocabulary)
        {
            this.p = p;
            this.q = q;
            this.vocabulary = vocabulary;
            this.maxThreads = Runtime.getRuntime().availableProcessors();
        }

        @Override
        public void addElement(String element)
        {
            trees.add(ParseTree.read(element, vocabulary));
        }

        @Override
        public Matrix distance()
        {
            matrix = new FixedPointShortMatrix(trees.size(), trees.size(), 3, true);

            // Pre-calculate all pq-gram profiles to save time during distance calculations
            profiles = new BaseNaryTree.PqgramProfile[trees.size()];
            for (int i = 0; i < trees.size(); i++)
            {
                profiles[i] = trees.get(i).pqgramProfile(p, q);
            }

            ForkJoinPool executor = new ForkJoinPool(maxThreads);
            executor.invoke(new RowDistanceCalculator(0, matrix.rows() - 1));
            executor.shutdown();
            return matrix;
        }

        private class RowDistanceCalculator extends RecursiveAction
        {
            private final int begin;
            private final int end;

            public RowDistanceCalculator(final int begin, final int end)
            {
                this.begin = begin;
                this.end = end;
            }

            /**
             * Splits distance calculation by rows, executing each row sequentially. This is a
             * pretty simplistic work-splitting algorithm, since the rows aren't of equal length. A
             * better splitting algorithm would divide the work more equally.
             * 
             * But as long as the number of matrix rows is much larger than the number of CPU cores,
             * this works pretty well. And it seems likely that it'll be some time before that
             * assumption breaks down for any problem of meaningful size.
             */
            @Override
            protected void compute()
            {
                if (end == begin)
                {
                    final int i = begin;
                    final PqgramProfile profileI = profiles[i];
                    for (int j = 0; j < i; j++)
                    {
                        matrix.set(i, j, BaseNaryTree.PqgramProfile.pqgramDistance(profileI, profiles[j]));
                    }
                    return;
                }

                RowDistanceCalculator rdc1 = new RowDistanceCalculator(begin, begin + (end - begin) / 2);
                RowDistanceCalculator rdc2 = new RowDistanceCalculator(begin + (end - begin) / 2 + 1, end);
                forkJoin(rdc1, rdc2);
            }
        }

    }

    public static class LevenshteinDistanceCalculator implements DistanceCalculator
    {
        private final ArrayList<String> strings = new ArrayList<String>();

        @Override
        public void addElement(String s)
        {
            strings.add(s);
        }

        @Override
        public Matrix distance()
        {
            Matrix matrix = new IntMatrix(strings.size(), strings.size(), true);

            // Calculate distances
            for (int i = 0; i < strings.size(); i++)
            {
                final String stringI = strings.get(i);
                for (int j = 0; j < i; j++)
                {
                    matrix.set(i, j, distance(stringI, strings.get(j)));
                }
            }

            return matrix;
        }

        public static Matrix distances(String[] elements)
        {
            LevenshteinDistanceCalculator calculator = new LevenshteinDistanceCalculator();
            for (String element : elements)
            {
                calculator.addElement(element);
            }
            return calculator.distance();
        }

        public static int distance(String s1, String s2)
        {
            final int COST = 1;
            // Swap strings so s1 is longer
            if (s2.length() > s1.length())
            {
                String tmp = s1;
                s1 = s2;
                s2 = tmp;
            }

            final char[] s1Chars = s1.toCharArray();
            final char[] s2Chars = s2.toCharArray();

            final int iSize = s2.length() + 1;
            final int jSize = s1.length() + 1;
            int[] previous = new int[jSize];
            int[] current = new int[jSize];

            for (int j = 1; j < jSize; j++)
            {
                previous[j] = previous[j - 1] + COST;
            }

            // TODO: This isn't really the best constant here, but using Integer.MAX_VALUE risks
            // overflow
            Arrays.fill(current, 50000);

            for (int i = 1; i < iSize; i++)
            {
                final int prevI = i - 1;
                final int maxJ = jSize - iSize + i + 1;
                for (int j = 1; j < maxJ; j++)
                {
                    final int prevJ = j - 1;
                    // Gap
                    final int f1 = current[prevJ] + COST;
                    // Substitution or match
                    int f2 = previous[prevJ];
                    // Delete
                    int f3 = previous[j];

                    if (s2Chars[prevI] != s1Chars[prevJ])
                    {
                        f2 += COST;
                        f3 += COST;
                    }

                    current[j] = Math.min(new int[] {f1, f2, f3});
                }

                final int[] tmp = previous;
                previous = current;
                current = tmp;
            }

            return previous[s1.length()];
        }
    }
}

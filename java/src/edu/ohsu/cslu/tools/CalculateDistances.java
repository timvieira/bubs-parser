package edu.ohsu.cslu.tools;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import jsr166y.forkjoin.ForkJoinPool;
import jsr166y.forkjoin.RecursiveAction;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import cltool.BaseCommandlineTool;
import cltool.Threadable;
import edu.ohsu.cslu.datastructs.matrices.FixedPointShortMatrix;
import edu.ohsu.cslu.datastructs.matrices.IntMatrix;
import edu.ohsu.cslu.datastructs.matrices.Matrix;
import edu.ohsu.cslu.datastructs.narytree.BaseNaryTree;
import edu.ohsu.cslu.datastructs.narytree.BaseNaryTree.PqgramProfile;
import edu.ohsu.cslu.util.Math;

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
@Threadable
public class CalculateDistances extends BaseCommandlineTool {

    @Option(name = "-m", aliases = { "--method" }, metaVar = "method", usage = "Distance Calculation Method (pqgram, levenshtein). Default = levenshtein")
    private CalculationMethod calculationMethod;
    @Option(name = "-p", aliases = { "--parameters" }, metaVar = "parameters", usage = "Additional parameters for the specified calucalation method")
    private String parameters;

    @Override
    public void run() throws Exception {
        DistanceCalculator calculator = null;

        // In order to induce a vocabulary and process the same input-stream, we read it into memory
        // up-front and reuse it.
        final StringBuilder sb = new StringBuilder(65536);
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            sb.append(line);
            sb.append('\n');
        }
        br.close();
        final String input = sb.toString();

        switch (calculationMethod) {
            case Levenshtein:
                calculator = new LevenshteinDistanceCalculator();
                break;

            case Pqgram:
                calculator = new PqgramDistanceCalculator(parameters, maxThreads);
                break;

            default:
                throw new IllegalArgumentException("Unknown calculation method");
        }

        // Read in all lines
        br = new BufferedReader(new StringReader(input));
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            calculator.addElement(line);
        }
        br.close();

        final Matrix matrix = calculator.distance();
        matrix.write(new OutputStreamWriter(System.out));
    }

    @Override
    public void setup(final CmdLineParser parser) throws Exception {
        if (calculationMethod == CalculationMethod.Pqgram && parameters == null) {
            throw new CmdLineException(parser,
                "P and Q parameters are required for pqgram distance calculation");
        }
    }

    public static void main(final String[] args) {
        run(args);
    }

    public static interface DistanceCalculator {

        public void addElement(String element);

        public Matrix distance();
    }

    private enum CalculationMethod {
        Pqgram, Levenshtein;

        private static HashMap<String, CalculationMethod> enumeration = new HashMap<String, CalculationMethod>();

        static {
            enumeration.put("pqgram", Pqgram);
            enumeration.put("levenshtein", Levenshtein);
        }

        @SuppressWarnings("unused")
        public static CalculationMethod forString(final String element) {
            CalculationMethod m = enumeration.get(element);
            if (m == null) {
                // Default to Levenshtein
                m = Levenshtein;
            }
            return m;
        }
    }

    public static class PqgramDistanceCalculator implements DistanceCalculator {

        private final int p;
        private final int q;
        private final ArrayList<BaseNaryTree<?>> trees = new ArrayList<BaseNaryTree<?>>();
        private Matrix matrix;
        private BaseNaryTree.PqgramProfile[] profiles;
        private final int maxThreads;

        public PqgramDistanceCalculator(final String parameters, final int maxThreads) {
            final String[] s = parameters.split(",");
            this.p = Integer.parseInt(s[0]);
            this.q = Integer.parseInt(s[1]);
            this.maxThreads = maxThreads;
        }

        public PqgramDistanceCalculator(final int p, final int q) {
            this.p = p;
            this.q = q;
            this.maxThreads = Runtime.getRuntime().availableProcessors();
        }

        @Override
        public void addElement(final String element) {
            trees.add(BaseNaryTree.read(element, String.class));
        }

        @Override
        public Matrix distance() {
            matrix = new FixedPointShortMatrix(trees.size(), trees.size(), 3, true);

            // Pre-calculate all pq-gram profiles to save time during distance calculations
            profiles = new BaseNaryTree.PqgramProfile[trees.size()];
            for (int i = 0; i < trees.size(); i++) {
                profiles[i] = trees.get(i).pqgramProfile(p, q);
            }

            final ForkJoinPool executor = new ForkJoinPool(maxThreads);
            executor.invoke(new RowDistanceCalculator(0, matrix.rows() - 1));
            executor.shutdown();
            return matrix;
        }

        private class RowDistanceCalculator extends RecursiveAction {

            private final int begin;
            private final int end;

            public RowDistanceCalculator(final int begin, final int end) {
                this.begin = begin;
                this.end = end;
            }

            /**
             * Splits distance calculation by rows, executing each row sequentially. This is a pretty
             * simplistic work-splitting algorithm, since the rows aren't of equal length. A better splitting
             * algorithm would divide the work more equally.
             * 
             * But as long as the number of matrix rows is much larger than the number of CPU cores, this
             * works pretty well. And it seems likely that it'll be some time before that assumption breaks
             * down for any problem of meaningful size.
             */
            @Override
            protected void compute() {
                if (end == begin) {
                    final int i = begin;
                    final PqgramProfile profileI = profiles[i];
                    for (int j = 0; j < i; j++) {
                        matrix.set(i, j, BaseNaryTree.PqgramProfile.pqgramDistance(profileI, profiles[j]));
                    }
                    return;
                }

                final RowDistanceCalculator rdc1 = new RowDistanceCalculator(begin, begin + (end - begin) / 2);
                final RowDistanceCalculator rdc2 = new RowDistanceCalculator(begin + (end - begin) / 2 + 1,
                    end);
                forkJoin(rdc1, rdc2);
            }
        }

    }

    public static class LevenshteinDistanceCalculator implements DistanceCalculator {

        private final ArrayList<String> strings = new ArrayList<String>();

        @Override
        public void addElement(final String s) {
            strings.add(s);
        }

        @Override
        public Matrix distance() {
            final Matrix matrix = new IntMatrix(strings.size(), strings.size(), true);

            // Calculate distances
            for (int i = 0; i < strings.size(); i++) {
                final String stringI = strings.get(i);
                for (int j = 0; j < i; j++) {
                    matrix.set(i, j, distance(stringI, strings.get(j)));
                }
            }

            return matrix;
        }

        public static Matrix distances(final String[] elements) {
            final LevenshteinDistanceCalculator calculator = new LevenshteinDistanceCalculator();
            for (final String element : elements) {
                calculator.addElement(element);
            }
            return calculator.distance();
        }

        public static int distance(String s1, String s2) {
            final int COST = 1;
            // Swap strings so s1 is longer
            if (s2.length() > s1.length()) {
                final String tmp = s1;
                s1 = s2;
                s2 = tmp;
            }

            // Access to char arrays is faster than String.charAt()
            final char[] s1Chars = s1.toCharArray();
            final char[] s2Chars = s2.toCharArray();

            // We'll simulate a chart of i rows and j columns, using only 2 arrays (since we only
            // need the current and previous rows)
            final int iSize = s2.length() + 1;
            final int jSize = s1.length() + 1;
            int[] previous = new int[jSize];
            int[] current = new int[jSize];

            // make previous[] look like [0, 1, 2, 3, ..., s1.length()]
            // (or equivalently: like [0, 1, 2, 3, ..., jSize - 1]
            //
            // Fill the 0'th row with the cost of substitutions all the way through
            for (int j = 1; j < jSize; j++) {
                previous[j] = previous[j - 1] + COST;
            }

            // And fill the 1st row with a huge value. Later, when this row is the previous row, we
            // don't want to accidentally get a cost of zero from the j'th entry of an uninitialized
            // array.
            // TODO: This isn't really the best constant here, but using Integer.MAX_VALUE risks
            // overflow
            Arrays.fill(current, 50000);

            for (int i = 1; i < iSize; i++) {
                final int prevI = i - 1;
                final int maxJ = jSize - iSize + i + 1;

                for (int j = 1; j < maxJ; j++) {
                    final int prevJ = j - 1;
                    // Gap
                    final int gapCost = current[prevJ] + COST;
                    // Substitution or match
                    int substitutionOrMatchCost = previous[prevJ];
                    // Delete
                    int deleteCost = previous[j];

                    if (s2Chars[prevI] != s1Chars[prevJ]) {
                        substitutionOrMatchCost += COST;
                        deleteCost += COST;
                    }

                    current[j] = Math.min(new int[] { gapCost, substitutionOrMatchCost, deleteCost });
                }

                final int[] tmp = previous;
                previous = current;
                current = tmp;
            }

            return previous[s1.length()];
        }
    }
}

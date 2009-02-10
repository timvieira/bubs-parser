package edu.ohsu.cslu.alignment.bio;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class EvaluateAlignment
{
    public final static String USAGE = "Usage: %s [-opts] train.file test.MSA                  \n\n"
        + "Options:                                                                 \n"
        + " -?              info/options\n";

    public static long[] evaluate(String[] candidateAlignment, String[] goldStandard)
    {
        long startTime = System.currentTimeMillis();

        if (candidateAlignment.length != goldStandard.length)
        {
            throw new RuntimeException("oops, gold MSA and system MSA don't have the same number of examples");

        }
        final int examples = goldStandard.length;
        final int cols = goldStandard[0].length();

        int j, m, pos, z, lng, nng;
        final int lno[] = new int[goldStandard.length], count[] = new int[goldStandard.length];
        final int columns1[][] = new int[cols][goldStandard.length];
        long correct = 0, total = 0;

        // Post-process corpus
        for (int i = 0; i < examples; i++)
        {
            String example = goldStandard[i];
            if (example.length() != cols)
                throw new RuntimeException("oops, not all columns the same length");
        }

        for (j = 0; j < cols; j++)
        { /* count total pairs */
            z = 0;
            for (int i = 0; i < examples; i++)
                if (goldStandard[i].charAt(j) != '-')
                    z++;
            for (int i = 1; i < z; i++)
                total += i;
        }

        for (int i = 0; i < examples; i++)
        {
            pos = 0;
            for (j = 0; j < cols; j++)
            {
                if (goldStandard[i].charAt(j) != '-')
                    columns1[j][i] = pos++;
                else
                    columns1[j][i] = -1;
            }
        }

        // Post-process system MSA
        int ncols = candidateAlignment[0].length();
        for (int i = 0; i < examples; i++)
        {
            String example = candidateAlignment[i];
            if (example.length() != ncols)
                throw new RuntimeException("oops, not all columns the same");
        }

        final int columns2[][] = new int[ncols][examples];
        int k = 0;
        for (k = 0; k < examples; k++)
        {
            pos = 0;
            for (j = 0; j < ncols; j++)
            {
                if (candidateAlignment[k].charAt(j) != '-')
                    columns2[j][k] = pos++;
                else
                    columns2[j][k] = -1;
            }
        }

        // count correct pairs
        for (j = 0; j < cols; j++)
        {
            lng = examples - 1;
            for (int i = 0; i < examples; i++)
                count[i] = 0;
            int i = 0;
            while (i <= lng)
            {
                while (i <= lng && goldStandard[i].charAt(j) == '-')
                    i++;
                if (i == lng + 1)
                    continue;
                if (count[i] > 0)
                {
                    correct += count[i++] - 1;
                    continue;
                }
                m = lno[i];
                while (m < ncols && columns2[m][i] != columns1[j][i])
                    m++;
                lno[i] = m + 1;
                if (m == ncols)
                {
                    throw new RuntimeException("oops, didn't find same position");
                }
                k = lng;
                nng = examples;
                z = 0;
                while (k > i)
                {
                    while (k > i && goldStandard[k].charAt(j) == '-')
                        k--;
                    if (k == i)
                        continue;
                    if (lng == examples - 1 && nng == examples)
                        lng = k;
                    nng = k;
                    if (columns2[m][k] == columns1[j][k])
                    {
                        lno[k] = m;
                        count[k] = ++z;
                    }
                    k--;
                }
                correct += z;
                i = nng;
            }
        }

        System.out.println("\nEvaluation Time: " + (System.currentTimeMillis() - startTime) + " ms");
        return new long[] {correct, total};
    }

    public static void main(String[] args) throws IOException
    {
        String[] filenames = new String[2];
        int params = 0;
        for (String arg : args)
        {
            if (arg.equals("-?"))
            {
                System.err.println(USAGE);
                System.exit(0);
            }
            else if (params < 2)
            {
                filenames[params++] = arg;
            }
        }

        // Read in gold standard alignment - gzipped
        InputStream goldStandardIs = new FileInputStream(filenames[0]);
        if (filenames[0].endsWith(".gz"))
        {
            goldStandardIs = new GZIPInputStream(goldStandardIs);
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(goldStandardIs));
        List<String> corpus = new LinkedList<String>();

        while ((br.readLine()) != null) // Discard description line
        {
            corpus.add(br.readLine());
        }
        goldStandardIs.close();

        InputStream candidateIs = new FileInputStream(filenames[1]);
        if (filenames[1].endsWith(".gz"))
        {
            candidateIs = new GZIPInputStream(candidateIs);
        }
        br = new BufferedReader(new InputStreamReader(candidateIs));

        List<String> candidateAlignment = new LinkedList<String>();
        // No description line
        for (String example = br.readLine(); example != null; example = br.readLine())
        {
            candidateAlignment.add(example);
        }

        long[] eval = evaluate(corpus.toArray(new String[0]), candidateAlignment.toArray(new String[0]));
        System.out.format("%15d correct out of %15d: %f\n", eval[0], eval[1], ((double) eval[0]) / eval[1]);
    }

}
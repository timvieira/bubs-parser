package edu.ohsu.cslu.hash;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashSet;

import org.kohsuke.args4j.Option;

import cltool.BaseCommandlineTool;

public class CreatePerfectIntPair2IntHash extends BaseCommandlineTool {

    @Option(name = "-m", metaVar = "modulus", usage = "Modulus")
    private int modulus = 0;

    @Override
    protected void run() throws Exception {
        final HashSet<String> lines = new HashSet<String>();
        final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            lines.add(line);
        }

        final int[][] keyPairs = new int[2][lines.size()];
        int i = 0;
        for (final String line : lines) {
            final String[] split = line.split(",");
            keyPairs[0][i] = Integer.parseInt(split[0]);
            keyPairs[1][i++] = Integer.parseInt(split[1]);
        }

        final ImmutableIntPair2IntHash hash = modulus == 0 ? new SegmentedPerfectIntPair2IntHash(keyPairs)
                : new SegmentedPerfectIntPair2IntHash(keyPairs, modulus);

        System.out.println(hash.toString());
    }

    public static void main(final String[] args) {
        run(args);
    }

}

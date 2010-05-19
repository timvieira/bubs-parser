package edu.ohsu.cslu.hash;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.kohsuke.args4j.Option;

import cltool.BaseCommandlineTool;

public class CreatePerfectHash extends BaseCommandlineTool {

    @Option(name = "-m", metaVar = "modulus", usage = "Modulus")
    private int modulus = 0;

    @Override
    protected void run() throws Exception {
        final IntSet keys = new IntOpenHashSet();
        final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            keys.add(Integer.parseInt(line));
        }

        final PerfectHash hash = modulus == 0 ? new PerfectHash(keys.toIntArray()) : new PerfectHash(keys
            .toIntArray(), modulus);

        System.out.println(hash.toString());
    }

    public static void main(final String[] args) {
        run(args);
    }

}

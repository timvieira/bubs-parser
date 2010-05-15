package edu.ohsu.cslu.hash;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import cltool.BaseCommandlineTool;

public class CreatePerfectHash extends BaseCommandlineTool
{

    @Override
    protected void run() throws Exception
    {
        final IntSet keys = new IntOpenHashSet();
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            keys.add(Integer.parseInt(line));
        }

        PerfectHash hash = new PerfectHash(keys.toIntArray());

        System.out.println(hash.toString());
    }

    public static void main(String[] args)
    {
        run(args);
    }

}

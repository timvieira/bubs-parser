/*
 * Copyright 2010, 2011 Aaron Dunlop and Nathan Bodenstab
 * 
 * This file is part of the cslu-common project.
 * 
 * cslu-common is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * cslu-common is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with cslu-common. If not, see <http://www.gnu.org/licenses/>
 */ 
package edu.ohsu.cslu.hash;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import cltool4j.BaseCommandlineTool;
import cltool4j.args4j.Option;

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

        final PerfectInt2IntHash hash = modulus == 0 ? new PerfectInt2IntHash(keys.toIntArray())
                : new PerfectInt2IntHash(keys.toIntArray(), modulus);

        System.out.println(hash.toString());
    }

    public static void main(final String[] args) {
        run(args);
    }

}

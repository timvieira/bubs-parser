/*
 * Copyright 2010-2014, Oregon Health & Science University
 * 
 * This file is part of the BUBS Parser.
 * 
 * The BUBS Parser is free software: you can redistribute it and/or 
 * modify  it under the terms of the GNU Affero General Public License 
 * as published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * The BUBS Parser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Further documentation and contact information is available at
 *   https://code.google.com/p/bubs-parser/ 
 */
package edu.ohsu.cslu.dep;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;

import cltool4j.BaseCommandlineTool;
import cltool4j.args4j.Option;

public class NivreDepParser extends BaseCommandlineTool {

    @Option(name = "-m", required = true, metaVar = "file", usage = "Model file")
    private File modelFile;

    @Override
    protected void run() throws Exception {

        final ObjectInputStream ois = new ObjectInputStream(new FileInputStream(modelFile));
        final TransitionDepParser parser = (TransitionDepParser) ois.readObject();
        ois.close();

        for (final BufferedReader br = inputAsBufferedReader(); br.ready();) {

            final DependencyGraph g = DependencyGraph.readConll(br);
            System.out.println(parser.parse(g));
        }
    }

    public static void main(final String[] args) {
        run(args);
    }
}

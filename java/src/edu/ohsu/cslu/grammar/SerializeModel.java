/*
 * Copyright 2010, 2011 Aaron Dunlop and Nathan Bodenstab
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
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>
 */ 
package edu.ohsu.cslu.grammar;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import cltool4j.BaseCommandlineTool;
import cltool4j.BaseLogger;
import cltool4j.GlobalConfigProperties;
import cltool4j.args4j.Argument;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.parser.edgeselector.BoundaryInOut;
import edu.ohsu.cslu.parser.edgeselector.EdgeSelector;
import edu.ohsu.cslu.parser.edgeselector.EdgeSelector.EdgeSelectorType;

/**
 * Command-line tool to convert textual grammar formats to Java serialized object format.
 * 
 * @author Aaron Dunlop
 * @since Sep 9, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class SerializeModel extends BaseCommandlineTool {

    @Argument(index = 0, metaVar = "grammar", required = true, usage = "Grammar file (text, gzipped text, or binary serialized")
    private String inputGrammarFile;

    @Option(name = "-gc", metaVar = "class", usage = "Grammar Class")
    public String grammarClass;

    @Option(name = "-fom", metaVar = "fom", usage = "Figure of Merit")
    public EdgeSelectorType fomType;

    @Option(name = "-fomModel", metaVar = "file", usage = "FOM model file")
    private String fomModelFileName;

    @Option(name = "-m", metaVar = "metadata", multiValued = true, usage = "Metadata to be serialized into the file (key=value)")
    private String[] options;

    @Option(name = "-ser", required = true, metaVar = "filename", usage = "Serialized output file")
    private String serializedOutputFile;

    public static void main(final String[] args) {
        run(args);
    }

    @Override
    protected void run() throws Exception {
        Reader grammarReader;

        if (inputGrammarFile.endsWith(".gz")) {
            grammarReader = new InputStreamReader(new GZIPInputStream(new FileInputStream(inputGrammarFile)));
        } else {
            grammarReader = new FileReader(inputGrammarFile);
        }

        // Try to create the output stream before we read in the source files. At least it will fail a little sooner if
        // the path is invalid
        OutputStream os = new FileOutputStream(serializedOutputFile);
        if (serializedOutputFile.endsWith(".gz")) {
            os = new GZIPOutputStream(os);
        }
        final ObjectOutputStream oos = new ObjectOutputStream(os);

        BaseLogger.singleton().info("Reading grammar...");
        Grammar g = new Grammar(grammarReader);

        if (grammarClass != null && grammarClass != Grammar.class.getName()) {
            BaseLogger.singleton().info("Converting grammar...");
            g = (Grammar) Class.forName(grammarClass).getConstructor(Grammar.class).newInstance(g);
        }

        EdgeSelector fom = null;
        if (fomModelFileName != null) {
            BaseLogger.singleton().info("Reading FOM...");
            // Handle gzipped and non-gzipped model files
            final BufferedReader fomModelReader = fomModelFileName.endsWith(".gz") ? new BufferedReader(
                    new InputStreamReader(new GZIPInputStream(new FileInputStream(fomModelFileName))))
                    : new BufferedReader(new FileReader(fomModelFileName));
            switch (fomType) {
            case BoundaryInOut:
                fom = new BoundaryInOut(EdgeSelectorType.BoundaryInOut, g, fomModelReader).createEdgeSelector(g);
                break;
            default:
                throw new UnsupportedOperationException("FOM type not supported");
            }
        }

        BaseLogger.singleton().info("Writing serialized model file...");

        // Write a key/value metadata line
        final StringBuilder metadata = new StringBuilder();
        metadata.append("grammarClass=" + g.getClass().getName());
        if (fom != null) {
            metadata.append(",fomClass=" + fom.getClass().getName());
        }
        oos.writeObject(metadata.toString());

        // Write the global properties
        oos.writeObject(GlobalConfigProperties.singleton());

        // Write the grammar
        BaseLogger.singleton().info("Writing grammar...");
        oos.writeObject(g);

        // Write the FOM model
        if (fom != null) {
            BaseLogger.singleton().info("Writing FOM...");
            oos.writeObject(fom);
        }
        oos.close();
        BaseLogger.singleton().info("Serialized model file written.");
    }
}

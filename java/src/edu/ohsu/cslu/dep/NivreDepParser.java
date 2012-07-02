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

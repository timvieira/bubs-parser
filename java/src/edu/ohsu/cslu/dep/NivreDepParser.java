package edu.ohsu.cslu.dep;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;

import cltool4j.args4j.Option;
import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.perceptron.AveragedPerceptron;

public class NivreDepParser extends BaseDepParser {

    @Option(name = "-m", required = true, metaVar = "file", usage = "Model file")
    private File modelFile;

    @SuppressWarnings("unchecked")
    @Override
    protected void run() throws Exception {

        final ObjectInputStream ois = new ObjectInputStream(new FileInputStream(modelFile));
        final AveragedPerceptron model = (AveragedPerceptron) ois.readObject();
        final SymbolSet<String> tokens = (SymbolSet<String>) ois.readObject();
        final SymbolSet<String> pos = (SymbolSet<String>) ois.readObject();
        ois.close();

        for (final BufferedReader br = inputAsBufferedReader(); br.ready();) {

            final DependencyGraph g = DependencyGraph.readConll(br);
            System.out.println(parse(g, model, tokens, pos));
        }
    }

    public static void main(final String[] args) {
        run(args);
    }
}
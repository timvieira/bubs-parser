package edu.ohsu.cslu.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.zip.GZIPInputStream;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import cltool.BaseCommandlineTool;

public class PrintAmbiguousWords extends BaseCommandlineTool {
    @Option(name = "-l", metaVar = "lexicon", usage = "Lexicon file")
    private String lexiconFilename;

    @Argument(multiValued = true)
    private String[] pos;

    public static void main(final String[] args) {
        run(args);
    }

    @Override
    protected void run() throws Exception {
        final File lexiconFile = new File(lexiconFilename);

        InputStream is;

        // Open the lexicon file whether it's gzipped or not
        if (lexiconFile.exists()) {
            is = new FileInputStream(lexiconFile);
            if (lexiconFile.getName().endsWith(".gz")) {
                is = new GZIPInputStream(is);
            }
        } else {
            is = new GZIPInputStream(new FileInputStream(lexiconFile.getAbsolutePath() + ".gz"));
        }

        final Reader lexiconReader = new InputStreamReader(is);

        final HashMap<String, HashSet<String>> posToWords = new HashMap<String, HashSet<String>>();

        final BufferedReader br = new BufferedReader(lexiconReader);
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            final String[] split = line.split(" +");
            HashSet<String> set = posToWords.get(split[0]);
            if (set == null) {
                set = new HashSet<String>(100);
                posToWords.put(split[0], set);
            }
            set.add(split[2]);
        }

        final HashSet<String> set = posToWords.get(pos[0]);
        for (int i = 1; i < pos.length; i++) {
            set.retainAll(posToWords.get(pos[i]));
        }

        for (final String w : set) {
            System.out.println(w);
        }

    }

}

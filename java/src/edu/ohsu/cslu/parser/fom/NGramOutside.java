package edu.ohsu.cslu.parser.fom;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import edu.ohsu.cslu.counters.SimpleCounterSet;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.Tokenizer;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.ParseTree;
import edu.ohsu.cslu.parser.chart.Chart;

public class NGramOutside extends FigureOfMeritModel {

    private Grammar grammar;
    private Tokenizer fomTokenizer;
    float[] unigramLogProb;

    public NGramOutside(final Grammar grammar, final BufferedReader modelStream) throws IOException {
        super(FigureOfMeritModel.FOMType.Ngram);
        this.grammar = grammar;
        final int numLex = grammar.lexSet.size();

        unigramLogProb = new float[numLex];
        Arrays.fill(unigramLogProb, Float.NEGATIVE_INFINITY);

        readModel(modelStream);
    }

    public NGramOutside() {
        super(FigureOfMeritModel.FOMType.Ngram);
    }

    @Override
    public FigureOfMerit createFOM() {
        return new NGramSelector();
    }

    public void train(final BufferedReader inStream, final BufferedWriter outStream, final String grammarFile,
            final double smoothingCount, final boolean writeCounts, final int pruneCount, final String lexCountFile,
            final int unkThresh) throws Exception {
        String line;
        ParseTree tree;
        final SimpleCounterSet<String> ngramCount = new SimpleCounterSet<String>();

        // grammar = ParserDriver.readGrammar(grammarFile, ResearchParserType.ECPCellCrossList, null);
        fomTokenizer = new Tokenizer(lexCountFile, unkThresh);

        while ((line = inStream.readLine()) != null) {
            tree = ParseTree.readBracketFormat(line);
            boolean isFirstWord = true;
            for (final String word : tree.getLeafNodesContent()) {
                final String lexWord = fomTokenizer.wordToLexSetEntry(word, isFirstWord);
                if (lexWord.startsWith("UNK")) {
                    ngramCount.increment(lexWord, "<NONE>");
                }
                ngramCount.increment(word, "<NONE>");
                isFirstWord = false;
            }
        }

        // Write model to file
        float score;
        outStream.write("# model=ngram order=1 smoothingCount=" + smoothingCount + " pruneCount=" + pruneCount
                + " unkThresh=" + unkThresh + " lexCountFile=" + lexCountFile + "\n");

        // left boundary = P(NT | POS-1)
        // NOTE: iterating over the keySet skips all unobserved entries (even if they were
        // smoothed above)
        for (final String hist : ngramCount.items.keySet()) {
            for (final Map.Entry<String, Float> entry : ngramCount.items.get(hist).entrySet()) {
                final String lex = entry.getKey();
                final int count = (int) ngramCount.getCount(lex, hist);
                if (writeCounts) {
                    score = count;
                } else {
                    score = (float) Math.log(ngramCount.getProb(lex, hist));
                }
                if (score > Float.NEGATIVE_INFINITY && count >= pruneCount) {
                    outStream.write("NG " + lex + " | " + hist + " " + score + "\n");
                }
            }
        }

        outStream.close();
    }

    public void readModel(final BufferedReader inStream) throws IOException {
        String line;

        while ((line = inStream.readLine()) != null) {
            // line format: label num | denom prob
            final String[] tokens = line.split("\\s+");
            if (tokens.length > 0 && !tokens[0].equals("#")) {

                if (tokens[0].equals("NG")) {
                    // NG lex | hist prob
                    final int lex = grammar.tokenizer.wordToLexSetIndex(tokens[1], false);
                    unigramLogProb[lex] = Float.parseFloat(tokens[4]);
                } else {
                    System.err.println("WARNING: ignoring line in model file '" + line + "'");
                }
            }
        }

        // Not all UNK classes are found in training. Back-off to UNK
        for (int i = 0; i < unigramLogProb.length; i++) {
            if (unigramLogProb[i] == Float.NEGATIVE_INFINITY) {
                unigramLogProb[i] = unigramLogProb[grammar.lexSet.getIndex("UNK")];
            }
        }
    }

    public class NGramSelector extends FigureOfMerit {

        private static final long serialVersionUID = 1L;
        private float[] fwdScores, bkwScores;

        @Override
        public float calcFOM(final int start, final int end, final short nt, final float insideProbability) {
            return insideProbability + outside(start, end);
            // For edge A(b,e) we can either add (mult) by ngram prob of
            // words outside the span (n_0 ... n_b-1, n_e+1 ... n_N) or subtract (divide)
            // by the ngram prob inside the span (n_b ... n_e). We are doing the former.
        }

        @Override
        public final float calcLexicalFOM(final int start, final int end, final short parent,
                final float insideProbability) {
            return insideProbability;
        }

        public float outside(final int start, final int end) {
            return fwdScores[start + 1] + bkwScores[end];
        }

        @Override
        public void initSentence(final ParseTask task, final Chart chart) {
            final int n = task.sentenceLength();
            fwdScores = new float[n + 1];
            bkwScores = new float[n + 1];
            fwdScores[0] = 0;
            bkwScores[n] = 0;
            for (int i = 1; i < n + 1; i++) {
                // -1 b/c fwdScores is one off from token index (account for <null> at beginning)
                fwdScores[i] = fwdScores[i - 1] + unigramLogProb[task.tokens[i - 1]];
                // System.out.println("fwdScore " + i + " " + fwdScores[i]);
            }
            for (int i = n - 1; i >= 0; i--) {
                // bkwScores is aligned with tokens index .. just has one extra slot at the end
                bkwScores[i] = bkwScores[i + 1] + unigramLogProb[task.tokens[i]];
                // System.out.println("bwkScore " + i + " " + bkwScores[i]);
            }
        }
    }
}

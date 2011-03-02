package edu.ohsu.cslu.parser;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import cltool4j.BaseLogger;
import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.chart.Chart;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.edgeselector.BoundaryInOut;

public abstract class ChartParser<G extends Grammar, C extends Chart> extends Parser<G> {

    public C chart;
    protected long extractTime;
    protected long initTime;
    protected long edgeSelectorInitTime;

    public ChartParser(final ParserDriver opts, final G grammar) {
        super(opts, grammar);
    }

    @Override
    public ParseTree findBestParse(final int[] tokens) throws Exception {
        final boolean collectDetailedStatistics = BaseLogger.singleton().isLoggable(Level.FINER);
        final long t0 = collectDetailedStatistics ? System.currentTimeMillis() : 0;
        initSentence(tokens);
        addLexicalProductions(tokens);

        if (edgeSelector != null) {
            if (collectDetailedStatistics) {
                final long t1 = System.currentTimeMillis();
                initTime = t1 - t0;
                edgeSelector.init(chart);
                edgeSelectorInitTime = System.currentTimeMillis() - t1;
            } else {
                edgeSelector.init(chart);
            }
        }
        cellSelector.initSentence(this);

        while (cellSelector.hasNext()) {
            final short[] startAndEnd = cellSelector.next();
            visitCell(startAndEnd[0], startAndEnd[1]);
        }

        if (collectDetailedStatistics) {
            final long t2 = System.currentTimeMillis();
            final ParseTree parseTree = chart.extractBestParse(grammar.startSymbol);
            extractTime = System.currentTimeMillis() - t2;
            return parseTree;
        }

        return chart.extractBestParse(grammar.startSymbol);
    }

    /**
     * Each subclass will implement this method to perform the inner-loop grammar intersection.
     * 
     * @param start
     * @param end
     */
    protected abstract void visitCell(short start, short end);

    @SuppressWarnings("unchecked")
    protected void initSentence(final int[] tokens) {
        chart = (C) new CellChart(tokens, opts.viterbiMax(), this);
    }

    protected void addLexicalProductions(final int sent[]) {
        // add lexical productions to the base cells of the chart
        for (int i = 0; i < chart.size(); i++) {
            final ChartCell cell = chart.getCell(i, i + 1);
            for (final Production lexProd : grammar.getLexicalProductionsWithChild(sent[i])) {
                cell.updateInside(lexProd, cell, null, lexProd.prob);
            }
        }
    }

    @Override
    public float getInside(final int start, final int end, final int nt) {
        return chart.getInside(start, end, nt);
    }

    @Override
    public float getOutside(final int start, final int end, final int nt) {
        return chart.getInside(start, end, nt);
    }

    @Override
    public String getStats() {
        return chart.getStats()
                + (BaseLogger.singleton().isLoggable(Level.FINER) ? (" edgeInitTime=" + initTime) : "");
    }

    public SparseBitVector getCellFeatures(final int start, final int end, final String featTemplate) {
        int numFeats = 0;
        final List<Integer> featIndicies = new LinkedList<Integer>();

        final int numTags = grammar.posSet.size();
        final int numWords = grammar.lexSet.size();

        for (final String featStr : ParserUtil.tokenize(featTemplate)) {

            // Left tags
            if (featStr.equals("lt")) {
                featIndicies.add(numFeats + getPOSIndex(start));
                numFeats += numTags;
            } else if (featStr.equals("lt+1")) {
                featIndicies.add(numFeats + getPOSIndex(start + 1));
                numFeats += numTags;
            } else if (featStr.equals("lt+2")) {
                featIndicies.add(numFeats + getPOSIndex(start + 2));
                numFeats += numTags;
            } else if (featStr.equals("lt-1")) {
                featIndicies.add(numFeats + getPOSIndex(start - 1));
                numFeats += numTags;
            } else if (featStr.equals("lt-2")) {
                featIndicies.add(numFeats + getPOSIndex(start - 2));
                numFeats += numTags;
            } else if (featStr.equals("lt_lt-1")) {
                featIndicies.add(numFeats + getPOSIndex(start) + numTags * getPOSIndex(start - 1));
                numFeats += numTags * numTags;

                // Right tags -- to get the last tag inside the constituent, we need to subtract 1
            } else if (featStr.equals("rt")) {
                featIndicies.add(numFeats + getPOSIndex(end - 1));
                numFeats += numTags;
            } else if (featStr.equals("rt+1")) {
                featIndicies.add(numFeats + getPOSIndex(end));
                numFeats += numTags;
            } else if (featStr.equals("rt+2")) {
                featIndicies.add(numFeats + getPOSIndex(end + 1));
                numFeats += numTags;
            } else if (featStr.equals("rt-1")) {
                featIndicies.add(numFeats + getPOSIndex(end - 2));
                numFeats += numTags;
            } else if (featStr.equals("rt-2")) {
                featIndicies.add(numFeats + getPOSIndex(end - 3));
                numFeats += numTags;
            } else if (featStr.equals("rt_rt+1")) {
                featIndicies.add(numFeats + getPOSIndex(end) + numTags * getPOSIndex(end + 1));
                numFeats += numTags * numTags;

                // Left words
            } else if (featStr.equals("lw")) {
                featIndicies.add(numFeats + getWordIndex(start));
                numFeats += numWords;
            } else if (featStr.equals("lw-1")) {
                featIndicies.add(numFeats + getWordIndex(start - 1));
                numFeats += numWords;
            } else if (featStr.equals("lw_lt")) {
                featIndicies.add(numFeats + getWordIndex(start) + numWords * getPOSIndex(start));
                numFeats += numWords * numTags;
            } else if (featStr.equals("lw-1_lt-1")) {
                featIndicies.add(numFeats + getWordIndex(start - 1) + numWords * getPOSIndex(start - 1));
                numFeats += numWords * numTags;

                // Right words
            } else if (featStr.equals("rw")) {
                featIndicies.add(numFeats + getWordIndex(end - 1));
                numFeats += numWords;
            } else if (featStr.equals("rw+1")) {
                featIndicies.add(numFeats + getWordIndex(end));
                numFeats += numWords;
            } else if (featStr.equals("rw_rt")) {
                featIndicies.add(numFeats + getWordIndex(end - 1) + numWords * getPOSIndex(end - 1));
                numFeats += numWords * numTags;
            } else if (featStr.equals("rw+1_rt+1")) {
                featIndicies.add(numFeats + getWordIndex(end) + numWords * getPOSIndex(end));
                numFeats += numWords * numTags;

                // cell location
            } else if (featStr.equals("loc")) {
                final int span = end - start;
                final int sentLen = currentInput.sentenceLength;
                for (int i = 1; i <= 5; i++) {
                    if (span == i) {
                        featIndicies.add(numFeats); // span length 1-5
                    }
                    numFeats++;
                    if (span >= i * 10) {
                        featIndicies.add(numFeats); // span > 10,20,30,40,50
                    }
                    numFeats++;
                    if ((float) span / sentLen >= i / 5.0) {
                        featIndicies.add(numFeats); // relative span width btwn 0 and 1
                    }
                    numFeats++;
                }

                if (span == sentLen) {
                    featIndicies.add(numFeats); // TOP cell
                }
                numFeats++;

            } else {
                throw new IllegalArgumentException("ERROR parsing feature template.  Not expecting '" + featStr + "'");
            }
        }

        // convert to int[] array
        final int[] values = new int[featIndicies.size()];
        int i = 0;
        for (final int x : featIndicies) {
            values[i++] = x;
        }
        return new SparseBitVector(numFeats, values);
    }

    private int getPOSIndex(final int start) {
        int index = -1;

        if (start < 0 || start >= this.currentInput.sentenceLength) {
            index = grammar.nullSymbol;
        } else {
            if (currentInput.inputTreeChart != null) {
                // if the input was a tree, we are training from gold trees. Get the gold POS tag.
                // TODO: should we be more robust here; check to see if this parser is an
                // instance of BSCPBeamConfTrain?
                // assert this instanceof BSCPBeamConfTrain;

                for (final Chart.ChartEdge goldEdge : currentInput.inputTreeChart.getEdgeList(start, start + 1)) {
                    if (goldEdge.prod.isLexProd()) {
                        index = goldEdge.prod.parent;
                    }
                }
            } else {
                // we are decoding -- there are a number of things we could do here to get the "best"
                // POS tag for this index; I'm choosing to tag the input sentence with a XX tagger
                // and use the 1-best output.
                index = ((BoundaryInOut) this.edgeSelector).get1bestPOSTag(start);
                // NOTE: this also works with InsideWithFwdBkwd since it inherits from BoundaryInOut
            }
        }

        if (index == -1) {
            throw new UnsupportedOperationException("ERROR: not able to get POS Index during feature extraction");
        }
        return grammar.posSet.getIndex(index); // map from sparce POS index to compact ordering
    }

    private int getWordIndex(final int start) {
        if (start < 0 || start >= this.currentInput.sentenceLength) {
            return grammar.nullWord;
        }
        return currentInput.tokens[start];
    }
}

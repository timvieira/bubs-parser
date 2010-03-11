package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.GrammarByChild;
import edu.ohsu.cslu.parser.cellselector.CellSelector;
import edu.ohsu.cslu.parser.util.ParseTree;

public abstract class ExhaustiveChartParser<G extends GrammarByChild, C extends Chart> extends ChartParser<G, C> {

    protected CellSelector cellSelector;
    protected long totalTime;

    public ExhaustiveChartParser(final G grammar, final CellSelector cellSelector) {
        super(grammar);
        this.cellSelector = cellSelector;
    }

    /**
     * Each subclass will implement this method to perform the inner-loop grammar intersection.
     * 
     * @param start
     * @param end
     */
    protected abstract void visitCell(short start, short end);

    @Override
    public ParseTree findBestParse(final String sentence) throws Exception {

        final long startTime = System.currentTimeMillis();

        final int sent[] = grammar.tokenizer.tokenizeToIndex(sentence);
        currentSentence = sentence;

        initParser(sent.length);
        addLexicalProductions(sent);
        cellSelector.init(this);

        while (cellSelector.hasNext()) {
            final short[] startAndEnd = cellSelector.next();
            visitCell(startAndEnd[0], startAndEnd[1]);
        }

        totalTime = System.currentTimeMillis() - startTime;

        return extractBestParse();
    }

}

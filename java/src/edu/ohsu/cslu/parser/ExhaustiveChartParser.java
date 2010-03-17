package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.GrammarByChild;
import edu.ohsu.cslu.parser.util.ParseTree;

public abstract class ExhaustiveChartParser<G extends GrammarByChild, C extends CellChart> extends ChartParser<G, C> {

    public ExhaustiveChartParser(final ParserOptions opts, final G grammar) {
        super(opts, grammar);
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

        final int sent[] = grammar.tokenizer.tokenizeToIndex(sentence);

        initParser(sent.length);
        addLexicalProductions(sent);
        cellSelector.init(this);

        while (cellSelector.hasNext()) {
            final short[] startAndEnd = cellSelector.next();
            visitCell(startAndEnd[0], startAndEnd[1]);
        }

        return extractBestParse();
    }

}

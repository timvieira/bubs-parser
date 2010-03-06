package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.GrammarByChild;
import edu.ohsu.cslu.parser.cellselector.CellSelector;
import edu.ohsu.cslu.parser.util.ParseTree;

public abstract class ExhaustiveChartParser extends ChartParser {

    protected CellSelector cellSelector;
    protected long totalTime;

    public ExhaustiveChartParser(final GrammarByChild grammar, final CellSelector cellSelector) {
        super(grammar);
        this.cellSelector = cellSelector;
    }

    // overwrite this method for the inner-loop implementation
    protected abstract void visitCell(ChartCell cell);

    @Override
    public ParseTree findBestParse(final String sentence) throws Exception {

        final long startTime = System.currentTimeMillis();

        final int sent[] = grammar.tokenizer.tokenizeToIndex(sentence);
        currentSentence = sentence;

        initParser(sent.length);
        addLexicalProductions(sent);
        cellSelector.init(this);

        while (cellSelector.hasNext()) {
            final ChartCell cell = cellSelector.next();
            visitCell(cell);
        }

        totalTime = System.currentTimeMillis() - startTime;

        return extractBestParse();
    }

}

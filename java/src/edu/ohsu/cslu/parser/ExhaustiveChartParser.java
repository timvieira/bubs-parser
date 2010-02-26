package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.Tokenizer.Token;
import edu.ohsu.cslu.parser.cellselector.CellSelector;
import edu.ohsu.cslu.parser.util.ParseTree;

public abstract class ExhaustiveChartParser extends ChartParser {

    protected CellSelector cellSelector;

    public ExhaustiveChartParser(final Grammar grammar, final CellSelector cellSelector) {
        super(grammar);
        this.cellSelector = cellSelector;
    }

    // overwrite this method for the inner-loop implementation
    protected abstract void visitCell(ChartCell cell);

    public ParseTree findBestParse(final String sentence) throws Exception {
        ChartCell cell;
        final Token sent[] = grammar.tokenize(sentence);
        currentSentence = sentence;

        initParser(sent.length);
        addLexicalProductions(sent);
        cellSelector.init(this);

        final boolean parseFound = false;
        while (!parseFound && cellSelector.hasNext()) {
            cell = cellSelector.next();
            visitCell(cell);
        }

        return extractBestParse();
    }

}

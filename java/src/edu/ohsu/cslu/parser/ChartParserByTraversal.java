package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.BaseGrammar;
import edu.ohsu.cslu.grammar.Tokenizer.Token;
import edu.ohsu.cslu.parser.traversal.ChartTraversal;
import edu.ohsu.cslu.parser.traversal.ChartTraversal.ChartTraversalType;
import edu.ohsu.cslu.parser.util.ParseTree;

public abstract class ChartParserByTraversal extends ChartParser {

    protected ChartTraversalType traversalType;

    public ChartParserByTraversal(final BaseGrammar grammar, final ChartTraversalType traversalType) {
        super(grammar);

        this.traversalType = traversalType;
    }

    // overwrite this method for the inner-loop implementation
    protected abstract void visitCell(ChartCell cell);

    public ParseTree findBestParse(final String sentence) throws Exception {
        ChartCell cell;
        final Token sent[] = grammar.tokenize(sentence);

        initParser(sent.length);
        addLexicalProductions(sent);

        final ChartTraversal chartTraversal = ChartTraversal.create(traversalType, this);
        while (chartTraversal.hasNext()) {
            cell = chartTraversal.next();
            visitCell(cell);
        }

        return extractBestParse();
    }

}

package edu.ohsu.cslu.lela;

import java.util.ArrayList;

import edu.ohsu.cslu.grammar.GrammarFormatType;
import edu.ohsu.cslu.grammar.InsideOutsideCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.grammar.SymbolSet;

/**
 * Grammar class for split-merge grammar learning. References a parent grammar, based on the parent (pre-split)
 * vocabulary.
 * 
 * @author Aaron Dunlop
 */
public class ConstrainedInsideOutsideGrammar extends InsideOutsideCscSparseMatrixGrammar {

    private static final long serialVersionUID = 1L;

    public ConstrainedInsideOutsideGrammar(final ArrayList<Production> binaryProductions,
            final ArrayList<Production> unaryProductions, final ArrayList<Production> lexicalProductions,
            final SymbolSet<String> vocabulary, final SymbolSet<String> lexicon, final GrammarFormatType grammarFormat,
            final Class<? extends PackingFunction> functionClass, final ProductionListGrammar baseGrammar) {

        super(binaryProductions, unaryProductions, lexicalProductions, vocabulary, lexicon, grammarFormat,
                functionClass, true);
    }

    public ConstrainedInsideOutsideGrammar(final ProductionListGrammar plGrammar,
            final GrammarFormatType grammarFormat, final Class<? extends PackingFunction> functionClass) {

        super(plGrammar.binaryProductions, plGrammar.unaryProductions, plGrammar.lexicalProductions,
                plGrammar.vocabulary, plGrammar.lexicon, grammarFormat, functionClass, true);
    }

    public ConstrainedInsideOutsideGrammar(final FractionalCountGrammar countGrammar,
            final GrammarFormatType grammarFormat, final Class<? extends PackingFunction> functionClass) {

        super(countGrammar.binaryProductions(Float.NEGATIVE_INFINITY), countGrammar
                .unaryProductions(Float.NEGATIVE_INFINITY), countGrammar.lexicalProductions(Float.NEGATIVE_INFINITY),
                countGrammar.vocabulary, countGrammar.lexicon, grammarFormat, functionClass, true);
    }
}

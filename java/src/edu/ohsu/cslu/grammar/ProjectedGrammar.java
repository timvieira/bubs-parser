package edu.ohsu.cslu.grammar;

public class ProjectedGrammar extends Grammar {

    Grammar parentGrammar;
    int ntProjection[];

    public ProjectedGrammar(final Grammar parentGrammar) {
        super(parentGrammar);

        this.parentGrammar = parentGrammar;
        this.lexSet = parentGrammar.lexSet;
        this.grammarFormat = parentGrammar.grammarFormat;

        // create mapping from parent grammar non-terms to the non-terms in this grammar
        ntProjection = new int[parentGrammar.numNonTerms()];
        for (int i = 0; i < parentGrammar.numNonTerms(); i++) {
            final String parentNonTermString = parentGrammar.mapNonterminal(i);
            ntProjection[i] = nonTermSet.addSymbol(projectNonTermString(parentNonTermString));

            if (i == parentGrammar.nullSymbol) {
                this.nullSymbol = ntProjection[i];
            }
            if (i == parentGrammar.startSymbol) {
                this.startSymbol = ntProjection[i];
            }
        }
    }

    public int projectNonTerm(final int parentGrammarNT) {
        return ntProjection[parentGrammarNT];
    }

    public String projectNonTermString(final String parentGrammarNT) {
        switch (parentGrammar.grammarFormat) {
        case Berkeley:
            if (parentGrammarNT.contains("_")) {
                // System.out.println("nt=" + nt + " index=" + mapNonterminal(nt) + " eval=" + nt.substring(0,
                // nt.indexOf("_")));
                // NP_12 => NP ; @S_5 => @S
                return parentGrammarNT.substring(0, parentGrammarNT.indexOf("_"));
            }
            // System.out.println("nt=" + nt + " index=" + mapNonterminal(nt) + " eval=" + nt);
            return parentGrammarNT; // <null>, ...
        case CSLU:
            return parentGrammarNT;
        case Roark:
            // SBAR_^SBAR+S_^SBAR+VP_^S => ???
            // @NP_^PP_PP_^NP_''_^NP => ???
            return parentGrammarNT;
        default:
            throw new RuntimeException("GrammarFormatType '" + grammarFormat + "' unknown");
        }
    }

}

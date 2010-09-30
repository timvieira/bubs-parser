package edu.ohsu.cslu.grammar;

import java.util.HashMap;

public class ProjectedGrammar extends Grammar {

    Grammar parentGrammar;
    int ntProjection[];

    public ProjectedGrammar(final Grammar parentGrammar) {
        // NOTE: This class DOES NOT WORK!!! Need projected productions
        super(parentGrammar); // TODO: fix this

        this.parentGrammar = parentGrammar;
        this.grammarFormat = parentGrammar.grammarFormat;
        this.lexSet = parentGrammar.lexSet;
        this.ntProjection = new int[parentGrammar.numNonTerms()];

        // create mapping from parent grammar non-terms to the non-terms in this grammar
        ntProjection = new int[parentGrammar.numNonTerms()];
        for (int i = 0; i < parentGrammar.numNonTerms(); i++) {
            final String parentNonTermString = parentGrammar.mapNonterminal(i);
            final String projNTStr = projectNonTermString(parentNonTermString);
            // System.out.println(parentNonTermString + " => " + projNTStr);
            ntProjection[i] = nonTermSet.addSymbol(projNTStr);

            if (i == parentGrammar.nullSymbol) {
                this.nullSymbol = ntProjection[i];
            }
            if (i == parentGrammar.startSymbol) {
                this.startSymbol = ntProjection[i];
            }
        }

        // * O(1) access from a parentGrammar.prod => this.prod
        // Using a HashMap because I can't get the HashSet does not have a get() method
        // and I want to get a canonical production for all possible projection mappings
        // since we will be accumulating scores given this prod.
        final HashMap<Production, Production> prods = new HashMap<Production, Production>();
        for (final Production p : parentGrammar.binaryProductions) {
            final int A = projectNonTerm(p.parent);
            final int B = projectNonTerm(p.leftChild);
            final int C = projectNonTerm(p.rightChild);
            final Production possProd = new Production(A, B, C, Float.NEGATIVE_INFINITY);
            Production grammarProd = prods.get(possProd);
            if (grammarProd == null) {
                prods.put(possProd, possProd);
                grammarProd = possProd;
            }
            // p.projProd = grammarProd;
        }

        prods.clear();
        for (final Production p : parentGrammar.unaryProductions) {
            final int A = projectNonTerm(p.parent);
            final int B = projectNonTerm(p.child());
            final Production possProd = new Production(A, B, Float.NEGATIVE_INFINITY, false);
            Production grammarProd = prods.get(possProd);
            if (grammarProd == null) {
                prods.put(possProd, possProd);
                grammarProd = possProd;
            }
            // p.projProd = grammarProd;
        }
    }

    // private Production getProduction(final Production p, final List<Production>[] hashSet) {
    // final int v = ((int) Math.pow(p.parent, p.leftChild) + p.rightChild) % hashSet.length;
    // if (hashSet[v] == null)
    // return null;
    // for (final Production grammarProd : hashSet[v]) {
    // if (grammarProd.equals(p))
    // return grammarProd;
    // }
    // return null;
    // }

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

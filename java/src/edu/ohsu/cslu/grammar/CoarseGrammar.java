/*
 * Copyright 2010, 2011 Aaron Dunlop and Nathan Bodenstab
 * 
 * This file is part of the BUBS Parser.
 * 
 * The BUBS Parser is free software: you can redistribute it and/or 
 * modify  it under the terms of the GNU Affero General Public License 
 * as published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * The BUBS Parser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>
 */
package edu.ohsu.cslu.grammar;

import java.io.IOException;

import cltool4j.BaseLogger;

public class CoarseGrammar extends ListGrammar {

    private static final long serialVersionUID = 1L;

    Grammar fineGrammar;
    int fineToCoarseIndex[];

    public CoarseGrammar(final String coarseGrammarFile, final Grammar fineGrammar) throws IOException {
        super(coarseGrammarFile);

        this.fineGrammar = fineGrammar;
        this.grammarFormat = fineGrammar.grammarFormat;
        this.fineToCoarseIndex = new int[fineGrammar.numNonTerms()];
        // this.lexSet = fineGrammar.lexSet;

        // create mapping from parent grammar non-terms to the non-terms in this grammar
        fineToCoarseIndex = new int[fineGrammar.numNonTerms()];
        for (short i = 0; i < fineGrammar.numNonTerms(); i++) {
            final String fineNTStr = fineGrammar.mapNonterminal(i);
            final String coarseNTStr = fineToCoarseNonTermString(fineNTStr);
            // System.out.println(parentNonTermString + " => " + projNTStr);
            if (this.nonTermSet.containsKey(coarseNTStr) == false) {
                BaseLogger.singleton().info(
                        "ERROR: mapping fine non-terminal '" + fineNTStr + "' to coarse non-terminal '" + coarseNTStr
                                + "' but coarse non-terminal not found in coarse grammar.");
                System.exit(1);
            }
            fineToCoarseIndex[i] = nonTermSet.getIndex(coarseNTStr);

            // if (i == fineGrammar.nullSymbol) {
            // this.nullSymbol = fineToCoarseIndex[i];
            // }
            // if (i == fineGrammar.startSymbol) {
            // this.startSymbol = fineToCoarseIndex[i];
            // }
        }

        /*
         * // * O(1) access from a fineGrammar.prod => this.prod // Using a HashMap because I can't get the HashSet does
         * not have a get() method // and I want to get a canonical production for all possible projection mappings //
         * since we will be accumulating scores given this prod. final HashMap<Production, Production> prods = new
         * HashMap<Production, Production>(); for (final Production p : fineGrammar.binaryProductions) { final int A =
         * projectNonTerm(p.parent); final int B = projectNonTerm(p.leftChild); final int C =
         * projectNonTerm(p.rightChild); final Production possProd = new Production(A, B, C, Float.NEGATIVE_INFINITY,
         * this); Production grammarProd = prods.get(possProd); if (grammarProd == null) { prods.put(possProd,
         * possProd); grammarProd = possProd; } // p.projProd = grammarProd; }
         * 
         * prods.clear(); for (final Production p : fineGrammar.unaryProductions) { final int A =
         * projectNonTerm(p.parent); final int B = projectNonTerm(p.child()); final Production possProd = new
         * Production(A, B, Float.NEGATIVE_INFINITY, false, this); Production grammarProd = prods.get(possProd); if
         * (grammarProd == null) { prods.put(possProd, possProd); grammarProd = possProd; } // p.projProd = grammarProd;
         * }
         */
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

    public int fineToCoarseNonTerm(final int fineGrammarNT) {
        return fineToCoarseIndex[fineGrammarNT];
    }

    public String fineToCoarseNonTermString(final String fineGrammarNT) {
        switch (fineGrammar.grammarFormat) {
        case Berkeley:
            // NP_12 => NP ; @S_5 => @S
            if (fineGrammarNT.equals("<null>")) {
                return fineGrammarNT;
            }
            // if (fineGrammarNT.contains("_")) {
            return fineGrammarNT.substring(0, fineGrammarNT.indexOf("_"));
            // }
            // System.out.println("nt=" + nt + " index=" + mapNonterminal(nt) + " eval=" + nt);
            // return fineGrammarNT; // <null>, ...
        case CSLU:
        case Roark:
            // SBAR_^SBAR+S_^SBAR+VP_^S => ???
            // @NP_^PP_PP_^NP_''_^NP => ???
        default:
            throw new RuntimeException("GrammarFormatType '" + grammarFormat
                    + "' does not support fine-to-coarse mapping");
        }
    }

    @Override
    public boolean isCoarseGrammar() {
        return true;
    }

    public Grammar getFineGrammar() {
        return fineGrammar;
    }
}

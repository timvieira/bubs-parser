/*
 * Copyright 2010-2014, Oregon Health & Science University
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
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Further documentation and contact information is available at
 *   https://code.google.com/p/bubs-parser/ 
 */
package edu.ohsu.cslu.grammar;

import java.util.LinkedList;

import cltool4j.BaseLogger;
import edu.ohsu.cslu.util.Strings;

public enum GrammarFormatType {
    CSLU, Roark, Berkeley;

    public String factoredNonTerminal(final String nonTerminal) {
        switch (this) {
        case Berkeley:
            return "@" + nonTerminal;
        case CSLU:
            return nonTerminal + "|";
        case Roark:
            // TODO Support Roark format
        default:
            throw new IllegalArgumentException("Unsupported format");
        }

    }

    public String getEvalNonTerminal(final String nt) {
        switch (this) {
        case CSLU:
            return getBaseNT(nt, false);
        case Berkeley:
            return nt.substring(0, nt.indexOf("_"));
        default:
            throw new IllegalArgumentException("Grammar format does not support getEvalNonTerminal()");
        }
    }

    public boolean isFactored(final String nonTerminal) {
        switch (this) {
        case CSLU:
            return nonTerminal.contains("|");
        case Berkeley:
            // TODO We shouldn't really need to check for a single '@' symbol - that should only be a terminal.
            // Trace back and eliminate calling isFactored() for terminals.
            return nonTerminal.charAt(0) == '@' && nonTerminal.length() > 1;
        case Roark:
            // TODO Support Roark format
        default:
            throw new IllegalArgumentException("Unsupported format");
        }
    }

    /**
     * Returns the base (unsplit) non-terminal from which the specified non-term is derived
     * 
     * @param nt
     * @param unfactor Remove factored labels
     * @return the base (unsplit) non-terminal from which the specified non-term is derived
     */
    public String getBaseNT(final String nt, final boolean unfactor) {
        switch (this) {
        case CSLU:
            // If we're not unfactoring, remove all annotations except the '|' which denotes a factored category. e.g.
            // NP^<S>|<VP-NN> -> NP|
            int i = nt.indexOf('|');
            int j = nt.indexOf('^');
            if (i < 0 && j < 0) {
                // nt is not factored or parent-annotated
                return nt;
            }

            if (j >= 0) {
                if (i >= 0 && !unfactor) {
                    // nt is factored and parent-annotated
                    return nt.substring(0, j) + '|';
                }
                // nt is parent-annotated but not factored or we're unfactoring anyway
                return nt.substring(0, j);
            }

            // nt is factored, but not parent-annotated
            return unfactor ? nt.substring(0, i) : nt.substring(0, i + 1);

        case Berkeley:
            i = unfactor && nt.charAt(0) == '@' ? 1 : 0;
            j = nt.indexOf('_');
            return j > 0 ? nt.substring(i, j) : nt.substring(i);

        case Roark:
            // TODO: Support Roark format
        default:
            throw new IllegalArgumentException("Unsupported format");
        }
    }

    public String createFactoredNT(final String unfactoredParent, final LinkedList<String> markovChildrenStr) {
        switch (this) {
        case CSLU:
            return unfactoredParent + "|<" + Strings.join(markovChildrenStr, "-") + ">";
        case Berkeley:
            if (markovChildrenStr.size() > 0) {
                BaseLogger.singleton().info(
                        "ERROR: Berkeley grammar does not support horizontal markov smoothing for factored nodes");
                System.exit(1);
            }
            return "@" + unfactoredParent;
        case Roark:
            // TODO Support Roark format
        default:
            throw new IllegalArgumentException("Unsupported format");
        }
    }

    public String createParentNT(final String contents, final LinkedList<String> parentsStr) {
        switch (this) {
        case CSLU:
            String base = contents,
            rest = "";

            if (isFactored(contents)) {
                final int i = contents.indexOf("|");
                base = contents.substring(0, i);
                rest = contents.substring(i);
            }
            return base + "^<" + Strings.join(parentsStr, "-") + ">" + rest;
        case Berkeley:
        case Roark:
        default:
            throw new IllegalArgumentException("Unsupported format");
        }
    }
}

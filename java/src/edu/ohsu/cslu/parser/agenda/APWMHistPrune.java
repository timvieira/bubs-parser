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
package edu.ohsu.cslu.parser.agenda;

import edu.ohsu.cslu.grammar.LeftRightListsGrammar;
import edu.ohsu.cslu.parser.ParserDriver;

/**
 * @author Nathan Bodenstab
 */
public class APWMHistPrune extends APWithMemory {

    // Agenda parsing can be slow because the global agenda contains
    // a lot of edges and we keep pushing bad edges into it. If we
    // can prune them before adding them to the agenda, we could save
    // a lot of time. Try histogram pruning. Look at the distribution
    // of edge scores for different sentences. Might need to dynamically
    // adjust/redo bin size and pruning thresholds on the fly after some
    // number of edges have been pushed.

    public APWMHistPrune(final ParserDriver opts, final LeftRightListsGrammar grammar) {
        super(opts, grammar);
        // TODO Auto-generated constructor stub
        // Test of commit ... again
    }

}

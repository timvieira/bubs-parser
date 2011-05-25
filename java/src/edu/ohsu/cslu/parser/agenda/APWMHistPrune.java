package edu.ohsu.cslu.parser.agenda;

import edu.ohsu.cslu.grammar.LeftRightListsGrammar;
import edu.ohsu.cslu.parser.ParserDriver;

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
    }

}

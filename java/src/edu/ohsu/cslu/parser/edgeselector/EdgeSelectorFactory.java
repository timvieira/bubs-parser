package edu.ohsu.cslu.parser.edgeselector;

import cltool4j.BaseLogger;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.parser.edgeselector.EdgeSelector.EdgeSelectorType;

/**
 * Represents a model for edge selection and creates edge selector instances using that model(see {@link EdgeSelector}).
 * Implementers may constrain chart cell population by lexical analysis of the sentence or other outside information.
 * 
 * @author Aaron Dunlop
 * @since Mar 10, 2011
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class EdgeSelectorFactory {

    protected EdgeSelectorType type;

    public EdgeSelectorFactory(final EdgeSelectorType type) {
        this.type = type;
    }

    /**
     * @return a new {@link EdgeSelector} instance based on this model.
     */
    public EdgeSelector createEdgeSelector(final Grammar grammar) {
        switch (type) {
        case Inside:
            return new InsideProb();
        case NormalizedInside:
            return new NormalizedInsideProb();
        case WeightedFeatures:
            return new WeightedFeatures(grammar);
        default:
            BaseLogger.singleton().info("ERROR: EdgeSelectorType " + type + " not supported.");
            System.exit(1);
            return null;
        }
    }
}

package edu.ohsu.cslu.parser.cellselector;

/**
 * Represents a model for cell selection and creates cell selector instances using that model(see {@link CellSelector}).
 * Implementers may constrain chart cell iteration or population by lexical analysis of the sentence or other outside
 * information.
 * 
 * @author Aaron Dunlop
 * @since Mar 10, 2011
 * 
 * @version $Revision$ $Date$ $Author$
 */
public interface CellSelectorFactory {

    /**
     * @return a new {@link CellSelector} instance based on this model
     */
    public CellSelector createCellSelector();
}

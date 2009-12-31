package edu.ohsu.cslu.datastructs;

/**
 * Enumeration of a variety of semirings under which mathematical operations can be carried out.
 * 
 * @author Aaron Dunlop
 * @since Dec 30, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public enum Semiring {

    /** Reals U -Infinity, Max, + */
    TROPICAL,

    /** Reals U Infinity, Min, + */
    TROPICAL_MIN;
}

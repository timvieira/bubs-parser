package edu.ohsu.cslu.common.tools;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a command-line tool is threadable.
 * 
 * @author Aaron Dunlop
 * @since Jan 10, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( {ElementType.TYPE})
@Inherited
public @interface Threadable {}

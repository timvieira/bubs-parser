package edu.ohsu.cslu.grammar;

import java.util.List;

import edu.ohsu.cslu.grammar.ArrayGrammar.Production;
import edu.ohsu.cslu.grammar.Tokenizer.Token;

/**
 * Interface for PCFG implementations.
 * 
 * @author Aaron Dunlop
 * @since Jan 7, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public interface Grammar {

    public Token[] tokenize(final String sentence) throws Exception;

    public List<Production> getLexProdsForToken(final Token token);

    public List<Production> getUnaryProdsWithChild(final int child);

}

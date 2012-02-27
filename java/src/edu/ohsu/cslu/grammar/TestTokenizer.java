package edu.ohsu.cslu.grammar;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests for {@link Tokenizer}
 * 
 * @author Aaron Dunlop
 */
public class TestTokenizer {

    @Test
    public void testTreebankTokenize() {

        assertEquals("He said , `` The children 's parents wo n't go . ''",
                Tokenizer.treebankTokenize("He said, \"The children's parents won't go.\""));
        assertEquals("I 'm gon na go !", Tokenizer.treebankTokenize("I'm gonna go!"));
        assertEquals("-LRB- -LSB- -LCB- -RCB- -RSB- -RRB-", Tokenizer.treebankTokenize("([{}])"));
        assertEquals("-LRB- a -LSB- b -LCB- c -LRB- d -RRB- -RCB- -RSB- -RRB-",
                Tokenizer.treebankTokenize("(a [b {c (d)}])"));
        assertEquals("Testing ellipses ...", Tokenizer.treebankTokenize("Testing ellipses..."));
        assertEquals("R_ -LRB- n -RRB- represents the number of documents retrieved",
                Tokenizer.treebankTokenize("R_(n ) represents the number of documents retrieved"));
    }
}

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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests for {@link TokenClassifier}
 * 
 * TODO Add a lot more unit tests before optimizing the tokenization routines
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

        // A couple tests for mid-sentence periods
        assertEquals("Testing etc. in mid-sentence .", Tokenizer.treebankTokenize("Testing etc. in mid-sentence."));
        assertEquals("Testing Ltd. in mid-sentence .", Tokenizer.treebankTokenize("Testing Ltd. in mid-sentence."));
        assertEquals("Testing Ph. D. in mid-sentence .", Tokenizer.treebankTokenize("Testing Ph.D. in mid-sentence."));

        // And for mid-sentence punctuation
        assertEquals("`` What happens with a question mark ? '' said Bob .",
                Tokenizer.treebankTokenize("\"What happens with a question mark?\" said Bob."));
        assertEquals("`` Ouch ! '' said Fred .", Tokenizer.treebankTokenize("\"Ouch!\" said Fred."));
    }

    @Test
    public void testTreebankDetokenize() {
        assertEquals(
                "Phil Smith, Ph.D. said, \"We're on our way.\" (as he ran to the car).",
                Tokenizer
                        .treebankDetokenize("Phil Smith , Ph. D. said , `` We 're on our way . '' -LRB- as he ran to the car -RRB- ."));
        assertEquals("Leisure & Artz [sic].", Tokenizer.treebankDetokenize("Leisure & Artz -LSB- sic -RSB- ."));
        assertEquals(
                "don't won't wouldn't it's Mitchell's they're he'd I'm I'll we're you'd they'll I've you've you'd",
                Tokenizer
                        .treebankDetokenize("do n't wo n't would n't it 's Mitchell 's they 're he 'd I 'm I 'll we 're you 'd they 'll I 've you 've you 'd "));
    }
}

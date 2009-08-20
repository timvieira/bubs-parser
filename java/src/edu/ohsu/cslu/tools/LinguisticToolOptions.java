package edu.ohsu.cslu.tools;

/**
 * Static constants and methods used by command-line tools which deal with commonly-used linguistic
 * options (words, POS, etc.)
 * 
 * @author Aaron Dunlop
 * @since Apr 2, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class LinguisticToolOptions
{
    public final static String OPTION_WORD = "w";
    public final static String OPTION_LOWERCASE_WORD = "lcw";
    public final static String OPTION_POS = "p";
    public final static String OPTION_PLAIN_POS = "ppos";

    public final static String OPTION_CAPITALIZED = "cap";
    public final static String OPTION_ALL_CAPS = "allcaps";
    public final static String OPTION_HYPHENATED = "hyphen";

    public final static String OPTION_HEAD_VERB = "h";
    public final static String OPTION_BEFORE_HEAD = "bh";
    public final static String OPTION_AFTER_HEAD = "ah";
    public final static String OPTION_PREVIOUS_WORD = "prevword";
    public final static String OPTION_SUBSEQUENT_WORD = "subword";
    public final static String OPTION_PREVIOUS_POS = "prevpos";
    public final static String OPTION_SUBSEQUENT_POS = "subpos";

    public final static String OPTION_GAP = "g";
}

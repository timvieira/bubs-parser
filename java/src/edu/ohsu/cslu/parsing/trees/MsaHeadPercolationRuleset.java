package edu.ohsu.cslu.parsing.trees;

public class MsaHeadPercolationRuleset extends HeadPercolationRuleset
{
    public MsaHeadPercolationRuleset()
    {
        super(ruleset());
    }

    private static String ruleset()
    {
        StringBuilder sb = new StringBuilder(2048);
        sb.append("*default* (r AUX AUXG BES CC CD DT EX FW HVS IN JJ JJR JJS LS MD NN NNS NNP NNPS PDT POS PRP ");
        sb.append("PRP$ RB RBR RBS RP SYM TO UH VB VBD VBG VBN VBP VBZ WDT WP WP$ WRB # $) (r ADJP ADVP CONJP ");
        sb.append("FRAG INTJ LST NAC NML NP NX PRN PRT QP RRC S S1 SBAR SBARQ SINV SQ UCP VP WHADJP WHADVP ");
        sb.append("WHNP WHPP X) (r PP) (r . , : -RRB- -LRB- `` '' XX GW) (r)\n");

        sb.append("ADJP (r JJ JJR JJS) (r ADJP) (r RB VBN)\n");
        sb.append("ADVP (r RB RBB) (r ADVP)\n");
        sb.append("CONJP (r CONJP)\n");
        sb.append("FRAG (r FRAG)\n");
        sb.append("INTJ (r INTJ)\n");
        sb.append("LST (r LS) (r LST)\n");
        sb.append("NAC (r NN NNP NNPS NNS PRP) (r NAC) (r ADJP CD FW JJ NP)\n");
        sb.append("NML (r NN NNP NNPS NNS PRP) (r NML) (r ADJP CD FW JJ NP)\n");
        sb.append("NP (r $ NN NNP NNPS NNS POS PRP) (r NP) (r ADJP CD JJ NX)\n");
        sb.append("NX (r NN NNP NNPS NNS PRP)  (r NX) (r ADJP CD FW JJ NP)\n");
        sb.append("PP (l IN RP TO) (r PP)\n");
        sb.append("PRN (r PRN)\n");
        sb.append("PRT (r RP) (r PRT) (r IN RB)\n");
        sb.append("QP (r QP) (r $ NN)\n");
        sb.append("RRC (r RRC)\n");
        sb.append("S (r VP) (r S) (r SBARQ SINV X)\n");
        sb.append("SBAR (r IN WHNP) (r SBAR) (r WHADJP WHADVP WHPP)\n");
        sb.append("SBARQ (r SQ VP) (r SBARQ) (r S SINV X)\n");
        sb.append("SINV (r VP) (r SINV) (r SBAR)\n");
        sb.append("SQ (r AUX BES HVS MD) (r SQ) (r VP)\n");
        sb.append("UCP (r UCP)\n");
        sb.append("VP (r VP) (r AUX AUXG BES HVS MD TO VB VBD VBG VBN VBP VBZ)\n");
        sb.append("WHADJP (r WRB) (r WHADJP)\n");
        sb.append("WHADVP (r WRB) (r WHADVP)\n");
        sb.append("WHNP (r WDT WP WP$) (r WHNP)\n");
        sb.append("WHPP (r IN TO) (r WHPP)\n");
        sb.append("X (r X)\n");

        // Added to Charniak's rule-set for semantic completeness
        sb.append("TOP (r S) (r FRAG)\n");

        return sb.toString();
    }

}

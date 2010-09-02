package edu.ohsu.cslu.grammar;

public class LeftListEvalGrammar extends LeftListGrammar {

	// protected final SymbolSet<String> evalNonTermSet = new SymbolSet<String>();
	// int ntToEvalMap[];

	public LeftListEvalGrammar(final String grammarFile, final String lexiconFile, final GrammarFormatType grammarFormat) throws Exception {
		super(grammarFile, lexiconFile, grammarFormat);

		// ntToEvalMap = new int[numNonTerms()];
		// for (int i = 0; i < numNonTerms(); i++) {
		// ntToEvalMap[i] = evalNonTermSet.addSymbol(getEvalNonTerm(mapNonterminal(i)));
		// }
	}
	//
	// public LeftListEvalGrammar(final Reader grammarFile, final Reader lexiconFile, final GrammarFormatType grammarFormat) throws Exception {
	// super(grammarFile, lexiconFile, grammarFormat);
	//
	// ntToEvalMap = new int[numNonTerms()];
	// for (int i = 0; i < numNonTerms(); i++) {
	// ntToEvalMap[i] = evalNonTermSet.addSymbol(getEvalNonTerm(mapNonterminal(i)));
	// }
	// }
	//
	// public int getEvalNonTerm(final int nt) {
	// return ntToEvalMap[nt];
	// }
	//
	// public int numEvalNonTerms() {
	// return evalNonTermSet.size();
	// }
	//
	// public String getEvalNonTerm(final String nt) {
	// switch (grammarFormatType) {
	// case Berkeley:
	// if (nt.contains("_")) {
	// // System.out.println("nt=" + nt + " index=" + mapNonterminal(nt) + " eval=" + nt.substring(0, nt.indexOf("_")));
	// // NP_12 => NP ; @S_5 => @S
	// return nt.substring(0, nt.indexOf("_"));
	// }
	// // System.out.println("nt=" + nt + " index=" + mapNonterminal(nt) + " eval=" + nt);
	// return nt; // <null>, ...
	// case CSLU:
	// return nt;
	// case Roark:
	// // SBAR_^SBAR+S_^SBAR+VP_^S => ???
	// // @NP_^PP_PP_^NP_''_^NP => ???
	// return nt;
	// default:
	// throw new RuntimeException("GrammarFormatType '" + grammarFormatType + "' unknown");
	// }
	// }
	//    
	// public class EvalProduction extends Production {
	//        
	// }
}

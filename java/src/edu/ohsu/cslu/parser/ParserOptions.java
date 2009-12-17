package edu.ohsu.cslu.parser;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;

import edu.ohsu.cslu.parser.fom.EdgeFOM.EdgeFOMType;
import edu.ohsu.cslu.parser.traversal.ChartTraversal.ChartTraversalType;
import edu.ohsu.cslu.parser.util.Log;


public class ParserOptions {
	public String pcfgFileName = null;
	public String lexFileName = null;
	public BufferedReader inputStream = new BufferedReader(new InputStreamReader(System.in));
	
	public boolean printInsideProbs = false;
	public boolean printUnkLabels = false;
	
	public ParserType parserType = null;
	public ChartTraversalType chartTraversalType = null;
	public ChartCellVisitationType chartCellVisitationType = null;
	public EdgeFOMType edgeFOMType = null;
	
	static public enum ParserType { 
		ExhaustiveChartParser,
		AgendaParser,
		AgendaParserWithGhostEdges,
		SuperAgendaParser }
	
	static public enum ChartCellVisitationType {
		CellCrossList,
		CellCrossHash,
		CellCrossMatrix,
		GrammarLoop,
		GrammarLoopBerkeleyFilter
	}
	
	public ParserOptions(String[] argv) throws FileNotFoundException {
		boolean foundGram=false;
		for (int i=0; i<argv.length; i++) {
			
			if (argv[i].equals("-scores")) { printInsideProbs = true; }
			if (argv[i].equals("-input")) { inputStream = new BufferedReader(new FileReader(argv[i+1])); }
			if (argv[i].equals("-unk")) { printUnkLabels = true; }
			if (argv[i].equals("-g") || argv[i].equals("-grammar")) {
				pcfgFileName = argv[i+1]+".pcfg";
				lexFileName = argv[i+1]+".lex";
				foundGram=true;
			}
			if (argv[i].equals("-p") || argv[i].equals("-parser")) {
				if (argv[i+1].equals("CCL")) {
					parserType=ParserType.ExhaustiveChartParser;
					chartTraversalType=ChartTraversalType.LeftRightBottomTopTraversal;
					chartCellVisitationType=ChartCellVisitationType.CellCrossList;
				} else if (argv[i+1].equals("CCH")) {
					parserType=ParserType.ExhaustiveChartParser;
					chartTraversalType=ChartTraversalType.LeftRightBottomTopTraversal;
					chartCellVisitationType=ChartCellVisitationType.CellCrossHash;
				} else if (argv[i+1].equals("CCM")) {
					parserType=ParserType.ExhaustiveChartParser;
					chartTraversalType=ChartTraversalType.LeftRightBottomTopTraversal;
					chartCellVisitationType=ChartCellVisitationType.CellCrossMatrix;					
				} else if (argv[i+1].equals("GL")) {
					parserType=ParserType.ExhaustiveChartParser;
					chartTraversalType=ChartTraversalType.LeftRightBottomTopTraversal;
					chartCellVisitationType=ChartCellVisitationType.GrammarLoop;
				} else if (argv[i+1].equals("GLBF")) {
					parserType=ParserType.ExhaustiveChartParser;
					chartTraversalType=ChartTraversalType.LeftRightBottomTopTraversal;
					chartCellVisitationType=ChartCellVisitationType.GrammarLoopBerkeleyFilter;
				} else if (argv[i+1].equals("A")) {
					parserType=ParserType.AgendaParser;
					edgeFOMType=EdgeFOMType.Inside;
				} else if (argv[i+1].equals("AGE")) {
					parserType=ParserType.AgendaParserWithGhostEdges;
					edgeFOMType=EdgeFOMType.Inside;
				} else {
					Log.info(0, "ERROR: -parser value '"+argv[i+1]+"' not a valid option");
					System.exit(1);
				}
			}
		}
		
		if (foundGram == false) {
			Log.info(0,"ERROR: Grammar file is required.  Use -g option to specify.");
			System.exit(1);
		}
		
		// default parser
		if (parserType == null) { 
			parserType = ParserType.ExhaustiveChartParser;
			chartTraversalType = ChartTraversalType.LeftRightBottomTopTraversal;
			chartCellVisitationType = ChartCellVisitationType.CellCrossList;
		}

	}
	
	public String toString() {
		return toString("");
	}
	
	public String toString(String prefix) {
		String s="";
		s+=prefix+" ParserType="+parserType+"\n";
		s+=prefix+"  Traversal="+chartTraversalType+"\n";
		s+=prefix+"  CellVisit="+chartCellVisitationType+"\n";
		s+=prefix+"        FOM="+edgeFOMType+"";
		
		return s;
	}
}

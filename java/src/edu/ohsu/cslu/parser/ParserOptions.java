package edu.ohsu.cslu.parser;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;

import edu.ohsu.cslu.parser.ParserDriver.ParserType;
import edu.ohsu.cslu.parser.util.Log;


public class ParserOptions {
	public String pcfgFileName = null;
	public String lexFileName = null;
	public BufferedReader inputStream = new BufferedReader(new InputStreamReader(System.in));
	
	public boolean printInsideProbs = false;
	public boolean printUnkLabels = false;
	public ParserType parserType = ParserType.ExhaustiveChartParserGramLoopBerkFilter;
	
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
				if (argv[i+1].equals("CCL")) parserType=ParserType.ExhaustiveChartParserCellCrossList;
				else if (argv[i+1].equals("CCH")) parserType=ParserType.ExhaustiveChartParserCellCrossHash;
				else if (argv[i+1].equals("CCM")) parserType=ParserType.ExhaustiveChartParserCellCrossMatrix;
				else if (argv[i+1].equals("GL")) parserType=ParserType.ExhaustiveChartParserGramLoop;
				else if (argv[i+1].equals("GLBF")) parserType=ParserType.ExhaustiveChartParserGramLoopBerkFilter;
				else if (argv[i+1].equals("Agenda")) parserType=ParserType.AgendaMaximumLikelihoodChartParser;
				else {
					Log.info(0, "ERROR: -parser value '"+argv[i+1]+"' not a valid option");
					System.exit(1);
				}
			}
		}
		
		if (foundGram == false) {
			Log.info(0,"ERROR: Grammar file is required.  Use -g option to specify.");
			System.exit(1);
		}
	}
}

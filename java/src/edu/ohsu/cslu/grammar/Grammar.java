package edu.ohsu.cslu.grammar;

import java.io.BufferedReader;

import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import edu.ohsu.cslu.grammar.Tokenizer.Token;
import edu.ohsu.cslu.parser.util.Log;


public class Grammar {
	public LinkedList<Production> binaryProds;
	public LinkedList<Production> unaryProds;
	public int startSymbol = -1;

	//private LinkedList<LexicalProduction>[] lexicalProds = new LinkedList<UnaryProduction>[5];
	// note: java doesn't allow generic array creation, so must do it like this: 
	private Vector<List<Production>> lexicalProds;
	private SymbolSet nonTermSet;
	private SymbolSet lexSet;
	private Tokenizer tokenizer;
	private boolean[] possibleLeftChild;
	private boolean[] possibleRightChild;
	
	public Grammar(String gramFileName, String lexFileName) throws IOException {
		binaryProds = new LinkedList<Production>();
		unaryProds = new LinkedList<Production>();
		nonTermSet = new SymbolSet();
		lexSet = new SymbolSet();
				
		readGrammarFromFile(gramFileName);
		if (startSymbol == -1) {
			Log.info(0,"ERROR: No start symbol found in grammar file.  Expecting a single non-terminal on the first line.");
			System.exit(1);
		}
		
		readLexProdsFromFile(lexFileName);
		tokenizer = new Tokenizer(lexSet);
		markLeftRightChildren();
	}
	
	public Token[] tokenize(String sentence) throws Exception {
		return tokenizer.tokenize(sentence);
	}
	
	public int numNonTerms() {
		return nonTermSet.numSymbols();
	}
	
	public boolean hasWord(String s) {
		return lexSet.hasLabel(s);
	}
	
	public List<Production> getLexProdsForToken(Token token) {
		/*
		if (token.isUnk()) {
			// make new lexical prods for the UNK words that will be deleted after parsing the sentence
			List<Production> unkProds = new LinkedList<Production>();
			for (Production p : lexicalProds.get(token.index)){
				unkProds.add(p.copy());
			}
			
		} else {
			return lexicalProds.get(token.index);
		}
		*/
		return lexicalProds.get(token.index);
	}
	
	// TODO: not efficient.  Should index by child
	public List<Production> getUnaryProdsWithChild(int child) {
		List<Production> matchingProds = new LinkedList<Production>();
		for (Production p : unaryProds) {
			if (p.leftChild == child) matchingProds.add(p);
		}
		
		return matchingProds;
	}
	
	private void markLeftRightChildren() {
		possibleLeftChild = new boolean[this.numNonTerms()];
		possibleRightChild = new boolean[this.numNonTerms()];
		Arrays.fill(possibleLeftChild, false);
		Arrays.fill(possibleRightChild, false);
		for (Production p : binaryProds) {
			possibleLeftChild[p.leftChild] = true;
			possibleRightChild[p.rightChild] = true;
		}
	}
	
	public boolean isLeftChild(int nonTerm) {
		return possibleLeftChild[nonTerm];
	}
	
	public boolean isRightChild(int nonTerm) {
		return possibleRightChild[nonTerm];
	}
	
	private void readLexProdsFromFile(String lexFileName) throws IOException 
	{
		String line;
		String[] tokens;
		Production lexProd;
		List<Production> tmpProdList = new LinkedList<Production>();
		
		Log.info(1,"INFO: Reading lexical productions from: "+lexFileName);
		
		BufferedReader lexFile = new BufferedReader(new FileReader(lexFileName));
		while ((line = lexFile.readLine()) != null) {
			tokens = line.split("\\s");
			if (tokens.length == 4) {
				// expecting: A -> B prob
				lexProd = new Production(tokens[0], tokens[2], Float.valueOf(tokens[3]), true);
				tmpProdList.add(lexProd);
			} else {
				Log.info(0, "ERROR: unexpected line in lexical file " + lexFileName + "\n\t" + line);
				System.exit(1);
			}
		}
		
		// store lexical prods indexed by the word
		lexicalProds = new Vector<List<Production>>(lexSet.numSymbols());
		for (int i=0; i<lexSet.numSymbols(); i++) { lexicalProds.add(null);	}
		
		for (Production p : tmpProdList) {
			if (lexicalProds.get(p.leftChild) == null) {
				lexicalProds.set(p.leftChild, new LinkedList<Production>());
			}
			lexicalProds.get(p.leftChild).add(p);
		}
	}
	
	private void readGrammarFromFile(String gramFileName) throws IOException 
	{
		BufferedReader gramFile = new BufferedReader(new FileReader(gramFileName));
				
		String line;
		String[] tokens;
		Production prod;
		
		Log.info(1,"INFO: Reading grammar from: "+gramFileName);
		while ((line = gramFile.readLine()) != null) {
			tokens = line.split("\\s");
			if (tokens.length == 1) {
				if (startSymbol != -1) {
					Log.info(0,"ERROR: grammar file must contain a single line with a single string representing the START SYMBOL.\nMore than one entry was found.  Last line: " + line);
					System.exit(1);
				} else {
					startSymbol = nonTermSet.getIndex(tokens[0]);
				}
			} else if (tokens.length == 4) {
				// expecting: A -> B prob
				prod = new Production(tokens[0], tokens[2], Float.valueOf(tokens[3]), false);
				// should we make sure there aren't any duplicates?
				unaryProds.add(prod);
			} else if (tokens.length == 5) {
				// expecting: A -> B C prob
				prod = new Production(tokens[0], tokens[2], tokens[3], Float.valueOf(tokens[4]));
				binaryProds.add(prod);
			} else {
				Log.info(0, "ERROR: unexpected line in grammar file " + gramFileName + "\n\t" + line);
				System.exit(1);
			}
		}
	}
	
	
	
	public class Production {
		public int parent, leftChild, rightChild; // if rightChild == -1, it's a unary prod
		public float prob;
		private boolean isLex;
		
		// Binary production
		public Production(String parent, String leftChild, String rightChild, float prob) {
			this.parent = nonTermSet.getIndex(parent);
			this.leftChild = nonTermSet.getIndex(leftChild);
			this.rightChild = nonTermSet.getIndex(rightChild);
			this.prob = prob;
			this.isLex = false;
			
			//Log.info(5, "binary: "+toString());
		}
		
		// Unary production
		public Production(String parent, String child, float prob, boolean isLex) {
			this.parent = nonTermSet.getIndex(parent);
			if (isLex == false) {
				this.leftChild = nonTermSet.getIndex(child);
			} else {
				this.leftChild = lexSet.getIndex(child);
			}
			this.rightChild = -1;
			this.prob = prob;
			this.isLex = isLex;
		}
		
		protected Production() {}
		
		public Production copy() {
			Production p = new Production();
			p.parent = this.parent;
			p.leftChild = this.leftChild;
			p.rightChild = this.rightChild;
			p.prob = this.prob;
			p.isLex = this.isLex;
			
			return p;
		}
		
		public boolean equals(Production otherProd) {
			if (parent != otherProd.parent) return false;
			if (leftChild != otherProd.leftChild) return false;
			if (rightChild != otherProd.rightChild) return false;
			
			return true;
		}
		
		public boolean isUnaryProd() {
			return rightChild == -1;
		}
		
		public boolean isLexProd() {
			return isLex;
		}
		
		public String parentToString() {
			return nonTermSet.getString(parent);
		}
		
		public String childrenToString() {
			if (rightChild != -1) {
				return nonTermSet.getString(leftChild) + " " + nonTermSet.getString(rightChild);
			} else if (isLex == false) {
				return nonTermSet.getString(leftChild);
			} else {
				return lexSet.getString(leftChild);
			}
		}
		
		public String toString() {
			return parentToString() + " -> " + childrenToString() + " (p=" + Double.toString(prob) + ")";
		}
	}
	

	/*
	public class Production {
		public int parent = -1;
		public double prob = -9999999;
		
		public String parentToString() {
			return nonTermSet.getString(parent);
		}
	}
	
	public class BinaryProduction extends Production implements Comparable<BinaryProduction> {
		public int leftChild, rightChild;
		
		public BinaryProduction(String A, String B, String C, double p) {
			parent=nonTermSet.getIndex(A);
			leftChild=nonTermSet.getIndex(B);
			rightChild=nonTermSet.getIndex(C);
			prob=p;
		}
		
		public String toString() {
			return nonTermSet.getString(parent) + " -> " +
				nonTermSet.getString(leftChild) + " " +
				nonTermSet.getString(rightChild) +
				" (p=" + Double.toString(prob) + ")";
		}
		
	    public int compareTo(BinaryProduction other) {
	        if (parent > other.parent) return 1;
	        if (parent < other.parent) return -1;
	        if (leftChild > other.leftChild) return 1;
	        if (leftChild < other.leftChild) return -1;
	        if (rightChild > other.rightChild) return 1;
	        if (rightChild < other.rightChild) return -1;
	        return 0;
	    }	
	}
	
	public class UnaryProduction extends Production {
		public int child;
		
		public UnaryProduction(String A, String B, double p) {
			parent=nonTermSet.getIndex(A);
			child=nonTermSet.getIndex(B);
			prob=p;
		}
		
		public String toString() {
			return nonTermSet.getString(parent) + " -> " +
				nonTermSet.getString(child) + " " +
				" (p=" + Double.toString(prob) + ")";
		}
	}
	
	public class LexicalProduction extends Production {
		public int child;
		
		public LexicalProduction(String A, String B, double p) {
			parent=nonTermSet.getIndex(A);
			child=lexSet.getIndex(B);
			prob=p;
		}
		
		public String toString() {
			return nonTermSet.getString(parent) + " -> " +
				lexSet.getString(child) + " " +
				" (p=" + Double.toString(prob) + ")";
		}
		
		public String childToString() {
			return lexSet.getString(child);
		}
	}
	*/
	
}

package edu.ohsu.cslu.grammar;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import edu.ohsu.cslu.datastructs.vectors.PackedBitVector;
import edu.ohsu.cslu.grammar.Tokenizer.Token;
import edu.ohsu.cslu.parser.util.Log;

public class ArrayGrammar {

    public Production[] binaryProds;
    public Production[] unaryProds;
    public int startSymbol = -1;
    public int nullSymbol = -1;
    public String nullSymbolStr = "<null>";

    // private LinkedList<LexicalProduction>[] lexicalProds = new LinkedList<UnaryProduction>[5];
    // note: java doesn't allow generic array creation, so must do it like this:
    private List<Production>[] lexicalProds;
    public final SymbolSet<String> nonTermSet;
    public final SymbolSet<Integer> posSet; // index into nonTermSet of POS non terms
    private final SymbolSet<String> lexSet;
    private Tokenizer tokenizer;
    private PackedBitVector possibleLeftChild;
    private PackedBitVector possibleRightChild;
    private int maxPOSIndex = -1; // TODO: should sort nonterms so ordered by POS then NTs

    private ArrayGrammar() {
        nonTermSet = new SymbolSet<String>();
        posSet = new SymbolSet<Integer>();
        lexSet = new SymbolSet<String>();
    }

    public ArrayGrammar(final Reader grammarFile, final Reader lexiconFile) throws IOException {
        this();
        init(grammarFile, lexiconFile);
    }

    public ArrayGrammar(final String grammarFile, final String lexiconFile) throws IOException {
        this();
        init(new FileReader(grammarFile), new FileReader(lexiconFile));
    }

    protected void init(final Reader grammarFile, final Reader lexiconFile) throws IOException {
        Log.info(1, "INFO: Reading grammar");

        // the nullSymbol is used for start/end of sentence markers and dummy non-terminals
        nullSymbol = nonTermSet.addSymbol(nullSymbolStr);
        posSet.addSymbol(nullSymbol);

        readGrammar(grammarFile);
        if (startSymbol == -1) {
            throw new IllegalArgumentException("No start symbol found in grammar file.  Expecting a single non-terminal on the first line.");
        }

        Log.info(1, "INFO: Reading lexical productions");
        readLexProds(lexiconFile);
        tokenizer = new Tokenizer(lexSet);
        markLeftRightChildren();
    }

    public Token[] tokenize(final String sentence) throws Exception {
        return tokenizer.tokenize(sentence);
    }

    public int numNonTerms() {
        return nonTermSet.numSymbols();
    }

    public boolean hasWord(final String s) {
        return lexSet.hasLabel(s);
    }

    public List<Production> getLexProdsForToken(final Token token) {
        /*
         * if (token.isUnk()) { // make new lexical prods for the UNK words that will be deleted after parsing the sentence List<Production> unkProds = new
         * LinkedList<Production>(); for (Production p : lexicalProds.get(token.index)){ unkProds.add(p.copy()); }
         * 
         * } else { return lexicalProds.get(token.index); }
         */
        return lexicalProds[token.index];
    }

    // TODO: not efficient. Should index by child
    public List<Production> getUnaryProdsWithChild(final int child) {
        final List<Production> matchingProds = new LinkedList<Production>();
        for (final Production p : unaryProds) {
            if (p.leftChild == child)
                matchingProds.add(p);
        }

        return matchingProds;
    }

    private void markLeftRightChildren() {
        possibleLeftChild = new PackedBitVector(this.numNonTerms());
        possibleRightChild = new PackedBitVector(this.numNonTerms());
        // Arrays.fill(possibleLeftChild, false);
        // Arrays.fill(possibleRightChild, false);
        for (final Production p : binaryProds) {
            possibleLeftChild.set(p.leftChild, true);
            possibleRightChild.set(p.rightChild, true);
        }
    }

    public boolean isLeftChild(final int nonTerm) {
        return possibleLeftChild.getBoolean(nonTerm);
    }

    public boolean isRightChild(final int nonTerm) {
        return possibleRightChild.getBoolean(nonTerm);
    }

    private void readLexProds(final Reader lexFile) throws IOException {
        String line;
        String[] tokens;
        Production lexProd;
        final List<Production> tmpProdList = new LinkedList<Production>();

        final BufferedReader br = new BufferedReader(lexFile);
        while ((line = br.readLine()) != null) {
            tokens = line.split("\\s");
            if (tokens.length == 4) {
                // expecting: A -> B prob
                lexProd = new Production(tokens[0], tokens[2], Float.valueOf(tokens[3]), true);
                tmpProdList.add(lexProd);
            } else {
                throw new IllegalArgumentException("Unexpected line in lexical file\n\t" + line);
            }
        }

        // store lexical prods indexed by the word
        final ArrayList<List<Production>> tmpLexicalProds = new ArrayList<List<Production>>(lexSet.numSymbols());
        for (int i = 0; i < lexSet.numSymbols(); i++) {
            tmpLexicalProds.add(null);
        }

        for (final Production p : tmpProdList) {
            if (tmpLexicalProds.get(p.leftChild) == null) {
                tmpLexicalProds.set(p.leftChild, new LinkedList<Production>());
            }
            tmpLexicalProds.get(p.leftChild).add(p);
        }
        lexicalProds = tmpLexicalProds.toArray(new LinkedList[tmpLexicalProds.size()]);
    }

    private void readGrammar(final Reader gramFile) throws IOException {
        final BufferedReader br = new BufferedReader(gramFile);

        String line;
        String[] tokens;
        Production prod;

        final LinkedList<Production> tmpUnaryProds = new LinkedList<Production>();
        final LinkedList<Production> tmpBinaryProds = new LinkedList<Production>();

        while ((line = br.readLine()) != null) {
            tokens = line.split("\\s");
            if (tokens.length == 1) {

                if (startSymbol != -1) {
                    throw new IllegalArgumentException(
                            "Grammar file must contain a single line with a single string representing the START SYMBOL.\nMore than one entry was found.  Last line: " + line);
                }

                startSymbol = nonTermSet.getIndex(tokens[0]);
            } else if (tokens.length == 4) {
                // expecting: A -> B prob
                prod = new Production(tokens[0], tokens[2], Float.valueOf(tokens[3]), false);
                // should we make sure there aren't any duplicates?
                tmpUnaryProds.add(prod);
            } else if (tokens.length == 5) {
                // expecting: A -> B C prob
                prod = new Production(tokens[0], tokens[2], tokens[3], Float.valueOf(tokens[4]));
                tmpBinaryProds.add(prod);
            } else {
                throw new IllegalArgumentException("Unexpected line in grammar file\n\t" + line);
            }
        }
        unaryProds = tmpUnaryProds.toArray(new Production[tmpUnaryProds.size()]);
        binaryProds = tmpBinaryProds.toArray(new Production[tmpBinaryProds.size()]);
    }

    public class Production {

        public int parent, leftChild, rightChild; // if rightChild == -1, it's a unary prod
        public float prob;
        private boolean isLex;

        // Binary production
        public Production(final String parent, final String leftChild, final String rightChild, final float prob) {
            this.parent = nonTermSet.getIndex(parent);
            this.leftChild = nonTermSet.getIndex(leftChild);
            this.rightChild = nonTermSet.getIndex(rightChild);
            this.prob = prob;
            this.isLex = false;

            // Log.info(5, "binary: "+toString());
        }

        // Unary production
        public Production(final String parent, final String child, final float prob, final boolean isLex) {
            this.parent = nonTermSet.getIndex(parent);
            if (isLex == false) {
                this.leftChild = nonTermSet.getIndex(child);
            } else {
                this.leftChild = lexSet.getIndex(child);
                posSet.addSymbol(this.parent);
                if (this.parent > maxPOSIndex) {
                    maxPOSIndex = this.parent;
                }
            }
            this.rightChild = -1;
            this.prob = prob;
            this.isLex = isLex;
        }

        protected Production() {
        }

        public Production copy() {
            final Production p = new Production();
            p.parent = this.parent;
            p.leftChild = this.leftChild;
            p.rightChild = this.rightChild;
            p.prob = this.prob;
            p.isLex = this.isLex;

            return p;
        }

        public boolean equals(final Production otherProd) {
            if (parent != otherProd.parent)
                return false;
            if (leftChild != otherProd.leftChild)
                return false;
            if (rightChild != otherProd.rightChild)
                return false;

            return true;
        }

        public boolean isUnaryProd() {
            return rightChild == -1;
        }

        public boolean isLexProd() {
            return isLex;
        }

        public String parentToString() {
            return nonTermSet.getSymbol(parent);
        }

        public String childrenToString() {
            if (rightChild != -1) {
                return nonTermSet.getSymbol(leftChild) + " " + nonTermSet.getSymbol(rightChild);
            } else if (isLex == false) {
                return nonTermSet.getSymbol(leftChild);
            } else {
                return lexSet.getSymbol(leftChild);
            }
        }

        @Override
        public String toString() {
            return parentToString() + " -> " + childrenToString() + " (p=" + Double.toString(prob) + ")";
        }
    }

    public int maxPOSIndex() {
        return maxPOSIndex;
    }

    /*
     * public class Production { public int parent = -1; public double prob = -9999999;
     * 
     * public String parentToString() { return nonTermSet.getString(parent); } }
     * 
     * public class BinaryProduction extends Production implements Comparable<BinaryProduction> { public int leftChild, rightChild;
     * 
     * public BinaryProduction(String A, String B, String C, double p) { parent=nonTermSet.getIndex(A); leftChild=nonTermSet.getIndex(B); rightChild=nonTermSet.getIndex(C); prob=p;
     * }
     * 
     * public String toString() { return nonTermSet.getString(parent) + " -> " + nonTermSet.getString(leftChild) + " " + nonTermSet.getString(rightChild) + " (p=" +
     * Double.toString(prob) + ")"; }
     * 
     * public int compareTo(BinaryProduction other) { if (parent > other.parent) return 1; if (parent < other.parent) return -1; if (leftChild > other.leftChild) return 1; if
     * (leftChild < other.leftChild) return -1; if (rightChild > other.rightChild) return 1; if (rightChild < other.rightChild) return -1; return 0; } }
     * 
     * public class UnaryProduction extends Production { public int child;
     * 
     * public UnaryProduction(String A, String B, double p) { parent=nonTermSet.getIndex(A); child=nonTermSet.getIndex(B); prob=p; }
     * 
     * public String toString() { return nonTermSet.getString(parent) + " -> " + nonTermSet.getString(child) + " " + " (p=" + Double.toString(prob) + ")"; } }
     * 
     * public class LexicalProduction extends Production { public int child;
     * 
     * public LexicalProduction(String A, String B, double p) { parent=nonTermSet.getIndex(A); child=lexSet.getIndex(B); prob=p; }
     * 
     * public String toString() { return nonTermSet.getString(parent) + " -> " + lexSet.getString(child) + " " + " (p=" + Double.toString(prob) + ")"; }
     * 
     * public String childToString() { return lexSet.getString(child); } }
     */

}

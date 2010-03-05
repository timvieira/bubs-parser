package edu.ohsu.cslu.grammar;

import java.io.FileReader;
import java.io.Reader;
import java.util.Collection;
import java.util.LinkedList;

import edu.ohsu.cslu.parser.ParserDriver.GrammarFormatType;

public class GrammarByChild extends Grammar {

    protected Collection<Production>[] unaryProdsByChild;
    protected Collection<Production>[] lexicalProdsByChild;

    protected int numLexProds, numUnaryProds;

    public GrammarByChild() {
        super();
    }

    public GrammarByChild(final Reader grammarFile, final Reader lexiconFile, final GrammarFormatType grammarFormat) throws Exception {
        super(grammarFile, lexiconFile, grammarFormat);

        numUnaryProds = unaryProductions.size();
        unaryProdsByChild = storeProductionByChild(unaryProductions, nonTermSet.size() - 1);
        unaryProductions = null; // remove from memory since we now store by child

        numLexProds = lexicalProductions.size();
        lexicalProdsByChild = storeProductionByChild(lexicalProductions, lexSet.size() - 1);
        lexicalProductions = null; // remove from memory since we now store by child

    }

    public GrammarByChild(final String grammarFile, final String lexiconFile, final GrammarFormatType grammarFormat) throws Exception {
        this(new FileReader(grammarFile), new FileReader(lexiconFile), grammarFormat);
    }

    @SuppressWarnings( { "cast", "unchecked" })
    public static Collection<Production>[] storeProductionByChild(final Collection<Production> prods, final int maxIndex) {
        final Collection<Production>[] prodsByChild = (LinkedList<Production>[]) new LinkedList[maxIndex + 1];

        for (final Production p : prods) {
            final int child = p.child();
            if (prodsByChild[child] == null) {
                prodsByChild[child] = new LinkedList<Production>();
            }
            prodsByChild[child].add(p);
        }

        return prodsByChild;
    }

    public static Collection<Production>[] storeProductionByChild(final Collection<Production> prods) {
        int maxChildIndex = -1;
        for (final Production p : prods) {
            if (p.child() > maxChildIndex) {
                maxChildIndex = p.child();
            }
        }
        return storeProductionByChild(prods, maxChildIndex);
    }

    @Override
    public Collection<Production> getUnaryProductionsWithChild(final int child) {
        if (child > unaryProdsByChild.length - 1 || unaryProdsByChild[child] == null) {
            return new LinkedList<Production>();
        }
        return unaryProdsByChild[child];
    }

    @Override
    public final Collection<Production> getLexicalProductionsWithChild(final int child) {
        if (child > lexicalProdsByChild.length - 1 || lexicalProdsByChild[child] == null) {
            return new LinkedList<Production>();
        }
        return lexicalProdsByChild[child];
    }

    @Override
    public int numUnaryProds() {
        return numUnaryProds;
    }

    @Override
    public int numLexProds() {
        return numLexProds;
    }
}

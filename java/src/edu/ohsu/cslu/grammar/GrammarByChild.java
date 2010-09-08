package edu.ohsu.cslu.grammar;

import java.io.FileReader;
import java.io.Reader;
import java.util.Collection;
import java.util.LinkedList;

public class GrammarByChild extends Grammar {

    protected Collection<Production>[] unaryProdsByChild;
    protected Collection<Production>[] lexicalProdsByChild;

    protected int numLexProds, numUnaryProds;

    public GrammarByChild() {
        super();
    }

    public GrammarByChild(final Reader grammarFile) throws Exception {
        super(grammarFile);

        numUnaryProds = unaryProductions.size();
        unaryProdsByChild = storeProductionByChild(unaryProductions, nonTermSet.size() - 1);
        unaryProductions = null; // remove from memory since we now store by child

        lexicalProdsByChild = storeProductionByChild(lexicalProductions, lexSet.size() - 1);
        numLexProds = lexicalProdsByChild.length;
        lexicalProductions = null; // remove from memory since we now store by child

    }

    public GrammarByChild(final String grammarFile) throws Exception {
        this(new FileReader(grammarFile));
    }

    @SuppressWarnings({ "cast", "unchecked" })
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

    public Collection<Production> getUnaryProductionsWithChild(final int child) {
        if (child > unaryProdsByChild.length - 1 || unaryProdsByChild[child] == null) {
            return new LinkedList<Production>();
        }
        return unaryProdsByChild[child];
    }

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

    @Override
    public Production getUnaryProduction(final int parent, final int child) {
        for (final Production p : getUnaryProductionsWithChild(child)) {
            if (p.parent == parent) {
                return p;
            }
        }
        return null;
    }

    @Override
    public Production getLexicalProduction(final int parent, final int lex) {
        for (final Production p : getLexicalProductionsWithChild(lex)) {
            if (p.parent == parent) {
                return p;
            }
        }
        return null;
    }
}

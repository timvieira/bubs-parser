package edu.berkeley.nlp.PCFGLA;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import edu.berkeley.nlp.util.Numberer;
import edu.ohsu.cslu.datastructs.narytree.NaryTree.Binarization;

/**
 * Stores the serialized material representing the grammar and lexicon of a parser, and an Options that specifies things
 * like how unknown words were handled and how distances were binned that will also be needed to parse with the grammar.
 * 
 * @author Slav Petrov
 */
public class ParserData implements Serializable {

    Lexicon lex;
    Grammar gr;
    Map<String, Numberer> numbs;
    short[] numSubStatesArray;
    int h_markov;
    int v_markov;
    Binarization bin;

    public Binarization getBinarization() {
        return bin;
    }

    public short[] getNumSubStatesArray() {
        return numSubStatesArray;
    }

    public Grammar getGrammar() {
        return gr;
    }

    public Lexicon getLexicon() {
        return lex;
    }

    public Map<String, Numberer> getNumbs() {
        return numbs;
    }

    public int getH_markov() {
        return h_markov;
    }

    public int getV_markov() {
        return v_markov;
    }

    public ParserData(final Lexicon lex, final Grammar gr, final Map<String, Numberer> numbs, final short[] nSub,
            final int v_m, final int h_m, final Binarization b) {
        this.lex = lex;
        this.gr = gr;
        this.numbs = numbs;
        this.numSubStatesArray = nSub;
        this.h_markov = h_m;
        this.v_markov = v_m;
        this.bin = b;
    }

    public boolean Save(final String fileName) {
        try {
            // here's some code from online; it looks good and gzips the output!
            // there's a whole explanation at
            // http://www.ecst.csuchico.edu/~amk/foo/advjava/notes/serial.html
            // Create the necessary output streams to save the scribble.
            final FileOutputStream fos = new FileOutputStream(fileName); // Save to
            // file
            final GZIPOutputStream gzos = new GZIPOutputStream(fos); // Compressed
            final ObjectOutputStream out = new ObjectOutputStream(gzos); // Save
            // objects
            out.writeObject(this); // Write the mix of grammars
            out.flush(); // Always flush the output.
            out.close(); // And close the stream.
            gzos.close();
            fos.close();
        } catch (final IOException e) {
            System.out.println("IOException: " + e);
            return false;
        }
        return true;
    }

    public static ParserData Load(final String fileName) {
        ParserData pData = null;
        try {
            final FileInputStream fis = new FileInputStream(fileName); // Load from
            // file
            final GZIPInputStream gzis = new GZIPInputStream(fis); // Compressed
            final ObjectInputStream in = new ObjectInputStream(gzis); // Load objects
            pData = (ParserData) in.readObject(); // Read the mix of grammars
            in.close(); // And close the stream.
            gzis.close();
            fis.close();
        } catch (final IOException e) {
            System.out.println("IOException\n" + e);
            return null;
        } catch (final ClassNotFoundException e) {
            System.out.println("Class not found!");
            return null;
        }
        return pData;
    }

    private static final long serialVersionUID = 1;

}

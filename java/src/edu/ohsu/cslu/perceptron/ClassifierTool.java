/*
 * Copyright 2010-2012 Aaron Dunlop and Nathan Bodenstab
 * 
 * This file is part of the BUBS Parser.
 * 
 * The BUBS Parser is free software: you can redistribute it and/or 
 * modify  it under the terms of the GNU Affero General Public License 
 * as published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * The BUBS Parser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>
 */

package edu.ohsu.cslu.perceptron;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;

import cltool4j.BaseCommandlineTool;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.SerializeModel;
import edu.ohsu.cslu.grammar.SymbolSet;

/**
 * Base class for tagging/classification tools (e.g. a POS-tagger, complete-closure classifier, etc.).
 * 
 * Subclasses of {@link ClassifierTool} extend {@link BaseCommandlineTool}, so they can be used from the command-line
 * for training and standalone classification. Additionally, they expose a programmatic interface, and can be embedded
 * into a larger system (e.g., during preprocessing for parsing).
 * 
 * Training: Executing a tool with the '-ti' option will train on the supplied input (STDIN or files).
 * 
 * Standalone Classification: Without '-ti', the input will be tagged according to the model specified with '-m'.
 * 
 * Training tools must implement {@link #train(BufferedReader)}, and most a separate implement separate 'train()' and
 * 'tag()' methods, for use in unit testing and so that training and tagging can be incorporated into a larger system.
 * 
 * @author Aaron Dunlop
 * 
 * @param <S> The type of {@link BaseSequence} this tool classifies
 */
public abstract class ClassifierTool<S extends Sequence> extends BaseCommandlineTool implements Serializable {

    private static final long serialVersionUID = 2L;

    @Option(name = "-ti", metaVar = "iterations", usage = "Train the tagger for n iterations (Optionally tests on dev-set with '-d' and outputs a model with '-m')")
    int trainingIterations = 2;

    @Option(name = "-ft", requires = "-ti", metaVar = "templates or file", usage = "Feature templates (comma-delimited), or template file")
    protected String featureTemplates = DEFAULT_FEATURE_TEMPLATES();

    @Option(name = "-d", requires = "-ti", metaVar = "file", usage = "Development set. If specified, test results are output after each training iteration.")
    protected File devSet;

    @Option(name = "-m", required = true, metaVar = "file", usage = "Model file (Java serialized object). If testing, the model will be read from this file; if training, the final model will be written to this file.")
    protected File modelFile;

    /**
     * @return A default set of feature templates
     */
    protected abstract String DEFAULT_FEATURE_TEMPLATES();

    protected SymbolSet<String> lexicon;
    protected SymbolSet<String> decisionTreeUnkClassSet;

    protected FeatureExtractor<S> featureExtractor;

    /**
     * Trains a model on the supplied input, optionally validating it on {@link #devSet} and writing it to
     * {@link #modelFile} as a Java serialized object.
     * 
     * @param input Training data
     * @throws IOException
     */
    protected abstract void train(final BufferedReader input) throws IOException;

    /**
     * Reads the model parameters into a temporary object (see {@link Model}) and copies them into this
     * {@link ClassifierTool} instance.
     * 
     * @param is
     * 
     * @throws IOException if unable to read the model from the {@link InputStream}.
     * @throws ClassNotFoundException if unable to interpret the model parameters.
     */
    protected abstract void readModel(final InputStream is) throws IOException, ClassNotFoundException;

    @Override
    protected void setup() throws Exception {
        // Read in a feature file if provided
        final File f = new File(featureTemplates);
        if (f.exists()) {
            featureTemplates = readFeatureTemplateFile(f);
        }
    }

    /**
     * Initializes internal data structures (e.g. {@link ClassifierTool#lexicon} and
     * {@link ClassifierTool#decisionTreeUnkClassSet}) from a {@link Grammar} instance.
     * 
     * @param grammar
     */
    void init(final Grammar grammar) {
        this.lexicon = grammar.lexSet;
        this.lexicon.finalize();
        this.decisionTreeUnkClassSet = grammar.unkClassSet();
        this.decisionTreeUnkClassSet.finalize();
    }

    protected void finalizeMaps() {
        lexicon.finalize();
        decisionTreeUnkClassSet.finalize();
    }

    /**
     * Reads a feature template file and returns a comma-delimited sequence of feature templates. The file format
     * ignores whitespace and uses # to denote comments. E.g.:
     * 
     * <pre>
     * wm2,wm1,w,wp1,wp2 # Unigram word features
     * 
     * tm1_tm2 # Tag i-2,i-1
     * tm1_wm1 # Tag i-1, word i-1
     * tm1_wm2 # Tag i-1, word i-2
     * tm1_w   # Tag i-1, word
     * 
     * tm1_w_d # Tag i-1, word, word-contains-digit
     * </pre>
     * 
     * @param f
     * @return Comma-delimited sequence of feature templates
     * @throws IOException
     */
    protected String readFeatureTemplateFile(final File f) throws IOException {
        final StringBuilder sb = new StringBuilder();
        for (final String line : fileLines(f)) {
            final String uncommented = line.split("#")[0].trim();
            if (uncommented.length() > 0) {
                sb.append(uncommented);
                sb.append(',');
            }
        }
        // Remove the trailing ','
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    /**
     * A simple container class, used only to allow serializing the model in
     * {@link ClassifierTool#train(BufferedReader)} and re-reading it in {@link ClassifierTool#run()} (since there isn't
     * any way to say 'read myself from this {@link ObjectInputStream}). Subclasses will add any additional model
     * representations required.
     * 
     * {@link SerializeModel} will read in and reserialize the entire {@link ClassifierTool}, so during 'real'
     * inference, we can just read the tool(s) we need directly.
     */
    protected static class Model implements Serializable {

        private static final long serialVersionUID = 1L;

        final String featureTemplates;
        final SymbolSet<String> lexicon;
        final SymbolSet<String> unkClassSet;
        final SymbolSet<String> posSet;

        protected Model(final String featureTemplates, final SymbolSet<String> lexicon,
                final SymbolSet<String> unkClassSet, final SymbolSet<String> posSet) {

            this.featureTemplates = featureTemplates;
            this.lexicon = lexicon;
            this.unkClassSet = unkClassSet;
            this.posSet = posSet;
        }
    }
}

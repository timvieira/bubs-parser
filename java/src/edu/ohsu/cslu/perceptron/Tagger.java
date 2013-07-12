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

import edu.ohsu.cslu.grammar.SymbolSet;

/**
 * A generic multiclass averaged-perceptron tagger. Tested primarily as a POS-tagger, but it should be applicable to
 * other tagging tasks as well.
 * 
 * Input: Tagged tokens, one sentence per line. Format: '(tag token) (tag token) ...'
 * 
 * TODO Handle tree input so we can train directly on a treebank
 * 
 * TODO In classification mode, output tagged sequences
 * 
 * @author Aaron Dunlop
 * @since Jul 11, 2013
 */
public class Tagger extends MulticlassClassifier<TagSequence> {

    private static final long serialVersionUID = 1L;

    /**
     * Default Feature Templates:
     * 
     * <pre>
     * # Unigram word, UNK, and tag features
     * wm2,wm1,w,wp1,wp2
     * um2,um1,u,up1,up2
     * tm3,tm2,tm1
     * 
     * # Bigram word features
     * wm2_wm1
     * wm1_w
     * w_wp1
     * wp1_wp2
     * 
     * # Bigram word/UNK features
     * wm2_um1
     * wm1_u
     * u_wp1
     * up1_wp2
     * 
     * # Trigram word features
     * wm2_wm1_w
     * wm1_w_wp1
     * w_wp1_wp2
     * 
     * # Bigram tag features
     * tm3_tm2
     * tm2_tm1
     * 
     * # Word/tag features
     * tm1_wm1
     * tm1_w
     * wm2_tm1
     * </pre>
     */
    @Override
    protected String DEFAULT_FEATURE_TEMPLATES() {
        return "wm2,wm1,w,wp1,wp2,um2,um1,u,up1,up2,tm3,tm2,tm1,wm2_wm1,wm1_w,w_wp1,wp1_wp2,wm2_um1,wm1_u,u_wp1,up1_wp2,wm2_wm1_w,wm1_w_wp1,w_wp1_wp2,tm3_tm2,tm2_tm1,tm1_wm1,tm1_w,wm2_tm1";
    }

    public Tagger() {
        super();
    }

    public Tagger(final String featureTemplates, final SymbolSet<String> lexicon, final SymbolSet<String> unkClassSet,
            final SymbolSet<String> tagSet) {
        super(featureTemplates, lexicon, unkClassSet, tagSet);
    }

    @Override
    protected TaggerFeatureExtractor featureExtractor() {
        return new TaggerFeatureExtractor(featureTemplates, lexicon, decisionTreeUnkClassSet, posSet, tagSet);
    }

    @Override
    protected TagSequence createSequence(final String line) {
        return new TagSequence(line, this);
    }

    public static void main(final String[] args) {
        run(args);
    }

}

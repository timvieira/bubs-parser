# Introduction #

The POS tagger included in BUBS is used primarily as input to its prioritization and pruning processes, but can also be used as a standalone process or embedded in a larger system. It uses the Averaged Perceptron training algorithm, and achieves accuracy of over 96% of standard WSJ test sets (not quite state-of-the-art, but reasonably close). And its throughput is usually around 200k words/second.

## Quick Start (Pre-trained WSJ model) ##

Download [bubs-tagger-20150306.tgz](https://www.dropbox.com/s/cq0pzy9tsl5pri3/bubs-tagger-20150306.tgz?dl=0)

```
tar zxvf bubs-tagger-20150306.tgz
cd bubs-parser
chmod a+x tag
echo "This is a test" | ./tag -m wsj_pos.mdl
```

Or, more generally, from an input file:
```
./tag -m wsj_pos.mdl [input file]
```

Input: one sentence per line, plain text. Note: BUBS will attempt to detect input in parenthesis-tagged format and evaluate accuracy vs. the gold tags.

Output: parenthesis-tagged text, one sentence per line



---


# Training a Custom Tagging Model #

## Training Requirements ##

  * Tagger jar. You can build it as follows (in the bubs-parser directory)

```
ant tag
cp -p build-dist/tag.jar <target directory>
```


  * Gold-standard training set - POS tagged, or Penn Treebank format trees. For this example, we'll assume WSJ sections 02-21 in `input/wsj.02-21.mrgNB.trees`.

  * A development set. For our example, we'll assume WSJ section 22 in `input/wsj.22.mrgNB.trees`.


---

Train the model for 5 iterations with the following command:

```
java -Xmx1g -jar tag.jar -m pos.mdl -ti 5 -d input/wsj.22.mrgNB.trees input/wsj.02-21.mrgNB.trees
```

You can experiment with feature-templates with the `-ft` option, which takes either a comma-separated list of templates (e.g. `-ft wm1,tm1,tm1_w,w,wp1`) or a file containing the same comma-separated format. See the feature template list below). But the default templates work fairly well for standard POS tagging.

The training already output dev-set accuracy, but we can execute the trained model on the same dev-set with a similar command, omitting the `-ti` (training iterations) option:

```
java -Xmx1g -jar tag.jar -m pos.mdl input/wsj.22.mrgNB.trees
```

Since the input was in tree format with gold tags, the tagger outputs only the accuracy measure. If the input is un-tagged text, the output will be in parenthesis-tagged format. E.g.:
```
echo "This is a test" | java -Xmx1g -jar tag.jar -m pos.mdl 
```

## Feature Templates ##
The basic format for any features applying to an element in the sequence is a base template (e.g. `w` for word) and `mx` (minus x) or `px` (plus x) for previous and subsequent words - where `x` is 1, 2, 3, ...

Templates can be combined with an intervening underscore - e.g. tm1\_w combines the tag predicted for the previous word (`tm1`) with the current word being tagged (`w`).

Note that most templates are only defined for a limited window around the current token, usually the 2 previous words and the 2 subsequent words (e.g., wm2 and wp2 are currently defined, but not wm3 or wp3). As of this writing, the following templates are valid - see `edu.ohsu.cslu.perceptron.TaggerFeatureExtractor.TemplateElement` for more details.

  * `w` : Word : supports 2 previous and 2 subsequent tokens (wm2,wm1,w,wp1,wp2)
  * `t` : Tag : the tags predicted for preceding words (note that tags are not available for the current or subsequent words)
  * `pos` : Part-of-speech : available if the sequence has already been POS tagged - not applicable for POS tagging itself, but some taggers can make use of a 2-stage approach, starting with POS tagging followed by a subsequent classification.
  * `u` : Unknown word class : Uses a close relative of the Berkeley Parser unknown word decision tree
  * `num` : True if the token contains a numeral
  * `num20`, `num40`, `num60`, `num80`, `num100` : True if the token consists of at least x% numerals
  * `punct` : True if the token contains one or more punctuation characters
  * `punct20`, `punct40`, `punct60`, `punct80`, `punct100`: Analogous to num20, etc.
  * `us` : Unigram suffix : The last character of the token
  * `bs` : Bigram suffix : The last 2 characters of the token


## Embedding ##
`edu.ohsu.cslu.parser.cellselector.CompleteClosureModel` embeds an instance of the tagger, and can be used as an example if you want to embed it in a similar application (or, if you're interested, please ask, and we can build a simpler example, similar to `EmbeddedExample` for embedding the parser).
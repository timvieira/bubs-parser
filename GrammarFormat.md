# Introduction #

This page describes BUBS grammar format; BUBS can handle this format in plain text, or gzipped (normal for larger grammars)

### Header ###

The first line is metadata output by the grammar training system, formatted as a sequence of `<key>=<value>` pairs.

E.g.
```
lang=UNK format=Berkeley unkThresh=20 start=ROOT_0 hMarkov=0 vMarkov=- date=2013/11/20 vocabSize=890 nBinary=191341 nUnary=7901 nLex=1972453
```

The most important entry is the start symbol, denoted above as `start=ROOT_0`. The other key/value pairs are optional; most are ignored by the parser when reading in a model.

  * lang : Language (unknown in the example above)
  * format : Berkeley or CSLU. Berlekey format is most common; it uses numeric latent annotations to denote state splits (e.g. `NP_0`, `NP_1`, `NN_37`) and `@` to denote factored categories (e.g. `@NP_12`).

### Phrase Level Rules ###
Binary and unary rules are listed, one per line, as
```
<parent> -> <left child> <right child> <log probability (base e)>
```
where `<parent>`, `<left child>`, and `<right child>` are all non-terminals.

Unary rules are formatted identically, excluding the `<right child>`.

### Lexical Rules ###
A single delimiter `===== LEXICON =====` ends the phrase-level section and begins the lexicon. Lexical rules are formatted similarly, but the right child is a terminal.

```
<parent> -> <terminal> <log probability>
```

The parent should be a part-of-speech (POS). In treebank-style grammars, the non-terminal set is generally segmented into 2 disjoint sets - phrase-level labels and POS labels. That division is convenient and recommended, but BUBS does not require or enforce the separation.

Unknown-word labels, taken from the Berkeley parser decision tree hierarchy, are also permitted as terminals.

Example:
```
format=Berkeley start=ROOT
S => NP VP 0
ROOT => S 0
NP => DT NP -1.386294361
NP => DT NN -1.386294361
NP => NN NN -1.791759469
NP => @NP NN -1.791759469
NP => NN RB -1.791759469
@NP => NN NN 0
VP => VB RB -0.693147181
VP => VB -0.693147181
===== LEXICON =====
DT => The 0
NN => fish -0.980829253
NN => market -2.079441542
NN => stands -1.386294361
NN => UNK -1.386294361
VB => market -1.098612289
VB => stands -1.098612289
VB => last -1.098612289
RB => last 0
```
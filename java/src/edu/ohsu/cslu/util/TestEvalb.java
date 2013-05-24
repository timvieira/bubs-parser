package edu.ohsu.cslu.util;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import org.junit.Test;

import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.util.Evalb.BracketEvaluator;
import edu.ohsu.cslu.util.Evalb.EvalbConfig;
import edu.ohsu.cslu.util.Evalb.EvalbResult;

/**
 * Unit tests for {@link Evalb}
 * 
 * Verified vs. Collins evalb 2013-05-24. Note: alternate configurations (implementations of {@link EvalbConfig}) are
 * not well-tested.
 */
public class TestEvalb {

    private String goldString() {
        final StringBuilder sb = new StringBuilder(8096);
        sb.append("(TOP (S (NP (NP (DT The) (NN economy) (POS 's)) (NN temperature)) (VP (MD will) (VP (AUX be) (VP (VBN taken) (PP (IN from) (NP (JJ several) (NN vantage) (NNS points))) (NP (DT this) (NN week)) (, ,) (PP (IN with) (NP (NP (NNS readings)) (PP (IN on) (NP (NP (NN trade)) (, ,) (NP (NN output)) (, ,) (NP (NN housing)) (CC and) (NP (NN inflation))))))))) (. .)))\n");
        sb.append("(TOP (S (NP (DT The) (ADJP (RBS most) (JJ troublesome)) (NN report)) (VP (MD may) (VP (AUX be) (NP (NP (DT the) (NNP August) (NN merchandise) (NN trade) (NN deficit)) (ADJP (JJ due) (ADVP (IN out)) (NP (NN tomorrow)))))) (. .)))\n");
        sb.append("(TOP (S (NP (DT The) (NN trade) (NN gap)) (VP (AUX is) (VP (VBN expected) (S (VP (TO to) (VP (VB widen) (PP (TO to) (NP (QP (IN about) ($ $) (CD 9) (CD billion)))) (PP (IN from) (NP (NP (NNP July) (POS 's)) (QP ($ $) (CD 7.6) (CD billion))))))) (, ,) (PP (VBG according) (PP (TO to) (NP (NP (DT a) (NN survey)) (PP (IN by) (NP (NP (NNP MMS) (NNP International)) (, ,) (NP (NP (DT a) (NN unit)) (PP (IN of) (NP (NP (NNP McGraw-Hill) (NNP Inc.)) (, ,) (NP (NNP New) (NNP York)))))))))))) (. .)))\n");
        sb.append("(TOP (S (NP (NP (NP (NNP Thursday) (POS 's)) (NN report)) (PP (IN on) (NP (DT the) (NNP September) (NN consumer) (NN price) (NN index)))) (VP (AUX is) (VP (VBN expected) (S (VP (TO to) (VP (VB rise) (, ,) (SBAR (IN although) (ADVP (ADVP (RB not) (RB as) (RB sharply)) (PP (IN as) (NP (NP (DT the) (ADJP (CD 0.9) (NN %)) (NN gain)) (VP (VBN reported) (NP (NNP Friday)) (PP (IN in) (NP (DT the) (NN producer) (NN price) (NN index))))))))))))) (. .)))\n");
        sb.append("(TOP (S (NP (DT That) (NN gain)) (VP (AUX was) (VP (AUXG being) (VP (VBD cited) (PP (IN as) (NP (NP (DT a) (NN reason)) (SBAR (S (NP (DT the) (NN stock) (NN market)) (VP (AUX was) (ADVP (IN down)) (ADVP (RB early) (PP (IN in) (NP (NP (NNP Friday) (POS 's)) (NN session)))) (, ,) (SBAR (IN before) (S (NP (PRP it)) (VP (VBD got) (S (VP (VBN started) (PP (IN on) (NP (PRP$ its) (JJ reckless) (JJ 190-point) (NN plunge)))))))))))))))) (. .)))\n");
        sb.append("(TOP (S (NP (NNS Economists)) (VP (AUX are) (VP (VBN divided) (PP (IN as) (PP (TO to) (SBAR (WHNP (WHADVP (WRB how) (JJ much)) (VBG manufacturing) (NN strength)) (S (NP (PRP they)) (VP (VBP expect) (S (VP (TO to) (VP (VB see) (PP (IN in) (NP (NP (NP (NNP September) (NNS reports)) (PP (IN on) (NP (NP (JJ industrial) (NN production)) (CC and) (NP (NN capacity) (NN utilization))))) (, ,) (ADJP (ADVP (RB also)) (JJ due) (NP (NN tomorrow))))))))))))))) (. .)))\n");
        sb.append("(TOP (S (ADVP (RB Meanwhile)) (, ,) (NP (NP (NNP September) (NN housing) (NNS starts)) (, ,) (ADJP (JJ due) (NP (NNP Wednesday))) (, ,)) (VP (AUX are) (VP (VBN thought) (S (VP (TO to) (VP (AUX have) (VP (VBN inched) (ADVP (RB upward)))))))) (. .)))\n");
        sb.append("(TOP (SINV (S (`` ``) (NP (EX There)) (VP (AUX 's) (NP (NP (DT a) (NN possibility)) (PP (IN of) (NP (NP (DT a) (NN surprise)) ('' '') (PP (IN in) (NP (DT the) (NN trade) (NN report)))))))) (, ,) (VP (VBD said)) (NP (NP (NNP Michael) (NNP Englund)) (, ,) (NP (NP (NN director)) (PP (IN of) (NP (NN research))) (PP (IN at) (NP (NNP MMS))))) (. .)))\n");
        sb.append("(TOP (S (S (NP (NP (DT A) (NN widening)) (PP (IN of) (NP (DT the) (NN deficit)))) (, ,) (SBAR (IN if) (S (NP (PRP it)) (VP (AUX were) (VP (VBN combined) (PP (IN with) (NP (DT a) (ADJP (RB stubbornly) (JJ strong)) (NN dollar))))))) (, ,) (VP (MD would) (VP (VB exacerbate) (NP (NN trade) (NNS problems)))) (: --)) (CC but) (S (NP (DT the) (NN dollar)) (VP (VBD weakened) (NP (NNP Friday)) (SBAR (IN as) (S (NP (NNS stocks)) (VP (VBD plummeted)))))) (. .)))\n");
        sb.append("(TOP (S (PP (IN In) (NP (DT any) (NN event))) (, ,) (NP (NP (NNP Mr.) (NNP Englund)) (CC and) (NP (JJ many) (NNS others))) (VP (VBP say) (SBAR (IN that) (S (NP (NP (DT the) (JJ easy) (NNS gains)) (PP (IN in) (S (VP (VBG narrowing) (NP (DT the) (NN trade) (NN gap)))))) (VP (AUX have) (ADVP (RB already)) (VP (AUX been) (VP (VBN made))))))) (. .)))\n");
        sb.append("(TOP (S (`` ``) (S (NP (NN Trade)) (VP (AUX is) (ADVP (RB definitely)) (VP (VBG going) (S (VP (TO to) (VP (AUX be) (ADJP (RBR more) (RB politically) (JJ sensitive)) (PP (IN over) (NP (DT the) (JJ next) (QP (CD six) (CC or) (CD seven)) (NNS months))) (SBAR (IN as) (S (NP (NN improvement)) (VP (VBZ begins) (S (VP (TO to) (VP (VB slow))))))))))))) (, ,) ('' '') (NP (PRP he)) (VP (VBD said)) (. .)))\n");
        sb.append("(TOP (S (S (NP (NNS Exports)) (VP (AUX are) (VP (VBN thought) (S (VP (TO to) (VP (AUX have) (VP (VBN risen) (ADVP (ADVP (RB strongly) (PP (IN in) (NP (NNP August)))) (, ,) (CC but) (ADVP (ADVP (RB probably)) (RB not) (RB enough) (S (VP (TO to) (VP (VB offset) (NP (NP (DT the) (NN jump)) (PP (IN in) (NP (NNS imports)))))))))))))))) (, ,) (NP (NNS economists)) (VP (VBD said)) (. .)))\n");
        sb.append("(TOP (S (NP (NP (NNS Views)) (PP (IN on) (NP (VBG manufacturing) (NN strength)))) (VP (AUX are) (ADJP (VBN split) (PP (IN between) (NP (NP (NP (NNS economists)) (SBAR (WHNP (WP who)) (S (VP (VBP read) (NP (NP (NP (NNP September) (POS 's)) (JJ low) (NN level)) (PP (IN of) (NP (NN factory) (NN job) (NN growth)))) (PP (IN as) (NP (NP (DT a) (NN sign)) (PP (IN of) (NP (DT a) (NN slowdown))))))))) (CC and) (NP (NP (DT those)) (SBAR (WHNP (WP who)) (S (VP (VBP use) (NP (DT the) (ADJP (RB somewhat) (JJR more) (VBG comforting)) (JJ total) (NN employment) (NNS figures)) (PP (IN in) (NP (PRP$ their) (NNS calculations))))))))))) (. .)))\n");
        sb.append("(TOP (S (S (NP (NP (DT The) (JJ wide) (NN range)) (PP (IN of) (NP (NP (NNS estimates)) (PP (IN for) (NP (DT the) (JJ industrial) (NN output) (NN number)))))) (VP (VBZ underscores) (NP (DT the) (NNS differences)))) (: :) (S (NP (DT The) (NNS forecasts)) (VP (VBD run) (PP (IN from) (NP (NP (DT a) (NN drop)) (PP (IN of) (NP (CD 0.5) (NN %))))) (PP (TO to) (NP (NP (DT an) (NN increase)) (PP (IN of) (NP (CD 0.4) (NN %))))) (, ,) (PP (VBG according) (PP (TO to) (NP (NNP MMS)))))) (. .)))\n");
        sb.append("(TOP (S (NP (NP (DT A) (NN rebound)) (PP (IN in) (NP (NN energy) (NNS prices))) (, ,) (SBAR (WHNP (WDT which)) (S (VP (VBD helped) (VP (VB push) (PRT (RP up)) (NP (DT the) (NN producer) (NN price) (NN index)))))) (, ,)) (VP (AUX is) (VP (VBN expected) (S (VP (TO to) (VP (AUX do) (NP (DT the) (JJ same)) (PP (IN in) (NP (DT the) (NN consumer) (NN price) (NN report)))))))) (. .)))\n");
        sb.append("(TOP (S (NP (DT The) (NN consensus) (NN view)) (VP (VBZ expects) (NP (NP (DT a) (ADJP (CD 0.4) (NN %)) (NN increase)) (PP (IN in) (NP (DT the) (NNP September) (NNP CPI)))) (PP (IN after) (NP (NP (DT a) (JJ flat) (NN reading)) (PP (IN in) (NP (NNP August)))))) (. .)))\n");
        sb.append("(TOP (S (NP (NP (NNP Robert) (NNP H.) (NNP Chandross)) (, ,) (NP (NP (DT an) (NN economist)) (PP (IN for) (NP (NP (NP (NNP Lloyd) (POS 's)) (NNP Bank)) (PP (IN in) (NP (NNP New) (NNP York)))))) (, ,)) (VP (AUX is) (PP (IN among) (NP (NP (DT those)) (VP (VBG expecting) (NP (NP (DT a) (ADJP (RBR more) (JJ moderate)) (NN gain)) (PP (IN in) (NP (DT the) (NNP CPI))) (PP (IN than) (PP (IN in) (NP (NP (NNS prices)) (PP (IN at) (NP (DT the) (NN producer) (NN level))))))))))) (. .)))\n");
        sb.append("(TOP (S (`` ``) (S (S (NP (NN Auto) (NNS prices)) (VP (AUX had) (NP (DT a) (JJ big) (NN effect)) (PP (IN in) (NP (DT the) (NNP PPI))))) (, ,) (CC and) (S (PP (IN at) (NP (DT the) (NNP CPI) (NN level))) (NP (PRP they)) (VP (MD wo) (RB n't)))) (, ,) ('' '') (NP (PRP he)) (VP (VBD said)) (. .)))\n");
        sb.append("(TOP (SINV (S (S (NP (NN Food) (NNS prices)) (VP (AUX are) (VP (VBN expected) (S (VP (TO to) (VP (AUX be) (ADJP (JJ unchanged)))))))) (, ,) (CC but) (S (NP (NN energy) (NNS costs)) (VP (VBD jumped) (NP (NP (RB as) (RB much) (IN as) (CD 4)) (NN %))))) (, ,) (VP (VBD said)) (NP (NP (NNP Gary) (NNP Ciminero)) (, ,) (NP (NP (NN economist)) (PP (IN at) (NP (NNP Fleet\\/Norstar) (NNP Financial) (NNP Group))))) (. .)))\n");
        sb.append("(TOP (S (NP (PRP He)) (ADVP (RB also)) (VP (VBZ says) (SBAR (S (NP (PRP he)) (VP (VBZ thinks) (SBAR (S (NP (`` ``) (NP (NN core) (NN inflation)) (, ,) ('' '') (SBAR (WHNP (WDT which)) (S (VP (VBZ excludes) (NP (DT the) (JJ volatile) (NN food) (CC and) (NN energy) (NNS prices))))) (, ,)) (VP (AUX was) (ADJP (JJ strong)) (NP (JJ last) (NN month))))))))) (. .)))\n");
        return sb.toString();
    }

    private String parseString() {
        final StringBuilder sb = new StringBuilder(8096);
        sb.append("(TOP (S (NP (NP (DT The) (NN economy) (POS 's)) (NN temperature)) (VP (MD will) (VP (AUX be) (VP (VBN taken) (PP (IN from) (NP (NP (NP (JJ several) (NN vantage) (NNS points)) (NP (DT this) (NN week)) (, ,)) (PP (IN with) (NP (NP (NNS readings)) (PP (IN on) (NP (NN trade) (, ,) (NN output) (, ,) (NN housing) (CC and) (NN inflation)))))))))) (. .)))\n");
        sb.append("(TOP (S (NP (DT The) (ADJP (RBS most) (JJ troublesome)) (NN report)) (VP (MD may) (VP (AUX be) (NP (DT the) (NNP August) (NN merchandise) (NN trade) (NN deficit)) (PP (JJ due) (IN out) (NP (NN tomorrow))))) (. .)))\n");
        sb.append("(TOP (S (NP (DT The) (NN trade) (NN gap)) (VP (AUX is) (VP (VBN expected) (S (S (VP (TO to) (VP (VB widen) (PP (TO to) (NP (QP (RB about) ($ $) (CD 9) (CD billion)))) (PP (IN from) (NP (NNP July) (POS 's)))))) (NP (QP ($ $) (CD 7.6) (CD billion))) (, ,) (VP (VBG according) (PP (TO to) (NP (NP (DT a) (NN survey)) (PP (IN by) (NP (NNP MMS) (NNP International))))) (, ,) (NP (DT a) (NN unit)) (PP (IN of) (NP (NP (NNP McGraw-Hill) (NNP Inc.)) (, ,) (NNP New) (NNP York)))) (. .))))))\n");
        sb.append("(TOP (S (NP (NNP Thursday) (POS 's)) (VP (VB report) (SBAR (IN on) (S (NP (DT the) (NNP September) (NN consumer) (NN price) (NN index)) (VP (AUX is) (VP (VBN expected) (S (VP (TO to) (VP (VB rise) (, ,) (SBAR (IN although) (S (ADVP (RB not) (RB as) (RB sharply) (PP (IN as) (NP (DT the) (ADJP (CD 0.9) (NN %)) (NN gain)))) (VP (VBD reported) (NP (NNP Friday)) (PP (IN in) (NP (NP (DT the) (NN producer)) (NN price) (NN index)))) (. .)))))))))))))\n");
        sb.append("(TOP (S (NP (DT That) (NN gain)) (VP (AUX was) (VP (AUXG being) (VP (VBD cited) (SBAR (IN as) (S (NP (DT a) (NN reason)) (NP (DT the) (NN stock) (NN market)) (VP (AUX was) (ADJP (RB down) (JJ early) (PP (IN in) (NP (NP (NNP Friday) (POS 's)) (NN session) (, ,) (SBAR (IN before) (S (NP (PRP it)) (VP (VBD got) (VBN started) (PP (IN on) (NP (PRP$ its) (JJ reckless) (JJ 190-point) (NN plunge)))))))))) (. .))))))))\n");
        sb.append("(TOP (S (NP (NNS Economists)) (VP (AUX are) (VP (VBN divided) (PP (IN as) (SBAR (WHPP (TO to) (WHNP (WHADJP (WRB how) (JJ much)) (NN manufacturing) (NN strength))) (S (NP (PRP they)) (VP (VBP expect) (S (VP (TO to) (VP (VB see) (PP (IN in) (NP (NNP September) (NNS reports))) (PP (IN on) (NP (NP (NP (JJ industrial) (NN production)) (CC and) (NN capacity) (NN utilization)) (, ,) (ADJP (RB also) (JJ due)) (NN tomorrow))))))) (. .))))))))\n");
        sb.append("(TOP (S (ADVP (RB Meanwhile)) (, ,) (NP (NNP September) (NN housing) (NNS starts)) (, ,) (NP (JJ due) (NNP Wednesday)) (, ,) (VP (AUX are) (VP (VBD thought) (S (VP (TO to) (VP (AUX have) (VP (VBD inched) (ADVP (RB upward)))))))) (. .)))\n");
        sb.append("(TOP (SINV (`` ``) (S (NP (EX There)) (VP (AUX 's) (NP (DT a) (NN possibility)) (PP (IN of) (NP (DT a) (NN surprise))))) ('' '') (PP (IN in) (NP (DT the) (NN trade) (NN report))) (, ,) (VP (VBD said) (NP (NNP Michael) (NNP Englund))) (, ,) (NP (NP (NN director)) (PP (IN of) (NP (NP (NN research)) (PP (IN at) (NP (NNP MMS)))))) (. .)))\n");
        sb.append("(TOP (S (NP (NP (DT A) (NN widening)) (PP (IN of) (NP (DT the) (NN deficit)))) (, ,) (SBAR (IN if) (S (NP (PRP it)) (VP (AUX were) (VP (VBN combined) (PP (IN with) (NP (DT a) (ADJP (RB stubbornly) (JJ strong)) (NN dollar))))))) (, ,) (VP (MD would) (VP (VB exacerbate) (S (NP (NP (NN trade) (NNS problems)) (: --) (CC but) (NP (DT the) (NN dollar))) (VP (VBN weakened) (S (NP (NP (NNP Friday)) (PP (IN as) (NP (NNS stocks)))) (VP (VBD plummeted))))))) (. .)))\n");
        sb.append("(TOP (S (PP (IN In) (NP (DT any) (NN event))) (, ,) (NP (NNP Mr.) (NNP Englund)) (CC and) (S (NP (JJ many) (NNS others)) (VP (VBP say) (SBAR (WHNP (WDT that)) (S (NP (NP (DT the) (JJ easy) (NNS gains)) (PP (IN in) (NP (NN narrowing)))) (NP (DT the) (NN trade) (NN gap)) (VP (AUX have) (RB already) (VP (AUX been) (VP (VBN made)))) (. .)))))))\n");
        sb.append("(TOP (SINV (`` ``) (S (NP (NNP Trade)) (VP (AUX is) (ADJP (RB definitely) (VBG going)))) (VP (TO to) (VP (AUX be) (ADJP (RBR more) (RB politically) (JJ sensitive) (PP (IN over) (NP (DT the) (JJ next) (CD six) (CC or) (NP (NP (CD seven) (NNS months)) (PP (IN as) (NP (NN improvement))))))))) (VP (VBZ begins)) (VP (TO to) (VP (VB slow))) (, ,) ('' '') (NP (NP (PRP he)) (VP (VBD said))) (. .)))\n");
        sb.append("(TOP (S (NP (NNS Exports)) (VP (AUX are) (VP (VBD thought) (VP (TO to) (VP (AUX have) (VP (VBN risen) (ADVP (RB strongly)) (PP (IN in) (NP (NNP August)))))) (, ,) (CC but) (VP (ADVP (RB probably) (RB not)) (RB enough) (VP (TO to) (VP (VBN offset) (S (NP (NP (DT the) (NN jump)) (PP (IN in) (NP (NNS imports)))) (, ,) (NP (NNS economists)) (VP (VBD said)) (. .)))))))))\n");
        sb.append("(TOP (S (NP (NP (NNP Views)) (PP (IN on) (NP (NN manufacturing) (NN strength)))) (VP (AUX are) (VP (VBD split) (PP (IN between) (NP (NNS economists))) (SBAR (WHNP (WP who)) (S (VP (VB read) (NP (NP (NNP September) (POS 's)) (JJ low) (NN level)) (PP (IN of) (NP (NP (NN factory) (NN job) (NN growth)) (PP (IN as) (NP (NP (DT a) (NN sign)) (PP (IN of) (NP (NP (DT a) (NN slowdown)) (CC and) (NP (NP (DT those) (SBAR (WHNP (WP who)) (S (VP (VB use) (NP (DT the) (ADJP (RB somewhat) (JJR more)) (JJ comforting) (NN total))))) (NN employment) (NNS figures)) (PP (IN in) (NP (PRP$ their) (NNS calculations))))))))))))))) (. .)))\n");
        sb.append("(TOP (S (NP (NP (DT The) (JJ wide) (NN range)) (PP (IN of) (NP (NP (NNS estimates)) (PP (IN for) (NP (DT the) (JJ industrial) (NN output) (NN number)))))) (VP (VBZ underscores) (S (NP (DT the) (NNS differences)) (PRN (: :) (S (NP (DT The) (NNS forecasts)) (VP (VB run) (PP (IN from) (NP (DT a) (NN drop))) (PP (IN of) (NP (NP (CD 0.5) (NN %)) (PP (TO to) (NP (NP (DT an) (NN increase)) (PP (IN of) (NP (CD 0.4) (NN %))))))))) (, ,)) (VP (VBG according) (PP (TO to) (NP (NNP MMS)))) (. .)))))\n");
        sb.append("(TOP (S (NP (NP (DT A) (NN rebound)) (PP (IN in) (NP (NN energy) (NNS prices)))) (, ,) (SBAR (WHNP (WDT which)) (S (VP (VBD helped) (VB push) (PRT (RP up)) (NP (NP (DT the) (NN producer)) (NN price) (NN index))))) (, ,) (VP (AUX is) (VP (VBN expected) (S (VP (TO to) (VP (AUX do) (NP (DT the) (ADJP (JJ same) (PP (IN in) (NP (DT the) (NN consumer) (NN price)))) (NN report))))))) (. .)))\n");
        sb.append("(TOP (S (NP (DT The) (NN consensus) (NN view)) (VP (VBZ expects) (NP (NP (DT a) (ADJP (CD 0.4) (NN %)) (NN increase)) (PP (IN in) (NP (NP (DT the) (NNP September) (NNP CPI)) (PP (IN after) (NP (DT a) (JJ flat) (NN reading)))))) (PP (IN in) (NP (NNP August)))) (. .)))\n");
        sb.append("(TOP (S (NP (NNP Robert) (NNP H.) (NNP Chandross)) (, ,) (NP (NP (DT an) (NN economist)) (PP (IN for) (NP (NNP Lloyd) (POS 's)))) (NP (NP (NNP Bank)) (PP (IN in) (NP (NNP New) (NNP York)))) (, ,) (VP (AUX is) (PP (IN among) (NP (DT those) (PP (VBG expecting) (NP (DT a) (ADJP (RBR more) (JJ moderate)) (NN gain))) (PP (IN in) (NP (DT the) (NNP CPI))))) (PP (IN than) (PP (IN in) (NP (NP (NNS prices)) (PP (IN at) (NP (DT the) (NN producer) (NN level))))))) (. .)))\n");
        sb.append("(TOP (SINV (`` ``) (S (NP (NN Auto) (NNS prices)) (VP (AUX had) (NP (DT a) (JJ big) (NN effect)) (PP (IN in) (NP (DT the) (NNP PPI) (, ,) (CC and) (PP (IN at) (NP (DT the) (NNP CPI) (NN level))) (SBAR (S (NP (PRP they)) (VP (MD wo) (RB n't)))))))) (, ,) ('' '') (NP (NP (PRP he)) (VP (VBD said))) (. .)))\n");
        sb.append("(TOP (S (NP (NNP Food) (NNS prices)) (VP (AUX are) (VP (VBN expected) (S (VP (TO to) (VP (AUX be) (S (NP (JJ unchanged) (, ,) (CC but) (NN energy) (NNS costs)) (VP (VBD jumped) (SBAR (IN as) (S (ADVP (RB much) (PP (IN as) (NP (CD 4) (NN %)))) (, ,) (VP (VBD said) (NP (NAC (NNP Gary) (NNP Ciminero) (, ,)) (NN economist)) (PP (IN at) (NP (NNP Fleet\\/Norstar) (NNP Financial) (NNP Group)))) (. .))))))))))))\n");
        sb.append("(TOP (S (NP (PRP He)) (ADVP (RB also)) (VP (VBZ says) (S (NP (PRP he)) (VP (VBZ thinks) (S (NP (`` ``) (NN core) (NN inflation)) (, ,) ('' '') (SBAR (WHNP (WDT which)) (S (VP (VBZ excludes) (NP (NP (DT the) (JJ volatile) (NN food)) (CC and) (NN energy) (NNS prices))))) (, ,) (VP (AUX was) (NP (JJ strong) (JJ last) (NN month))) (. .)))))))\n");
        return sb.toString();
    }

    @Test
    public void testIndividualTrees() throws IOException {
        final BufferedReader goldReader = new BufferedReader(new StringReader(goldString()));
        final BufferedReader parseReader = new BufferedReader(new StringReader(parseString()));

        EvalbResult r = Evalb.eval(new StringReader(goldReader.readLine()), new StringReader(parseReader.readLine()));
        assertEquals(.8235f, r.precision(), .0001f);
        assertEquals(.7368f, r.recall(), .0001f);

        r = Evalb.eval(new StringReader(goldReader.readLine()), new StringReader(parseReader.readLine()));
        assertEquals(.8889f, r.precision(), .0001f);
        assertEquals(.7273f, r.recall(), .0001f);

        // Tests ADVP / PRT equivalence
        StringReader goldTreeReader = new StringReader(
                "(TOP (S (NP (NP (NN Today) (POS 's)) (NNP Fidelity) (NN ad)) (VP (VBZ goes) (NP (NP (DT a) (NN step)) (ADVP (RBR further))) (, ,) (S (VP (VBG encouraging) (S (NP (NNS investors)) (VP (VP (TO to) (VP (VB stay) (PP (IN in) (NP (DT the) (NN market))))) (CC or) (VP (ADVP (RB even)) (TO to) (VP (VB plunge) (PRT (RP in)) (PP (IN with) (NP (NNP Fidelity)))))))))) (. .)))");
        StringReader parseTreeReader = new StringReader(
                "(TOP (S (NP (NP (NNP Today) (POS 's)) (NNP Fidelity) (NN ad)) (VP (VBZ goes) (ADVP (NP (DT a) (NN step)) (RBR further)) (, ,) (S (VP (VBG encouraging) (S (NP (NNS investors)) (VP (TO to) (VP (VB stay) (UCP (PP (IN in) (NP (DT the) (NN market))) (CC or) (ADVP (RB even)) (S (VP (TO to) (VP (VB plunge) (ADVP (RB in)) (PP (IN with) (NP (NNP Fidelity))))))))))))) (. .)))");
        r = Evalb.eval(goldTreeReader, parseTreeReader);
        assertEquals(.7391f, r.precision(), .0001f);
        assertEquals(.7391f, r.recall(), .0001f);

        goldTreeReader = new StringReader(
                "(TOP (FRAG (NP (NP (NP (NNP Bear) (NNP Stearns) (POS 's)) (JJ chief) (NN economist)) (, ,) (NP (NNP Lawrence) (NNP Kudlow)) (, ,)) (PP (IN in) (NP (NP (DT the) (NNP Sept.) (CD 29) (NN issue)) (PP (IN of) (NP (NP (DT the) (NN firm) (POS 's)) (NNP Global) (NNP Spectator))))) (: :)))");
        parseTreeReader = new StringReader(
                "(TOP (NP (NP (NP (NP (NNP Bear) (NNP Stearns) (POS 's)) (NN chief) (NN economist)) (, ,) (NP (NNP Lawrence) (NNP Kudlow)) (, ,) (PP (IN in) (NP (NP (DT the) (NAC (NNP Sept.) (CD 29)) (NN issue)) (PP (IN of) (NP (NP (DT the) (NN firm) (POS 's)) (NNP Global) (NNP Spectator)))))) (: :)))");
        r = Evalb.eval(goldTreeReader, parseTreeReader);
        assertEquals(.7692f, r.precision(), .0001f);
        assertEquals(.8333f, r.recall(), .0001f);

        goldTreeReader = new StringReader(
                "(TOP (SINV (`` ``) (NP (NP (NN Rally) (. !)) (NP (NN Rally) (. !)) (NP (NN Rally) (. !))) ('' '') (VP (VBD shouted)) (NP (NNP Shearson) (NN trader) (NNP Andy) (NNP Rosen)) (, ,) (S (VP (VBG selling) (NP (JJR more) (NNP Jaguar) (NNS shares)))) (. .)))");
        parseTreeReader = new StringReader(
                "(TOP (SINV (`` ``) (FRAG (NP (NNP Rally)) (. !) (NP (NNP Rally)) (. !) (NP (NNP Rally)) (. !)) ('' '') (VP (VBD shouted)) (NP (NNP Shearson) (NN trader) (NNP Andy) (NNP Rosen)) (, ,) (S (VP (VBG selling) (NP (JJR more) (NNP Jaguar) (NNS shares)))) (. .)))");
        r = Evalb.eval(goldTreeReader, parseTreeReader);
        assertEquals(.6364f, r.precision(), .0001f);
        assertEquals(.6364f, r.recall(), .0001f);
    }

    @Test
    public void testDeletedNodes() throws IOException {
        // Ensure indices match properly after deleting ignored nodes
        final String sentence1419 = "(ROOT (S (NP (NNP DD) (NNP Acquisition) (NNP Corp.)) (VP (VBD said) (SBAR (S (NP (PRP it)) (VP (VBD extended) (NP (NP (PRP$ its) (ADJP ($ $) (JJ 45-a-share)) (NN offer)) (PP (IN for) (NP (NNP Dunkin) ('' ') (NNP Donuts) (NNP Inc.)))) (PP (TO to) (NP (NNP Nov.) (CD 1))) (PP (IN from) (NP (NN yesterday))))))) (. .)))";
        EvalbResult r = Evalb.eval(new StringReader(sentence1419), new StringReader(sentence1419));
        assertEquals(1.0f, r.precision(), .0001f);
        assertEquals(1.0f, r.recall(), .0001f);

        StringReader parseTreeReader = new StringReader(
                "(ROOT (S (NP (NNP DD) (NNP Acquisition) (NNP Corp.)) (VP (VBD said) (SBAR (S (NP (PRP it)) (VP (VBD extended) (NP (NP (PRP$ its) (ADJP ($ $) (JJ 45-a-share)) (NN offer)) (PP (IN for) (NP (NP (NNP Dunkin) (POS ')) (NNP Donuts) (NNP Inc.)))) (PP (TO to) (NP (NNP Nov.) (CD 1))) (PP (IN from) (NP (NN yesterday))))))) (. .)))");
        r = Evalb.eval(new StringReader(sentence1419), parseTreeReader);
        assertEquals(.9444f, r.precision(), .0001f);
        assertEquals(1.0f, r.recall(), .0001f);

        final String sentence92 = "(ROOT (S (S (SBAR (IN That) (S (NP (PRP he)) (VP (VBD was) (NP (DT the) (NNP A) (NNP 's) (JJS winningest) (NN pitcher)) (PP (IN during) (NP (PRP$ its) (NNP American) (NNP League) (NN campaign))) (PP (IN with) (NP (NP (DT a) (CD 21-9) (NN mark)) (, ,) (IN plus) (NP (NP (CD two) (NNS wins)) (PP (IN over) (NP (NNP Toronto))) (PP (IN in) (NP (DT the) (NNS playoffs)))) (, ,)))))) (VP (VBZ indicates) (SBAR (S (NP (PRP he)) (VP (MD may) (VP (VB have) (NP (NP (DT some) (NN evening) (RP up)) (VP (VBG coming))))))))) (, ,) (CC but) (S (PP (IN with) (NP (NP (DT the) (NN way)) (SBAR (S (NP (PRP$ his) (JJ split-fingered) (NN fastball)) (VP (VBZ is) (VP (VBG behaving))))))) (, ,) (NP (IN that)) (VP (MD might) (RB not) (VP (VB be) (NP (DT this) (NN week))))) (. .)))";
        parseTreeReader = new StringReader(
                "(ROOT (S (S (SBAR (IN That) (S (NP (PRP he)) (VP (VBD was) (NP (NP (DT the) (NNP A) (POS 's)) (JJS winningest) (NN pitcher)) (PP (IN during) (NP (PRP$ its) (NNP American) (NNP League) (NN campaign))) (PP (IN with) (NP (DT a) (JJ 21-9) (NN mark))) (, ,) (PP (CC plus) (NP (NP (CD two) (NNS wins)) (PP (IN over) (NP (NNP Toronto))) (PP (IN in) (NP (DT the) (NNS playoffs)))))))) (, ,) (VP (VBZ indicates) (SBAR (S (NP (PRP he)) (VP (MD may) (VP (VB have) (NP (DT some) (NN evening)) (PP (IN up) (S (VBG coming))))))))) (, ,) (CC but) (S (PP (IN with) (NP (NP (DT the) (NN way)) (SBAR (S (NP (PRP$ his) (JJ split-fingered) (NN fastball)) (VP (VBZ is) (VP (VBG behaving))))))) (, ,) (NP (DT that)) (VP (MD might) (RB not) (VP (VB be) (NP (DT this) (NN week))))) (. .)))");
        r = Evalb.eval(new StringReader(sentence92), parseTreeReader);
        assertEquals(.7857f, r.precision(), .0001f);
        assertEquals(.8049f, r.recall(), .0001f);
    }

    @Test
    public void testSmallCorpus() throws IOException {
        final float[] expectedRecall = new float[] { 73.68f, 72.73f, 50.00f, 43.48f, 50.00f, 44.44f, 78.57f, 61.90f,
                76.92f, 55.00f, 30.43f, 44.00f, 50.00f, 71.43f, 76.19f, 71.43f, 64.29f, 62.50f, 17.39f, 52.38f };
        final float[] expectedPrecision = new float[] { 82.35f, 88.89f, 53.85f, 43.48f, 61.90f, 52.17f, 91.67f, 68.42f,
                74.07f, 57.89f, 33.33f, 50.00f, 51.43f, 68.97f, 72.73f, 71.43f, 72.00f, 58.82f, 19.05f, 61.11f };

        final int[] expectedGold = new int[] { 19, 11, 28, 23, 26, 27, 14, 21, 26, 20, 23, 25, 36, 28, 21, 14, 28, 16,
                23, 21 };
        final int[] expectedTest = new int[] { 17, 9, 26, 23, 21, 23, 12, 19, 27, 19, 21, 22, 35, 29, 22, 14, 25, 17,
                21, 18 };
        final int[] expectedMatch = new int[] { 14, 8, 14, 10, 13, 12, 11, 13, 20, 11, 7, 11, 18, 20, 16, 10, 18, 10,
                4, 11 };
        final String[] goldStrings = goldString().split("\n");
        final String[] parseStrings = parseString().split("\n");

        for (int i = 0; i < goldStrings.length; i++) {
            final EvalbResult result = Evalb.eval(new StringReader(goldStrings[i]), new StringReader(parseStrings[i]));
            assertEquals("Mismatched gold on sentence " + i, expectedGold[i], result.goldBrackets);
            assertEquals("Mismatched test on sentence " + i, expectedTest[i], result.parseBrackets);
            assertEquals("Mismatched match on sentence " + i, expectedMatch[i], result.matchedBrackets);
            assertEquals("Mismatched recall on sentence " + i, expectedRecall[i] / 100, result.recall(), .0001);
            assertEquals("Mismatched precision on sentence " + i, expectedPrecision[i] / 100, result.precision(), .0001);
        }

        // And finally, the entire small corpus together
        final EvalbResult result = Evalb.eval(new StringReader(goldString()), new StringReader(parseString()));
        assertEquals(.5976, result.precision(), 0.001);
        assertEquals(.5578, result.recall(), 0.001);
        assertEquals(.5770, result.f1(), 0.001);
        // TODO Add real tests for exact match
        assertEquals(0.0, result.exactMatch(), 0.001);
    }

    /**
     * Tests evaluating a null (failed) parse.
     */
    @Test
    public void testFailedParse() {

        final String sentence1 = "(ROOT (S (NP (DT The) (NN cat)) (VP (VBD runs)) (. .)))";
        final String sentence2 = "(ROOT (S (NP (DT The) (NN dog)) (VP (VBD eats)) (. .)))";

        // For a single failed parse, precision and recall should both be 0
        BracketEvaluator e = new BracketEvaluator();
        e.evaluate(NaryTree.read(sentence1, String.class), null);
        EvalbResult result = e.accumulatedResult();
        assertEquals(0, result.precision(), .0001f);
        assertEquals(0, result.recall(), .0001f);

        // If 1 parse of 2 failed, recall should be penalized by the failed parse
        e = new BracketEvaluator();
        e.evaluate(NaryTree.read(sentence1, String.class), NaryTree.read(sentence1, String.class));
        e.evaluate(NaryTree.read(sentence2, String.class), null);
        result = e.accumulatedResult();
        assertEquals(1f, result.precision(), .0001f);
        assertEquals(0.5f, result.recall(), .0001f);
    }
}

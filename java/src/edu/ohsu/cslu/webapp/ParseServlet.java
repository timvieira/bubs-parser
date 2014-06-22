/*
 * Copyright 2010-2014, Oregon Health & Science University
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

package edu.ohsu.cslu.webapp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.zip.GZIPInputStream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cltool4j.BaseLogger;
import cltool4j.GlobalConfigProperties;
import edu.ohsu.cslu.grammar.DecisionTreeTokenClassifier;
import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashPackingFunction;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.SparseMatrixParser;
import edu.ohsu.cslu.parser.cellselector.CompleteClosureModel;
import edu.ohsu.cslu.parser.fom.BoundaryLex;
import edu.ohsu.cslu.parser.fom.FigureOfMeritModel.FOMType;
import edu.ohsu.cslu.parser.ml.CartesianProductHashSpmlParser;

/**
 * A simple servlet implementation, which deploys BUBS as a service. Takes plain text as input, and returns a parse tree
 * in standard 1-line bracketed format. Loads the grammar, prioritization, and pruning models at servlet initialization,
 * so subsequent parsing should execute quickly.
 * 
 * TODO Cleanup the {@link ThreadLocal} instances in {@link SparseMatrixParser} if the webapp is reloaded (maybe with a
 * context listener?)
 * 
 * @author Aaron Dunlop
 */
public class ParseServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final ParserDriver opts = new ParserDriver();

    // The grammar, FOM, and pruning model are thread-safe, so we can create them 1-time at
    private LeftCscSparseMatrixGrammar grammar;

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException,
            IOException {

        // The parser implementation is _not_ thread-safe, so we have to recreate it for each request. That entails
        // creating a new chart object as well; we could probably use a thread-local to reuse the parser and chart
        // instances, but creating them at request time isn't too terribly expensive.
        final CartesianProductHashSpmlParser parser = new CartesianProductHashSpmlParser(opts, grammar);

        final BufferedReader br = new BufferedReader(new InputStreamReader(req.getInputStream()));
        final BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(resp.getOutputStream()));
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            bw.write(parser.parseSentence(line).naryParse().toString());
        }
        bw.flush();
        resp.getOutputStream().close();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void init() throws ServletException {

        try {
            final ServletConfig config = getServletConfig();

            // Load grammar, prioritization model, and pruning model from classpath ('grammar', 'fom', 'pruning',
            // respectively)
            final String grammarFile = config.getInitParameter("grammar");
            BaseLogger.singleton().info("Loading grammar from " + grammarFile);
            grammar = new LeftCscSparseMatrixGrammar(new InputStreamReader(new GZIPInputStream(getClass()
                    .getClassLoader().getResourceAsStream(grammarFile))), new DecisionTreeTokenClassifier(),
                    PerfectIntPairHashPackingFunction.class);
            opts.setGrammar(grammar);

            final String fomFile = config.getInitParameter("fom");
            BaseLogger.singleton().info("Loading FOM model from " + fomFile);
            opts.fomModel = new BoundaryLex(FOMType.BoundaryLex, grammar, new BufferedReader(new InputStreamReader(
                    new GZIPInputStream(getClass().getClassLoader().getResourceAsStream(fomFile)),
                    Charset.defaultCharset())));

            final String pruningFile = config.getInitParameter("pruning");
            BaseLogger.singleton().info("Loading pruning model from " + pruningFile);
            opts.cellSelectorModel = new CompleteClosureModel(getClass().getClassLoader().getResourceAsStream(
                    pruningFile), null);

            // Set any other init parameters as global config properties
            for (final Enumeration<String> e = config.getInitParameterNames(); e.hasMoreElements();) {
                final String key = e.nextElement();
                if (key.equals("grammar") || key.equals("fom") || key.equals("pruning")) {
                    continue;
                }
                GlobalConfigProperties.singleton().setProperty(key, config.getInitParameter(key));
            }

        } catch (final IOException e) {
            throw new ServletException(e);
        } catch (final ClassNotFoundException e) {
            throw new ServletException(e);
        }
    }
}

/*
 * Copyright (c) 2011, INRIA
 * All rights reserved.
 */
package fr.inria.edelweiss.semantic.analyzer;

import fr.inria.edelweiss.semantic.LayoutExampleAbstractPostProcessor;
import fr.inria.edelweiss.sparql.RdfParser;
import fr.inria.edelweiss.sparql.SparqlDriver;
import org.gephi.graph.api.GraphModel;
import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;
import org.openide.util.Exceptions;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The purpose of this class is to build a semantic graph from the result of a
 * SPARQL request to a SPARQL driver. It is responsible to call the driver
 * parameterized with the request and parse the result to produce the graph.
 *
 * @author Erwan Demairy <Erwan.Demairy@inria.fr>
 */
public class RdfAnalyzer implements LongTask, Runnable {

    private static final Logger logger = Logger.getLogger(RdfAnalyzer.class.getName());
    final GraphModel model;
    final String sparqlRequest;
    private SparqlDriver driver;
    private AbstractPostProcessor abstractPostProcessor;
    private String saveResultName;
    private String sparqlRequestResult;
    private ProgressTicket progressTicket;
    private final int fynLevel;

    /**
     * Constructor.
     *
     * @param newModel         Model to fill.
     * @param newSparqlRequest SPARQL request to fill with the model.
     */
    public RdfAnalyzer(final GraphModel newModel, final String newSparqlRequest, final int fynLevel) {
        super();
        this.model = newModel;
        this.sparqlRequest = newSparqlRequest;
        this.fynLevel = fynLevel;
        abstractPostProcessor = new EmptyAbstractPostProcessor();
    }

    @Override
    public final void run() {
        logger.info("Begin: Building the implementation relationships graph.");
        int waitSeconds = 5;
        Progress.start(progressTicket, waitSeconds);

        try {
            Progress.progress(progressTicket);
            sparqlRequestResult = driver.sparqlQuery(sparqlRequest);

            InputStream rdf = new ByteArrayInputStream(getSparqlRequestResult().getBytes(StandardCharsets.UTF_8));
            RdfParser parser = new RdfParser(rdf, model, fynLevel);

            Progress.progress(progressTicket);
            parser.parse();
            logger.log(Level.INFO, "Number of triples parsed: {0}", parser.getTripleNumber());
        } catch (Exception e) {
            logger.log(Level.INFO, "error when obtaining the nodes and edges: {0}", e.getMessage());
            Exceptions.printStackTrace(e);
        }

        Progress.progress(progressTicket);
        try {
            saveResult(getSparqlRequestResult());
        } catch (Exception e) {
            logger.log(Level.INFO, "error when saving the result: {0}", e.getMessage());
        }
        Progress.progress(progressTicket);
        abstractPostProcessor.setModel(model);
        abstractPostProcessor.run();

        logger.info("End: Building the implementation relationships graph.");
        Progress.finish(progressTicket);
    }

    public final void setPostProcessing(final LayoutExampleAbstractPostProcessor newPostProcessor) {
        this.abstractPostProcessor = newPostProcessor;
    }

    public final SparqlDriver getSparqlEngine() {
        return this.driver;
    }

    public final void setSparqlEngine(final SparqlDriver newDriver) {
        this.driver = newDriver;
    }

    public void setSaveResult(String saveResultName) {
        this.saveResultName = saveResultName;
    }

    private void saveResult(String sparqlRequestResult) throws IOException {
        if (this.saveResultName.isEmpty()) {
            return;
        }
        try (var fSave = new FileOutputStream(saveResultName)) {
            fSave.write(sparqlRequestResult.getBytes());
        }
    }

    /**
     * @return the sparqlRequestResult
     */
    public String getSparqlRequestResult() {
        return sparqlRequestResult;
    }

    @Override
    public boolean cancel() {
        return true;
    }

    @Override
    public void setProgressTicket(ProgressTicket pt) {
        this.progressTicket = pt;
    }

}

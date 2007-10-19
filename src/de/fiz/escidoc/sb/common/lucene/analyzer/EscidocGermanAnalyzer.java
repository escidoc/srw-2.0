package de.fiz.escidoc.sb.common.lucene.analyzer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Analyzer for german escidoc lucene index.
 * 
 * @author MIH
 * @sb
 */
public class EscidocGermanAnalyzer extends EscidocAnalyzer {
    private static Log log = LogFactory.getLog(EscidocGermanAnalyzer.class);

    /**
     * initializes the analyzer.
     * 
     * @sb
     */
    public EscidocGermanAnalyzer() {
        if (log.isDebugEnabled()) {
            log.debug("Initializing EscidocGermanAnalyzer");
        }
        super.setLanguage("de");
    }
}

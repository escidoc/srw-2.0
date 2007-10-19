package de.fiz.escidoc.sb.common.lucene.analyzer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Analyzer for english escidoc lucene index.
 * 
 * @author MIH
 * @sb
 */
public class EscidocEnglishAnalyzer extends EscidocAnalyzer {
    private static Log log = LogFactory.getLog(EscidocEnglishAnalyzer.class);

    /**
     * initializes the analyzer.
     * 
     * @sb
     */
    public EscidocEnglishAnalyzer() {
        if (log.isDebugEnabled()) {
            log.debug("Initializing EscidocEnglishAnalyzer");
        }
        super.setLanguage("en");
    }
}

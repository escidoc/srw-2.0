package de.fiz.escidoc.sb.common.lucene.analyzer;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.snowball.SnowballFilter;

import de.fiz.escidoc.sb.common.Constants;

/**
 * Analyzer for all escidoc lucene indices.
 * 
 * @author MIH
 * @sb
 */
public class EscidocAnalyzer extends Analyzer {
    private String language = null;

    private HashMap<String, String> analyzableFields =
        new HashMap<String, String>();

    private Collection<String> analyzableMatchFields = new ArrayList<String>();

    private HashMap<String, HashMap> supportedLanguages =
        new HashMap<String, HashMap>();

    private static Log log = LogFactory.getLog(EscidocAnalyzer.class);

    /**
     * initializes the analyzer.
     * 
     * @sb
     */
    public EscidocAnalyzer() {
        if (log.isDebugEnabled()) {
            log.debug("Initializing EscidocAnalyzer");
        }
        // fill index-fields that have to get analyzed into HashMap
        analyzableFields = Constants.ANALYZABLE_FIELDS;
        analyzableMatchFields = Constants.ANALYZABLE_MATCH_FIELDS;

        // initialize Analyzers for languages
        supportedLanguages = Constants.SUPPORTED_ANALYZING_LANGUAGES;

    }

    /**
     * Constructs a token stream. -Whitespace tokenization -Language-dependant
     * stemming with SnowballFilter
     * 
     * @param fieldName
     *            name of the Lucene Indexfield.
     * @param reader
     *            reader with field-value
     * 
     * @return TokenStream tokenStream
     * 
     * @sb
     */
    public TokenStream tokenStream(final String fieldName, final Reader reader) {
        if (log.isDebugEnabled()) {
            log.debug("tokenizing with EscidocAnalyzer");
        }

        // Tokenize with WhitespaceTokenizer
        TokenStream result = new XmlWhitespaceTokenizer(reader);

        // reduce to lower case
        result = new JunkFilter(result);
        result = new GreekFilter(result);
        result = new SpecialSignsFilter(result);
        result = new LowerCaseFilter(result);

        // extract field-name
        String reducedFieldName =
            fieldName.replaceFirst(".*\\.", "").toLowerCase();

        // Do further stop-word removal,
        // stemming + normalization for certain fields
        // this can only be done if language is set.
        boolean matchField = false;
        for (Iterator iter = analyzableMatchFields.iterator(); iter.hasNext();) {
            String field = (String) iter.next();
            if (reducedFieldName.matches(".*" + field + ".*")) {
                matchField = true;
            }
        }
        if (analyzableFields.get(reducedFieldName) != null || matchField) {
            if (language != null && supportedLanguages.get(language) != null) {
                // remove stop words
                result =
                    new StopFilter(result,
                        (String[]) ((HashMap) supportedLanguages.get(language))
                            .get("stopwords"));
                // stem
                result =
                    new SnowballFilter(result,
                        (String) ((HashMap) supportedLanguages.get(language))
                            .get("snowballType"));
            }

        }
        return result;
    }

    /**
     * @return Returns the language.
     */
    public String getLanguage() {
        return language;
    }

    /**
     * @param language
     *            The language to set.
     */
    public void setLanguage(final String language) {
        this.language = language;
    }

}

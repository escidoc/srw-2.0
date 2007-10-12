package de.fiz.escidoc.sb.srw.lucene.highlighting;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleFragmenter;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 * Class implements lucene-highlighting of configurable lucene-fields.
 * 
 * @author MIH
 * @sb
 */
public class SimpleHighlighter implements SrwHighlighter {

    public static final String PROPERTY_ANALYZER = "cqlTranslator.analyzer";

    public static final String PROPERTY_HIGHLIGHT_TERM_FULLTEXT =
        "cqlTranslator.highlightTermFulltext";

    private static final int DEFAULT_HIGHLIGHT_FRAGMENT_SIZE = 100;

    private static final int DEFAULT_HIGHLIGHT_MAX_FRAGMENTS = 4;

    private Highlighter highlighter = null;

    private Analyzer analyzer = new SimpleAnalyzer();

    private String highlightStartMarker = "<B>";

    private String highlightEndMarker = "</B>";

    private String highlightFragmentSeparator = "...";

    private int highlightFragmentSize = DEFAULT_HIGHLIGHT_FRAGMENT_SIZE;

    private int highlightMaxFragments = DEFAULT_HIGHLIGHT_MAX_FRAGMENTS;

    private String highlightFulltextField = null;

    private static Log log = LogFactory.getLog(SimpleHighlighter.class);

    /**
     * set properties from config-file into variables.
     * 
     * @param props
     *            properties
     * 
     * @sb
     */
    public void setProperties(final Properties props) {
        String temp;

        temp = (String) props.get(PROPERTY_ANALYZER);
        if (temp != null && temp.trim().length() != 0) {
            try {
                analyzer = (Analyzer) Class.forName(temp).newInstance();
            }
            catch (Exception e) {
                log.error(e);
                analyzer = new StandardAnalyzer();
            }
        }
        temp = (String) props.get(PROPERTY_HIGHLIGHT_TERM_FULLTEXT);
        if (temp != null && temp.trim().length() != 0) {
            highlightFulltextField =
                props.getProperty(PROPERTY_HIGHLIGHT_TERM_FULLTEXT);
        }
    }

    /**
     * initialize lucene-highlighter.
     * 
     * @param indexPath
     *            path to lucene-index
     * @param query
     *            lucene-query
     * 
     * @exception Exception
     *                e
     * 
     * @sb
     */
    public void initialize(final String indexPath, final Query query)
        throws Exception {
        Query replacedQuery = query;
        if (indexPath != null && indexPath.trim().length() != 0
            && query != null) {
            Directory directory = null;
            IndexReader reader = null;
            try {
                directory = FSDirectory.getDirectory(indexPath, false);
                reader = IndexReader.open(directory);
                replacedQuery = query.rewrite(reader);
                // Initialize Highlighter with formatter and scorer
                highlighter =
                    new Highlighter(new SimpleHTMLFormatter(
                        highlightStartMarker, highlightEndMarker),
                        new QueryScorer(replacedQuery));
                // Set Text-Fragmenter
                highlighter.setTextFragmenter(new SimpleFragmenter(
                    highlightFragmentSize));
            }
            finally {
                if (directory != null) {
                    try {
                        directory.close();
                    }
                    catch (IOException e) {
                        log.error(
                            "Exception while closing lucene directory object",
                            e);
                    }
                    directory = null;
                }
                if (reader != null) {
                    try {
                        reader.close();
                    }
                    catch (IOException e) {
                        log.error(
                            "Exception while closing lucene reader object", e);
                    }
                    reader = null;
                }
            }
        }
    }

    /**
     * Gets all highlight-snippets for the given lucene-document and returns it
     * as string.
     * 
     * @param doc
     *            lucene-document
     * @param namespacePrefix
     *            namespacePrefix for xml
     * @exception Exception
     *                e
     * 
     * @return String highlight-xml.
     * 
     * @sb
     */
    public String getFragments(final Document doc, final String namespacePrefix)
        throws Exception {
        if (highlighter == null || highlightFulltextField == null
            || highlightFulltextField.trim().length() == 0) {
            return "";
        }
        String highlightSnippet = "";
        Field highlightField = doc.getField(highlightFulltextField);
        String text = null;
        if (highlightField != null) {
            text = highlightField.stringValue();
        }
        if (text != null && !text.equals("")) {
            TokenStream tokenStream =
                analyzer.tokenStream(highlightFulltextField, new StringReader(
                    text));
            highlightSnippet =
                highlighter.getBestFragments(tokenStream, text,
                    highlightMaxFragments, highlightFragmentSeparator);
        }
        return "<" + namespacePrefix + ":highlight><![CDATA["
            + highlightSnippet + "]]></" + namespacePrefix + ":highlight>";
    }

}

package de.fiz.escidoc.sb.srw.lucene.highlighting;

import java.util.Properties;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;

/**
 * Class is Interface for Objects that return highlighting String for one
 * search-result-document.
 * 
 * @author MIH
 * @sb
 */
public interface SrwHighlighter {

    /**
     * set properties from config-file into variables.
     * 
     * @param props
     *            properties
     * 
     * @sb
     */
    void setProperties(final Properties props);

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
    void initialize(final String indexPath, Query query) throws Exception;

    /**
     * Gets all highlight-snippets for the given lucene-document and returns it
     * as string (possibly xml).
     * 
     * @param doc
     *            lucene-document
     * @param namespacePrefix
     *            namespacePrefix for xml
     * @exception Exception
     *                e
     * 
     * @return String highlight-string.
     * 
     * @sb
     */
    String getFragments(final Document doc, final String namespacePrefix)
        throws Exception;

}

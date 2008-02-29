/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at license/ESCIDOC.LICENSE
 * or http://www.escidoc.de/license.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at license/ESCIDOC.LICENSE.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information: Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 */

/*
 * Copyright 2008 Fachinformationszentrum Karlsruhe Gesellschaft
 * fuer wissenschaftlich-technische Information mbH and Max-Planck-
 * Gesellschaft zur Foerderung der Wissenschaft e.V.  
 * All rights reserved.  Use is subject to license terms.
 */

package de.escidoc.sb.srw.lucene.highlighting;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.TokenStream;
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
public class EscidocHighlighter implements SrwHighlighter {

    //*************Variables from properties-file****************************
	public static final String PROPERTY_ANALYZER = "cqlTranslator.analyzer";

    public static final String PROPERTY_HIGHLIGHT_TERM_FULLTEXT =
        "cqlTranslator.highlightTermFulltext";

    public static final String PROPERTY_HIGHLIGHT_TERM_FULLTEXT_ITERABLE =
        "cqlTranslator.highlightTermFulltextIterable";

    public static final String PROPERTY_HIGHLIGHT_TERM_FILENAME =
        "cqlTranslator.highlightTermFilename";

    public static final String PROPERTY_HIGHLIGHT_TERM_METADATA =
        "cqlTranslator.highlightTermMetadata";

    public static final String PROPERTY_HIGHLIGHT_TERM_METADATA_ITERABLE =
        "cqlTranslator.highlightTermMetadataIterable";

    public static final String PROPERTY_HIGHLIGHT_TERM_PROPERTIES =
        "cqlTranslator.highlightTermProperties";

    public static final String PROPERTY_DEFAULT_INDEX_FIELD =
        "cqlTranslator.defaultIndexField";

    public static final String PROPERTY_FULLTEXT_INDEX_FIELD =
        "cqlTranslator.fulltextIndexField";

    public static final String PROPERTY_HIGHLIGHT_START_MARKER =
        "cqlTranslator.highlightStartMarker";

    public static final String PROPERTY_HIGHLIGHT_END_MARKER =
        "cqlTranslator.highlightEndMarker";

    public static final String PROPERTY_HIGHLIGHT_FRAGMENT_SIZE =
        "cqlTranslator.highlightFragmentSize";

    public static final String PROPERTY_HIGHLIGHT_MAX_FRAGMENTS =
        "cqlTranslator.highlightMaxFragments";

    public static final String PROPERTY_HIGHLIGHT_FRAGMENT_SEPARATOR =
        "cqlTranslator.highlightFragmentSeparator";

    public static final String PROPERTY_HIGHLIGHTER =
        "cqlTranslator.highlighterClass";
    //*************************************************************************

    //********Defaults*********************************************************
    private Highlighter highlighter = null;

    private Analyzer analyzer = new SimpleAnalyzer();

    private String highlightStartMarker = "<B>";

    private String highlightEndMarker = "</B>";

    private String highlightFragmentSeparator = "...";

    private int highlightFragmentSize = 100;

    private int highlightMaxFragments = 4;

    private String fulltextIndexField = null;

    private String highlightFulltextField = null;

    private String highlightFulltextFilenameField = null;

    private String highlightFulltextFieldIterable = "false";

    private String highlightMetadataField = null;

    private String highlightMetadataFieldIterable = "false";
    //*************************************************************************

    private HashSet<String> searchFields = new HashSet<String>();

    private static Log log = LogFactory.getLog(EscidocHighlighter.class);

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
            }
        }
        temp = (String) props.get(PROPERTY_HIGHLIGHT_START_MARKER);
        if (temp != null && temp.trim().length() != 0) {
            highlightStartMarker =
                props.getProperty(PROPERTY_HIGHLIGHT_START_MARKER);
        }
        temp = (String) props.get(PROPERTY_HIGHLIGHT_END_MARKER);
        if (temp != null && temp.trim().length() != 0) {
            highlightEndMarker =
                props.getProperty(PROPERTY_HIGHLIGHT_END_MARKER);
        }
        temp = (String) props.get(PROPERTY_HIGHLIGHT_FRAGMENT_SEPARATOR);
        if (temp != null && temp.trim().length() != 0) {
            highlightFragmentSeparator =
                props.getProperty(PROPERTY_HIGHLIGHT_FRAGMENT_SEPARATOR);
        }
        temp = (String) props.get(PROPERTY_HIGHLIGHT_FRAGMENT_SIZE);
        if (temp != null && temp.trim().length() != 0) {
            highlightFragmentSize =
                new Integer(props.getProperty(PROPERTY_HIGHLIGHT_FRAGMENT_SIZE))
                    .intValue();
        }
        temp = (String) props.get(PROPERTY_HIGHLIGHT_MAX_FRAGMENTS);
        if (temp != null && temp.trim().length() != 0) {
            highlightMaxFragments =
                new Integer(props.getProperty(PROPERTY_HIGHLIGHT_MAX_FRAGMENTS))
                    .intValue();
        }
        temp = (String) props.get(PROPERTY_FULLTEXT_INDEX_FIELD);
        if (temp != null && temp.trim().length() != 0) {
            fulltextIndexField =
                props.getProperty(PROPERTY_FULLTEXT_INDEX_FIELD);
        }
        temp = (String) props.get(PROPERTY_HIGHLIGHT_TERM_FULLTEXT);
        if (temp != null && temp.trim().length() != 0) {
            highlightFulltextField =
                props.getProperty(PROPERTY_HIGHLIGHT_TERM_FULLTEXT);
        }
        temp = (String) props.get(PROPERTY_HIGHLIGHT_TERM_FILENAME);
        if (temp != null && temp.trim().length() != 0) {
            highlightFulltextFilenameField =
                props.getProperty(PROPERTY_HIGHLIGHT_TERM_FILENAME);
        }
        temp = (String) props.get(PROPERTY_HIGHLIGHT_TERM_FULLTEXT_ITERABLE);
        if (temp != null && temp.trim().length() != 0) {
            highlightFulltextFieldIterable =
                props.getProperty(PROPERTY_HIGHLIGHT_TERM_FULLTEXT_ITERABLE);
        }
        temp = (String) props.get(PROPERTY_HIGHLIGHT_TERM_METADATA);
        if (temp != null && temp.trim().length() != 0) {
            highlightMetadataField =
                props.getProperty(PROPERTY_HIGHLIGHT_TERM_METADATA);
        }
        temp = (String) props.get(PROPERTY_HIGHLIGHT_TERM_METADATA_ITERABLE);
        if (temp != null && temp.trim().length() != 0) {
            highlightMetadataFieldIterable =
                props.getProperty(PROPERTY_HIGHLIGHT_TERM_METADATA_ITERABLE);
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
            // get search-fields from query////////////////////////////////////
        	// always highlight stored metadata
            searchFields.add("metadata");
            
            // maybe highlight stored fulltext
            if (fulltextIndexField != null
                && fulltextIndexField.trim().length() != 0) {
                String queryString = query.toString();
                if (queryString.matches(".*" + fulltextIndexField + ".*")) {
                    searchFields.add("fulltext");
                }
            }
            // ////////////////////////////////////////////////////////////////

            // Initialize Highlighter with query, highlight-start + end marker
            // and highlightFragmentSize
            Directory directory = null;
            IndexReader reader = null;
            try {
                directory = FSDirectory.getDirectory(indexPath);
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
     * as xml.
     * xml-structure: 
     * <namespacePrefix:highlight>highlightData</namespacePrefix:highlight>
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
        if (highlighter == null) {
            return "";
        }
        HashMap<String, String> highlightFragmentData = null;
        HighlightXmlizer highlightXmlizer =
            new HighlightXmlizer(highlightFragmentSeparator,
                highlightStartMarker, highlightEndMarker);
        // If search-field was fulltext, highlight fulltext/////////
        if (searchFields.contains("fulltext")
            && highlightFulltextField != null
            && highlightFulltextField.trim().length() != 0) {
            if (highlightFulltextFieldIterable.equalsIgnoreCase("false")) {
                try {
                    highlightFragmentData =
                        getHighlightData(highlightFulltextField,
                            highlightFulltextFilenameField, doc, highlighter,
                            "fulltext");
                    if (highlightFragmentData != null) {
                        highlightXmlizer
                            .addHighlightFragmentData(highlightFragmentData);
                    }
                }
                catch (Exception e) {
                    log.error(e);
                }
            }
            else {
                for (int j = 1;; j++) {
                    try {
                        highlightFragmentData =
                            getHighlightData(highlightFulltextField + j,
                                highlightFulltextFilenameField + j, doc,
                                highlighter, "fulltext");
                        if (highlightFragmentData != null) {
                            highlightXmlizer
                                .addHighlightFragmentData(highlightFragmentData);
                        }
                    }
                    catch (NoSuchFieldException e) {
                        break;
                    }
                    catch (Exception e) {
                        log.error(e);
                    }
                }
            }
        }
        // /////////////////////////////////////////////////////////
        // If search-field was metadata, highlight metadata/////////
        if (searchFields.contains("metadata")
            && highlightMetadataField != null
            && highlightMetadataField.trim().length() != 0) {
            if (highlightMetadataFieldIterable.equalsIgnoreCase("false")) {
                try {
                    highlightFragmentData =
                        getHighlightData(highlightMetadataField, null, doc,
                            highlighter, "metadata");
                    if (highlightFragmentData != null) {
                        highlightXmlizer
                            .addHighlightFragmentData(highlightFragmentData);
                    }
                }
                catch (Exception e) {
                    log.error(e);
                }
            }
            else {
                for (int j = 1;; j++) {
                    try {
                        highlightFragmentData =
                            getHighlightData(highlightMetadataField + j, null,
                                doc, highlighter, "metadata");
                        if (highlightFragmentData != null) {
                            highlightXmlizer
                                .addHighlightFragmentData(highlightFragmentData);
                        }
                    }
                    catch (NoSuchFieldException e) {
                        break;
                    }
                    catch (Exception e) {
                        log.error(e);
                    }
                }
            }

        }
        // /////////////////////////////////////////////////////////
        // If search-field was properties, highlight properties/////////
        // if (searchFields.contains("properties")) {
        // try {
        // highlightFragmentData = getHighlightData(
        // highlightTermMetadata, null, doc, highlighter, "properties");
        // if (highlightFragmentData != null) {
        // highlightXmlizer.addHighlightFragmentData(highlightFragmentData);
        // }
        // } catch (Exception e) {
        // log.error(e);
        // }
        // }
        // /////////////////////////////////////////////////////////
        return highlightXmlizer.xmlize(namespacePrefix);
    }

    /**
     * Gets highlight-snippet and additional data depending on field-name.
     * 
     * @param fieldName
     *            name of Lucene-field
     * @param locatorFieldName
     *            name of Lucene-field that contains link to fulltext
     * @param doc
     *            Lucene Hit-document
     * @param highlighterIn
     *            highlighter to use
     * @param type
     *            type of highlighting-snippet (fulltext or metadata)
     * 
     * @throws NoSuchFieldException
     *             if given field is not found in lucene-index
     * @throws IOException
     *             e
     * 
     * @return HashMap with highlighted text-fragment and additional Data.
     * 
     * @sb
     */
    private HashMap<String, String> getHighlightData(
        final String fieldName, final String locatorFieldName,
        final Document doc, final Highlighter highlighterIn, final String type)
        throws IOException, NoSuchFieldException {
        HashMap<String, String> highlightData = new HashMap<String, String>();
        highlightData.put("type", type);
        String highlightSnippet = null;

        // Get text of all Fields with name <fieldName>////////////////////////
        // and concatenate then with highlightFragmentSeparator////////////////
        StringBuffer fieldValues = new StringBuffer("");
        Field[] fields = doc.getFields(fieldName);
        if (fields != null && fields.length > 0) {
            for (int j = 0; j < fields.length; j++) {
                Field field = fields[j];
                if (fieldValues.length() > 0) {
                    fieldValues.append(highlightFragmentSeparator);
                }
                fieldValues.append(field.stringValue());
            }
        }
        else {
            throw new NoSuchFieldException("Field not found " + fieldName);
        }
        String text = fieldValues.toString();
        // /////////////////////////////////////////////////////////////////////

        text = text.replaceAll("\\s+", " ");
        text = StringEscapeUtils.unescapeXml(text);
        // /////////////////////////////////////////////////////////////////////

        // Highlight text with Highlighter//////////////////////////////////////
        if (text != null && !text.equals("")) {
            TokenStream tokenStream =
                analyzer.tokenStream(fieldName, new StringReader(text));
            highlightSnippet =
                highlighterIn.getBestFragments(tokenStream, text,
                    highlightMaxFragments, highlightFragmentSeparator);
            if (highlightSnippet == null) {
                highlightSnippet = "";
            }
            if (highlightSnippet.equals("")) {
                return null;
            }
            highlightData.put("highlightSnippet", highlightSnippet);
        }
        // /////////////////////////////////////////////////////////////////////
        // Get Information about location of component where hit was found//////
        if (locatorFieldName != null) {
            fields = doc.getFields(locatorFieldName);
            if (fields != null && fields.length > 0) {
                highlightData.put("highlightLocator", fields[0].stringValue());
            }
        }
        // /////////////////////////////////////////////////////////////////////
        return highlightData;
    }

}

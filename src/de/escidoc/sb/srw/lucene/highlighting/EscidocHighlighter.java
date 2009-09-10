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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
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

import de.escidoc.core.common.util.configuration.EscidocConfiguration;
import de.escidoc.sb.srw.Constants;

/**
 * Class implements lucene-highlighting of configurable lucene-fields.
 * 
 * @author MIH
 * @sb
 */
public class EscidocHighlighter implements SrwHighlighter {

    //********Defaults*********************************************************
    private Highlighter highlighter = null;

    private Analyzer analyzer = new StandardAnalyzer();
    
    private SrwHighlightXmlizer highlightXmlizer = 
                    new EscidocSimpleHighlightXmlizer();

    private String highlightStartMarker = "<escidoc-highlight-start>";

    private String highlightEndMarker = "<escidoc-highlight-end>";

    private String highlightFragmentSeparator = "<escidoc-fragment-separator>";

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

    private final Pattern SEARCHFIELD_PATTERN = 
                    Pattern.compile("([^\\s\\\\:]+?):");

    private Matcher SEARCHFIELD_MATCHER = SEARCHFIELD_PATTERN.matcher("");

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

        temp = (String) props.get(Constants.PROPERTY_ANALYZER);
        try {
            //Try to get Analyzer from escidoc-configuration
            String analyzerStr = EscidocConfiguration.getInstance()
            .get(
                EscidocConfiguration.LUCENE_ANALYZER, temp);
            if (analyzerStr != null && analyzerStr.trim().length() != 0) {
                analyzer = (Analyzer) Class.forName(analyzerStr).newInstance();
            } else {
                analyzer = new StandardAnalyzer();
            }
        }
        catch (Exception e) {
            log.error(e);
            analyzer = new StandardAnalyzer();
        }

        temp = (String) props.get(Constants.PROPERTY_HIGHLIGHT_XMLIZER);
        if (temp != null && temp.trim().length() != 0) {
            try {
                highlightXmlizer =
                    (SrwHighlightXmlizer) Class.forName(temp).newInstance();
            }
            catch (Exception e) {
                log.error(e);
                highlightXmlizer = new EscidocSimpleHighlightXmlizer();
            }
        }

        temp = (String) props.get(Constants.PROPERTY_HIGHLIGHT_START_MARKER);
        if (temp != null && temp.trim().length() != 0) {
            highlightStartMarker =
                props.getProperty(Constants.PROPERTY_HIGHLIGHT_START_MARKER);
        }
        temp = (String) props.get(Constants.PROPERTY_HIGHLIGHT_END_MARKER);
        if (temp != null && temp.trim().length() != 0) {
            highlightEndMarker =
                props.getProperty(Constants.PROPERTY_HIGHLIGHT_END_MARKER);
        }
        temp = (String) props.get(Constants.PROPERTY_HIGHLIGHT_FRAGMENT_SEPARATOR);
        if (temp != null && temp.trim().length() != 0) {
            highlightFragmentSeparator =
                props.getProperty(Constants.PROPERTY_HIGHLIGHT_FRAGMENT_SEPARATOR);
        }
        temp = (String) props.get(Constants.PROPERTY_HIGHLIGHT_FRAGMENT_SIZE);
        if (temp != null && temp.trim().length() != 0) {
            highlightFragmentSize =
                new Integer(props.getProperty(Constants.PROPERTY_HIGHLIGHT_FRAGMENT_SIZE))
                    .intValue();
        }
        temp = (String) props.get(Constants.PROPERTY_HIGHLIGHT_MAX_FRAGMENTS);
        if (temp != null && temp.trim().length() != 0) {
            highlightMaxFragments =
                new Integer(props.getProperty(Constants.PROPERTY_HIGHLIGHT_MAX_FRAGMENTS))
                    .intValue();
        }
        temp = (String) props.get(Constants.PROPERTY_FULLTEXT_INDEX_FIELD);
        if (temp != null && temp.trim().length() != 0) {
            fulltextIndexField =
                props.getProperty(Constants.PROPERTY_FULLTEXT_INDEX_FIELD);
        }
        temp = (String) props.get(Constants.PROPERTY_HIGHLIGHT_TERM_FULLTEXT);
        if (temp != null && temp.trim().length() != 0) {
            highlightFulltextField =
                props.getProperty(Constants.PROPERTY_HIGHLIGHT_TERM_FULLTEXT);
        }
        temp = (String) props.get(Constants.PROPERTY_HIGHLIGHT_TERM_FILENAME);
        if (temp != null && temp.trim().length() != 0) {
            highlightFulltextFilenameField =
                props.getProperty(Constants.PROPERTY_HIGHLIGHT_TERM_FILENAME);
        }
        temp = (String) props.get(Constants.PROPERTY_HIGHLIGHT_TERM_FULLTEXT_ITERABLE);
        if (temp != null && temp.trim().length() != 0) {
            highlightFulltextFieldIterable =
                props.getProperty(Constants.PROPERTY_HIGHLIGHT_TERM_FULLTEXT_ITERABLE);
        }
        temp = (String) props.get(Constants.PROPERTY_HIGHLIGHT_TERM_METADATA);
        if (temp != null && temp.trim().length() != 0) {
            highlightMetadataField =
                props.getProperty(Constants.PROPERTY_HIGHLIGHT_TERM_METADATA);
        }
        temp = (String) props.get(Constants.PROPERTY_HIGHLIGHT_TERM_METADATA_ITERABLE);
        if (temp != null && temp.trim().length() != 0) {
            highlightMetadataFieldIterable =
                props.getProperty(Constants.PROPERTY_HIGHLIGHT_TERM_METADATA_ITERABLE);
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
        searchFields = new HashSet<String>();
        if (indexPath != null && indexPath.trim().length() != 0
            && query != null && query.toString() != null) {

            // get search-fields from query////////////////////////////////////
            SEARCHFIELD_MATCHER.reset(query.toString());
            boolean fulltextFound = false;
            boolean nonFulltextFound = false;
            
            while (SEARCHFIELD_MATCHER.find()) {
                if (SEARCHFIELD_MATCHER.group(1) != null
                        && fulltextIndexField != null
                        && SEARCHFIELD_MATCHER.group(1)
                            .matches(".*" + fulltextIndexField + ".*")) {
                    fulltextFound = true;
                } else {
                    nonFulltextFound = true;
                }
                if (fulltextFound && nonFulltextFound) {
                    break;
                }
            }
            if (fulltextFound) {
                searchFields.add("fulltext");
            }
            if (nonFulltextFound) {
                searchFields.add("metadata");
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
     * @exception Exception
     *                e
     * 
     * @return String highlight-xml.
     * 
     * @sb
     */
    public String getFragments(final Document doc)
        throws Exception {
        if (highlighter == null) {
            return "";
        }

        //clear highlightXmlizer fragment data
        highlightXmlizer.clearHighlightFragmentData();
        //get properties like highlight-start-marker etc.
        highlightXmlizer.setProperties(getCustomProperties());
        
        HashMap<String, String> highlightFragmentData = null;
        // Get highlight-snippets from luene-highlighter
        //and add them to highlight-xmlizer
        // If search-field was fulltext, highlight fulltext.
        if (searchFields.contains("fulltext")
            && highlightFulltextField != null
            && highlightFulltextField.trim().length() != 0) {
            if (!highlightFulltextFieldIterable.equalsIgnoreCase("true")) {
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
            if (!highlightMetadataFieldIterable.equalsIgnoreCase("true")) {
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
        //generate highlight-xml from highlight-data
        return highlightXmlizer.xmlize();
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
        
        //check values
        if (fieldName == null || fieldName.trim().length() == 0
                || type == null || type.trim().length() == 0) {
            return null;
        }
        
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
            //remove non-valid unicode-characters
            highlightSnippet = stripNonValidXMLCharacters(highlightSnippet);
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

    /**
     * This method ensures that the output String has only
     * valid XML unicode characters as specified by the
     * XML 1.0 standard. For reference, please see
     * <a href="http://www.w3.org/TR/2000/REC-xml-20001006#NT-Char">the
     * standard</a>. This method will return an empty
     * String if the input is null or empty.
     *
     * @param in The String whose non-valid characters we want to remove.
     * @return The in String, stripped of non-valid characters.
     */
    public String stripNonValidXMLCharacters(String in) {
        StringBuffer out = new StringBuffer(); // Used to hold the output.
        char current; // Used to reference the current character.

        if (in == null || ("".equals(in))) return ""; // vacancy test.
        for (int i = 0; i < in.length(); i++) {
            current = in.charAt(i); // NOTE: No IndexOutOfBoundsException caught here; it should not happen.
            if ((current == 0x9) ||
                (current == 0xA) ||
                (current == 0xD) ||
                ((current >= 0x20) && (current <= 0xD7FF)) ||
                ((current >= 0xE000) && (current <= 0xFFFD)) ||
                ((current >= 0x10000) && (current <= 0x10FFFF)))
                out.append(current);
        }
        return out.toString();
    } 
    
    /**
     * This method puts the generated properties (defaults + evtl overwritten)
     * into a properties-Object.
     *
     * @return The Properties.
     */
    private Properties getCustomProperties() {
        Properties props = new Properties();
        props.put(Constants.PROPERTY_HIGHLIGHT_START_MARKER, 
                                        highlightStartMarker);
        props.put(Constants.PROPERTY_HIGHLIGHT_END_MARKER, 
                                        highlightEndMarker);
        props.put(Constants.PROPERTY_HIGHLIGHT_FRAGMENT_SEPARATOR, 
                                        highlightFragmentSeparator);
        return props;
    }
    
}

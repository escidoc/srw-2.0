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
 * Copyright 2006-2007 Fachinformationszentrum Karlsruhe Gesellschaft
 * für wissenschaftlich-technische Information mbH and Max-Planck-
 * Gesellschaft zur Förderung der Wissenschaft e.V.  
 * All rights reserved.  Use is subject to license terms.
 */

package de.escidoc.sb.srw.lucene;

import gov.loc.www.zing.srw.ExtraDataType;
import gov.loc.www.zing.srw.SearchRetrieveRequestType;
import gov.loc.www.zing.srw.TermType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.axis.types.NonNegativeInteger;
import org.apache.axis.types.PositiveInteger;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexReader.FieldOption;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FieldDoc;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RangeQuery;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortComparatorSource;
import org.apache.lucene.search.SortField;
import org.osuosl.srw.ResolvingQueryResult;
import org.osuosl.srw.SRWDiagnostic;
import org.osuosl.srw.lucene.LuceneTranslator;
import org.z3950.zing.cql.CQLAndNode;
import org.z3950.zing.cql.CQLBooleanNode;
import org.z3950.zing.cql.CQLNode;
import org.z3950.zing.cql.CQLNotNode;
import org.z3950.zing.cql.CQLOrNode;
import org.z3950.zing.cql.CQLTermNode;

import ORG.oclc.os.SRW.QueryResult;
import de.escidoc.sb.srw.lucene.highlighting.SrwHighlighter;
import de.escidoc.sb.srw.lucene.queryParser.EscidocQueryParser;
import de.escidoc.sb.srw.lucene.sorting.EscidocSearchResultComparator;

/**
 * Class overwrites org.osuosl.srw.lucene.LuceneTranslator. This is done
 * because: -we dont retrieve and store all search-hits but only the ones
 * requested -we dont use result-sets -we do sorting while querying lucene and
 * not afterwards -we have to rewrite the CQLTermNodes because we have to
 * replace default search field cql.serverChoice with configured default-search
 * field -we have to analyze the terms with the analyzer -enable fuzzy search
 * 
 * @author MIH
 * @sb
 */
public class EscidocLuceneTranslator extends LuceneTranslator {

    private static Log log = LogFactory.getLog(EscidocLuceneTranslator.class);

    public static final String PROPERTY_ANALYZER = "cqlTranslator.analyzer";

    public static final String PROPERTY_DEFAULT_INDEX_FIELD =
        "cqlTranslator.defaultIndexField";

    public static final String PROPERTY_HIGHLIGHTER =
        "cqlTranslator.highlighterClass";

    public static final String PROPERTY_COMPARATOR =
        "cqlTranslator.sortComparator";

    public static final int BOOLEAN_MAX_CLAUSE_COUNT = 1000000;

    public static final int DIAGNOSTIC_CODE_NINETEEN = 19;

    public static final int DIAGNOSTIC_CODE_TWENTY = 19;

    public static final int DIAGNOSTIC_CODE_FOURTYSEVEN = 47;

    /**
     * Default Index Field. Is static because it is used in overwritten static
     * method
     */
    private String defaultIndexField;

    /**
     * @return String defaultIndexField.
     */
    public String getDefaultIndexField() {
        return defaultIndexField;
    }

    /**
     * @param inp
     *            defaultIndexField.
     */
    public void setDefaultIndexField(final String inp) {
        defaultIndexField = inp;
    }

    /**
     * SrwHighlighter.
     */
    private SrwHighlighter highlighter = null;

    /**
     * @return String highlighter.
     */
    public SrwHighlighter getHighlighter() {
        return highlighter;
    }

    /**
     * @param inp
     *            highlighter.
     */
    public void setHighlighter(final SrwHighlighter inp) {
        highlighter = inp;
    }

    /**
     * Analyzer. Is static because it is used in overwritten static method
     * Default: StandardAnalyzer
     */
    private Analyzer analyzer;

    /**
     * @return String analyzer.
     */
    public Analyzer getAnalyzer() {
        return analyzer;
    }

    /**
     * @param inp
     *            analyzer.
     */
    public void setAnalyzer(final Analyzer inp) {
        analyzer = inp;
    }

    /**
     * Comparator for custom sorting of search-result.
     * Default: EscidocSearchResultComparator
     */
    private SortComparatorSource comparator;

    /**
     * @return String comparator.
     */
    public SortComparatorSource getComparator() {
        return comparator;
    }

    /**
     * @param inp comparator.
     */
    public void setComparator(final SortComparatorSource inp) {
    	comparator = inp;
    }

    /**
     * construct.
     * 
     * @sb
     */
    public EscidocLuceneTranslator() {
        setIdentifierTerm(DEFAULT_ID_TERM);
    }

    /**
     * construct with path to lucene-index.
     * 
     * @param indexPath
     *            path to lucene-index
     * @throws IOException
     *             e
     * @sb
     */
    public EscidocLuceneTranslator(final String indexPath) throws IOException {
        setIndexPath(indexPath);
        setIdentifierTerm(DEFAULT_ID_TERM);
    }

    /**
     * construct with path to lucene-index and identifierTerm(field that
     * contains xml that gets returned by search).
     * 
     * @param indexPath
     *            path to lucene-index
     * @param identifierTerm
     *            field that contains xml that gets returned by search
     * @throws IOException
     *             e
     * @sb
     */
    public EscidocLuceneTranslator(final String indexPath,
        final String identifierTerm) throws IOException {
        setIndexPath(indexPath);
        setIdentifierTerm(identifierTerm);
    }

    /**
     * initialize.
     * 
     * @param properties
     *            properties
     * 
     * @sb
     */
    public void init(final Properties properties) {
        String temp;

        temp = (String) properties.get(PROPERTY_INDEXPATH);
        if (temp != null && temp.trim().length() != 0) {
            temp = replaceEnvVariables(temp);
            setIndexPath(temp);
        }

        temp = (String) properties.get(PROPERTY_DEFAULT_INDEX_FIELD);
        if (temp != null && temp.trim().length() != 0) {
            setDefaultIndexField(temp);
        }

        temp = (String) properties.get(PROPERTY_IDENTIFIER_TERM);
        if (temp != null && temp.trim().length() != 0) {
            setIdentifierTerm(temp);
        }

        temp = (String) properties.get(PROPERTY_HIGHLIGHTER);
        if (temp != null && temp.trim().length() != 0) {
            try {
                highlighter =
                    (SrwHighlighter) Class.forName(temp).newInstance();
                highlighter.setProperties(properties);
            }
            catch (Exception e) {
                log.error(e);
                highlighter = null;
            }
        }

        temp = (String) properties.get(PROPERTY_ANALYZER);
        if (temp != null && temp.trim().length() != 0) {
            try {
                analyzer = (Analyzer) Class.forName(temp).newInstance();
            }
            catch (Exception e) {
                log.error(e);
                analyzer = new StandardAnalyzer();
            }
        }

        temp = (String) properties.get(PROPERTY_COMPARATOR);
        if (temp != null && temp.trim().length() != 0) {
            try {
                comparator = (SortComparatorSource) Class.forName(temp).newInstance();
            }
            catch (Exception e) {
                log.error(e);
                comparator = new EscidocSearchResultComparator();
            }
        }

    }

    /**
     * overwritten method from LuceneTranslator. Just calls new implemented
     * method in this class, but without SearchRetrieveRequestType-object.
     * SearchRetrieveRequestType-object is needed to get startRecord,
     * maximumRecords, sortKeys
     * 
     * @param queryRoot
     *            cql-query
     * @param extraDataType
     *            extraDataType
     * @return QueryResult queryResult-Object
     * @throws SRWDiagnostic
     *             e
     * 
     * @sb
     */
    public QueryResult search(
        final CQLNode queryRoot, final ExtraDataType extraDataType)
        throws SRWDiagnostic {
        return search(queryRoot, extraDataType, null);
    }

    /**
     * New implemented method in this class with
     * SearchRetrieveRequestType-object. SearchRetrieveRequestType-object is
     * needed to get startRecord, maximumRecords, sortKeys. Method searches
     * Lucene-index with sortKeys, only gets requested records from Hits, get
     * data from identifierTerm-field and puts it into queryResult.
     * 
     * @param queryRoot
     *            cql-query
     * @param extraDataType
     *            extraDataType
     * @param request
     *            SearchRetrieveRequestType
     * @return QueryResult queryResult-Object
     * @throws SRWDiagnostic
     *             e
     * 
     * @sb
     */
    public QueryResult search(
        final CQLNode queryRoot, final ExtraDataType extraDataType,
        final SearchRetrieveRequestType request) throws SRWDiagnostic {
        // Increase maxClauseCount of BooleanQuery for Wildcard-Searches
        BooleanQuery.setMaxClauseCount(BOOLEAN_MAX_CLAUSE_COUNT);

        // Get Lucene Sort-Object
        Sort sort = getLuceneSortObject(request.getSortKeys());

        String[] identifiers = null;
        IndexSearcher searcher = null;

        try {
            // convert the CQL search to lucene search
            // while doing that, call method get getAnalyzedCqlTermNode
            // Additionally replaces fieldname cql.serverChoice
            // (this is the case if user gives no field name)
            // with the defaultFieldName from configuration
            Query unanalyzedQuery = makeQuery(queryRoot);

            // rewrite query to analyzed query
            QueryParser parser =
                new EscidocQueryParser(getDefaultIndexField(), analyzer);
            Query query = parser.parse(unanalyzedQuery.toString());

            log.info("escidoc lucene search=" + query);

            try {
                searcher = new IndexSearcher(getIndexPath());
            }
            catch (Exception e) {
                log.info(e);
            }
            int size = 0;
            Hits results = null;
            if (searcher != null) {
                // perform sorted search?
                if (sort == null) {
                    results = searcher.search(query);
                }
                else {
                    results = searcher.search(query, sort);
                }
                size = results.length();

                // initialize Highlighter
                if (highlighter != null) {
                    highlighter.initialize(getIndexPath(), query);
                }
            }

            log.info(size + " handles found");
            identifiers = new String[size];

            /**
             * get startRecord
             */
            if (size == 0 && request.getStartRecord() != null) {
                throw new SRWDiagnostic(
                    SRWDiagnostic.FirstRecordPositionOutOfRange,
                    "StartRecord > endRecord");
            }
            int startRecord = 1;
            PositiveInteger startRecordInt = request.getStartRecord();
            if (startRecordInt != null) {
                startRecord = startRecordInt.intValue();
            }

            /**
             * get endRecord
             */
            int maxRecords = DIAGNOSTIC_CODE_TWENTY;
            int endRecord = 0;
            NonNegativeInteger maxRecordsInt = request.getMaximumRecords();
            if (maxRecordsInt != null) {
                maxRecords = maxRecordsInt.intValue();
            }
            endRecord = startRecord - 1 + maxRecords;
            if (endRecord > size) {
                endRecord = size;
            }
            if (endRecord < startRecord && endRecord > 0) {
                throw new SRWDiagnostic(
                    SRWDiagnostic.FirstRecordPositionOutOfRange,
                    "StartRecord > endRecord");
            }

            // now instantiate the results and put them into the response object
            if (log.isDebugEnabled()) {
                log.debug("iterating resultset from record " + startRecord
                    + " to " + endRecord);
            }
            for (int i = startRecord - 1; i < endRecord; i++) {
                org.apache.lucene.document.Document doc = results.doc(i);
                if (log.isDebugEnabled()) {
                    log.debug("identifierTerm: " + getIdentifierTerm());
                }
                Field idField = doc.getField(getIdentifierTerm());
                if (idField != null) {
                    identifiers[i] = createIdentifier(doc, idField);
                }
            }
        }
        catch (Exception e) {
            throw new SRWDiagnostic(SRWDiagnostic.GeneralSystemError, e
                .toString());
        }
        finally {
            if (searcher != null) {
                try {
                    searcher.close();
                }
                catch (IOException e) {
                    log.error("Exception while closing lucene index searcher",
                        e);
                }
                searcher = null;
            }
        }
        return new ResolvingQueryResult(identifiers);
    }

    /**
     * Scan-Request. Scans index for terms that are alphabetically around the
     * search-term.
     * 
     * @param queryRoot
     *            cql-query
     * @param extraDataType
     *            extraDataType
     * @return TermType[] Array of TermTypes
     * @throws Exception
     *             e
     * 
     * @sb
     */
    public TermType[] scan(
        final CQLNode queryRoot, final ExtraDataType extraDataType)
        throws Exception {

        TermType[] response = new TermType[0];
        Map termMap = new HashMap();
        IndexSearcher searcher = null;

        try {
            // convert the CQL search to lucene search
            Query query = makeQuery(queryRoot);
            log.info("lucene search=" + query);

            /**
             * scan query should always be a single term, just get that term's
             * qualifier
             */
            String searchField = ((CQLTermNode) queryRoot).getQualifier();
            boolean exact =
                ((CQLTermNode) queryRoot)
                    .getRelation().toCQL().equalsIgnoreCase("exact");

            // perform search
            searcher = new IndexSearcher(getIndexPath());
            Hits results = searcher.search(query);
            int size = results.length();

            log.info(size + " handles found");

            if (size != 0) {
                // iterater through hits counting terms
                for (int i = 0; i < size; i++) {
                    org.apache.lucene.document.Document doc = results.doc(i);

                    // MIH: Changed: get all fileds and not only one.
                    // Concat fieldValues into fieldString
                    Field[] fields = doc.getFields(searchField);
                    StringBuffer fieldValue = new StringBuffer("");
                    if (fields != null) {
                        for (int j = 0; j < fields.length; j++) {
                            fieldValue.append(fields[j].stringValue()).append(
                                " ");
                        }
                    }
                    // /////////////////////////////////////////////////////////

                    if (exact) {
                        // each field is counted as a term
                        countTerm(termMap, fieldValue.toString());

                    }
                    else {
                        /**
                         * each word in the field is counted as a term. A term
                         * should only be counted once per document so use a
                         * tokenizer and a Set to create a list of unique terms
                         * 
                         * this is the default scan but can be explicitly
                         * invoked with the "any" keyword.
                         * 
                         * example: 'dc.title any fish'
                         */
                        StringTokenizer tokenizer =
                            new StringTokenizer(fieldValue.toString(), " ");
                        Set termSet = new HashSet();
                        while (tokenizer.hasMoreTokens()) {
                            termSet.add(tokenizer.nextToken());
                        }
                        // count all terms
                        Iterator iter = termSet.iterator();
                        while (iter.hasNext()) {
                            String term = (String) iter.next();
                            countTerm(termMap, term);
                        }
                    }
                }

                // done counting terms in all documents, convert map to array
                response = (TermType[]) termMap.values().toArray(response);
            }

        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if (searcher != null) {
                try {
                    searcher.close();
                }
                catch (IOException e) {
                    log.error("Exception while closing lucene index searcher",
                        e);
                }
                searcher = null;
            }
        }

        return response;
    }

    /**
     * Counts the term. If the term matches an existing term it is added on to
     * the count for that term. If
     * 
     * @param termMap -
     *            map of terms already counted
     * @param value -
     *            value of the term
     */
    private void countTerm(final Map termMap, final String value) {
        TermType termType = (TermType) termMap.get(value);
        if (termType == null) {
            // not found, create
            termType = new TermType();
            termType.setValue(value);
            termType.setNumberOfRecords(new NonNegativeInteger(Integer
                .toString(1)));
            termMap.put(value, termType);
        }
        else {
            NonNegativeInteger count = termType.getNumberOfRecords();
            int newValue = count.intValue() + 1;
            termType.setNumberOfRecords(new NonNegativeInteger(Integer
                .toString(newValue)));
        }
    }

    /**
     * Returns a list of all FieldNames currently in lucene-index
     * that are indexed.
     * 
     * @return Collection all FieldNames currently in lucene-index
     * that are indexed.
     * 
     * @sb
     */
    public Collection<String> getIndexedFieldList() {
        Collection<String> fieldList = new ArrayList<String>();
        IndexReader reader = null;
        try {
            reader = IndexReader.open(getIndexPath());
            fieldList = reader.getFieldNames(FieldOption.INDEXED);
        }
        catch (Exception e) {
            log.error(e);
        }
        finally {
            if (reader != null) {
                try {
                    reader.close();
                }
                catch (IOException e) {
                    log.error("Exception while closing lucene index reader", e);
                }
                reader = null;
            }
        }
        return fieldList;
    }

    /**
     * Returns a list of all FieldNames currently in lucene-index
     * that are stored.
     * 
     * @return Collection all FieldNames currently in lucene-index
     * that are stored
     * 
     * @sb
     */
    public Collection<String> getStoredFieldList() {
        Collection<String> fieldList = new ArrayList<String>();
        IndexReader reader = null;
        try {
            reader = IndexReader.open(getIndexPath());
            //Hack, because its not possible to get all stored fields
            //of an index
            for (int i = 0; i < 10 ; i++) {
            	try {
                	Document doc = reader.document(i);
                	List<Field> fields = doc.getFields();
                	for (Field field : fields) {
                		if (field.isStored() && !fieldList.contains(field.name())) {
                			fieldList.add(field.name());
                		}
                	}
            	} catch (Exception e) {
            		break;
            	}
            }
        }
        catch (Exception e) {
            log.error(e);
        }
        finally {
            if (reader != null) {
                try {
                    reader.close();
                }
                catch (IOException e) {
                    log.error("Exception while closing lucene index reader", e);
                }
                reader = null;
            }
        }
        return fieldList;
    }

    /**
     * Replaces environment-variable placeholders (${java.home}) in the given
     * String with their value.
     * 
     * @param property
     *            inputString
     * @return String Replaced String
     * 
     * @sb
     */
    private String replaceEnvVariables(final String property) {
        String replacedProperty = property;
        if (property.indexOf("${") > -1) {
            String[] envVariables = property.split("\\}.*?\\$\\{");
            if (envVariables != null) {
                for (int i = 0; i < envVariables.length; i++) {
                    envVariables[i] =
                        envVariables[i].replaceFirst(".*?\\$\\{", "");
                    envVariables[i] = envVariables[i].replaceFirst("\\}.*", "");
                    if (System.getProperty(envVariables[i]) != null
                        && !System.getProperty(envVariables[i]).equals("")) {
                        String envVariable =
                            System.getProperty(envVariables[i]);
                        envVariable = envVariable.replaceAll("\\\\", "/");
                        replacedProperty =
                            property.replaceAll("\\$\\{" + envVariables[i]
                                + "}", envVariable);
                    }
                }
            }
        }
        return replacedProperty;
    }

    /**
     * Creates the identifier (search-xml that is returned as response) with
     * highlight-information.
     * 
     * @param doc
     *            Lucene Hit-Document
     * @param idField
     *            Field that holds the primary key of the Object
     * @return String Replaced String
     * @throws Exception
     *             e
     * 
     * @sb
     */
    private String createIdentifier(final Document doc, final Field idField)
        throws Exception {
        String idFieldStr = null;
        if (idField != null) {
            idFieldStr = idField.stringValue();
        }
        if (idFieldStr != null && idFieldStr.trim().length() != 0) {
        	if (idFieldStr.trim().startsWith("&")) {
                idFieldStr = StringEscapeUtils.unescapeXml(idFieldStr);
        	}
            if (highlighter != null) {
                String nsName;
                Pattern pattern = Pattern.compile("(?s)<([^>]*?):");
                Matcher matcher = pattern.matcher(idFieldStr);
                if (matcher.find()) {
                    nsName = matcher.group(1);
                }
                else {
                    nsName = "";
                }
                String highlight = highlighter.getFragments(doc, nsName);
                if (highlight != null && !highlight.equals("")) {
                    idFieldStr =
                        idFieldStr.replaceFirst("(?s)(<\\s*[^\\?]*?>)", "$1"
                            + highlight);
                }
            }
        }
        return idFieldStr;
    }

    /**
     * Recreates CQLTermNode by analyzing all Terms with analyzer This only
     * works if Analyzer uses WhitespaceTokenizer!! this is done because
     * cql.serverChoice gets replaced with defaultIndexField. Afterwards
     * indexFields has to get analyzed. Additionally replaces fieldname
     * cql.serverChoice (this is the case if user gives no field name) with the
     * defaultFieldName from configuration
     * 
     * @param ctn
     *            CQLTermNode
     * @return CQLTermNode Replaced CQLTermNode
     * @throws SRWDiagnostic
     *             e
     * 
     * @sb
     */
    private CQLTermNode getDefaultReplacedCqlTermNode(final CQLTermNode ctn)
        throws SRWDiagnostic {
        CQLTermNode replacedCtn = ctn;
        // eventually replace cql.serverChoice with defaultIndexField///////////
        String qualifier = ctn.getQualifier();
        if (qualifier.matches(".*cql\\.serverChoice.*")
            && getDefaultIndexField() != null) {
            qualifier =
                qualifier.replaceAll("cql\\.serverChoice",
                    getDefaultIndexField());
        }
        String term = ctn.getTerm();
        term = escapeSpecialCharacters(term);
        replacedCtn = new CQLTermNode(qualifier, ctn.getRelation(), term);

        return replacedCtn;
    }

    /**
     * Extracts sortKeys and fills them into a Lucene Sort-Object.
     * 
     * @param sortKeysString
     *            String with sort keys
     * @return Sort Lucene Sort-Object
     * 
     * @sb
     */
    private Sort getLuceneSortObject(final String sortKeysString) {
        Sort sort = null;
        String replacedSortKeysString = sortKeysString;
        if (replacedSortKeysString != null 
            && replacedSortKeysString.length() == 0) {
            replacedSortKeysString = null;
        }

        // extract sortKeys and fill them into a Lucene Sort-Object
        Collection sortFields = new ArrayList();
        if (replacedSortKeysString != null) {
            String[] sortKeys = replacedSortKeysString.split("\\s");
            for (int i = 0; i < sortKeys.length; i++) {
                sortFields.add(sortKeys[i]);
            }
        }
        if (sortFields != null && !sortFields.isEmpty()) {
            int i = 0;
            SortField[] sortFieldArr = new SortField[sortFields.size()];
            for (Iterator iter = sortFields.iterator(); iter.hasNext();) {
                String sortField = (String) iter.next();
                String[] sortPart = sortField.split(",");
                if (sortPart != null && sortPart.length > 0) {
                    if (sortPart.length > 2 && sortPart[2].equals("0")) {
                    	if (comparator == null) {
                            sortFieldArr[i] = new SortField(sortPart[0], true);
                    	} else {
                            sortFieldArr[i] = new SortField(sortPart[0], comparator, true);
                    	}
                    }
                    else {
                    	if (comparator == null) {
                            sortFieldArr[i] = new SortField(sortPart[0]);
                    	} else {
                            sortFieldArr[i] = new SortField(sortPart[0], comparator);
                    	}
                    }
                    i++;
                }
            }
            sort = new Sort(sortFieldArr);
        }
        return sort;
    }

    /**
     * special characters that Lucene requires to escape: + - ! ( ) { } [ ] ^ " ~ * ? : \
     * cql already escaped *,?,",\ and ^
     * so escape the rest.
     * 
     * @param text
     *            Umzuwandelnde Zeichenkette
     * @return Zeichenkette, in der die betroffenen Sonderzeichen markiert sind
     */
    public static String escapeSpecialCharacters(final String text) {
        String replacedText = text;
        replacedText = " " + replacedText;
        replacedText = StringUtils.replace(replacedText, "+", "\\+");
        replacedText = StringUtils.replace(replacedText, "-", "\\-");
        replacedText = StringUtils.replace(replacedText, "!", "\\!");
        replacedText = StringUtils.replace(replacedText, "(", "\\(");
        replacedText = StringUtils.replace(replacedText, ")", "\\)");
        replacedText = StringUtils.replace(replacedText, "{", "\\{");
        replacedText = StringUtils.replace(replacedText, "}", "\\}");
        replacedText = StringUtils.replace(replacedText, "[", "\\[");
        replacedText = StringUtils.replace(replacedText, "]", "\\]");
        replacedText = StringUtils.replace(replacedText, "~", "\\~");
        replacedText = StringUtils.replace(replacedText, ":", "\\:");
        return replacedText.substring(1);
    }

    /**
     * Copied Method from LuceneTranslator.
     * 
     * @param node
     *            CQLNode
     * @return Query query
     * @throws SRWDiagnostic
     *             e
     * 
     * @sb
     */
    public Query makeQuery(final CQLNode node) throws SRWDiagnostic {
        return makeQuery(node, null);
    }

    /**
     * Copied Method from LuceneTranslator and build in analyzing CQLTermNodes.
     * 
     * @param node
     *            CQLNode
     * @param leftQuery
     *            Query
     * @return Query query
     * @throws SRWDiagnostic
     *             e
     * 
     * @sb
     */
    public Query makeQuery(
           final CQLNode node, final Query leftQuery) 
                                  throws SRWDiagnostic {
        Query query = null;

        if (node instanceof CQLBooleanNode) {
            CQLBooleanNode cbn = (CQLBooleanNode) node;

            Query left = makeQuery(cbn.left);
            Query right = makeQuery(cbn.right, left);
            if (node instanceof CQLAndNode) {
                query = new BooleanQuery();
                log.debug("  Anding left and right in new query");
                AndQuery((BooleanQuery) query, left);
                AndQuery((BooleanQuery) query, right);

            }
            else if (node instanceof CQLNotNode) {

                query = new BooleanQuery();
                log.debug("  Notting left and right in new query");
                AndQuery((BooleanQuery) query, left);
                NotQuery((BooleanQuery) query, right);

            }
            else if (node instanceof CQLOrNode) {
                log.debug("  Or'ing left and right in new query");
                query = new BooleanQuery();
                OrQuery((BooleanQuery) query, left);
                OrQuery((BooleanQuery) query, right);
            }
            else {
                throw new RuntimeException("Unknown boolean");
            }

        }
        else if (node instanceof CQLTermNode) {
            CQLTermNode ctn = (CQLTermNode) node;

            // MIH use Analyzer with Term here and recreate CQLTermNode/////////
            // this is done because cql.serverChoice
            // gets replaced with defaultIndexField.
            // Afterwards indexFields has to get analyzed
            ctn = getDefaultReplacedCqlTermNode(ctn);
            // /////////////////////////////////////////////////////////////////
            // MIH get modifiers////////////////////////////////////////////////
            String[] modifiers = ctn.getRelation().getModifiers();
            String modifier = "";
            for (int i = 0; i < modifiers.length; i++) {
                if (modifiers[i].equalsIgnoreCase("fuzzy")) {
                    modifier = "~";
                }
            }
            // /////////////////////////////////////////////////////////////////

            String relation = ctn.getRelation().getBase();
            // MIH scr doesnt work with LuceneTranslator////////////////////////
            if (relation.equalsIgnoreCase("scr")) {
                relation = "=";
            }
            // /////////////////////////////////////////////////////////////////
            String index = ctn.getQualifier();

            if (!index.equals("")) {
                if (relation.equals("=") || relation.equals("scr")) {
                    query =
                        createTermQuery(index, ctn.getTerm() + modifier,
                            relation);
                }
                else if (relation.equals("<")) {
                    Term term = new Term(index, ctn.getTerm() + modifier);
                    // term is upperbound, exclusive
                    query = new RangeQuery(new Term(term.field(),"0"), term, false);
                }
                else if (relation.equals(">")) {
                    Term term = new Term(index, ctn.getTerm() + modifier);
                    // term is lowerbound, exclusive
                    query = new RangeQuery(term, new Term(term.field(),"ZZZZZZZZZZZZZZZ"), false);
                }
                else if (relation.equals("<=")) {
                    Term term = new Term(index, ctn.getTerm() + modifier);
                    // term is upperbound, inclusive
                    query = new RangeQuery(new Term(term.field(),"0"), term, true);
                }
                else if (relation.equals(">=")) {
                    Term term = new Term(index, ctn.getTerm() + modifier);
                    // term is lowebound, inclusive
                    query = new RangeQuery(term, new Term(term.field(),"ZZZZZZZZZZZZZZZ"), true);

                }
                else if (relation.equals("<>")) {
                    /**
                     * <> is an implicit NOT.
                     * 
                     * For example the following statements are identical
                     * results: foo=bar and zoo<>xar foo=bar not zoo=xar
                     */

                    if (leftQuery == null) {
                        // first term in query
                        // create an empty Boolean query to NOT
                        query = new BooleanQuery();
                    }
                    else {
                        if (leftQuery instanceof BooleanQuery) {
                            // left query is already a BooleanQuery use it
                            query = leftQuery;
                        }
                        else {
                            // left query was not a boolean,
                            // create a boolean query
                            // and AND the left query to it
                            query = new BooleanQuery();
                            AndQuery((BooleanQuery) query, leftQuery);
                        }
                    }
                    // create a term query for the term
                    // then NOT it to the boolean query
                    Query termQuery =
                        createTermQuery(index, ctn.getTerm() + modifier,
                            relation);
                    NotQuery((BooleanQuery) query, termQuery);

                }
                else if (relation.equals("any")) {
                    // implicit or
                    query =
                        createTermQuery(index, ctn.getTerm() + modifier,
                            relation);

                }
                else if (relation.equals("all")) {
                    // implicit and
                    query =
                        createTermQuery(index, ctn.getTerm() + modifier,
                            relation);
                }
                else if (relation.equals("exact")) {
                    /**
                     * implicit and. this query will only return accurate
                     * results for indexes that have been indexed using a
                     * non-tokenizing analyzer
                     */
                    query =
                        createTermQuery(index, ctn.getTerm() + modifier,
                            relation);
                }
                else {
                    // anything else is unsupported
                    throw new SRWDiagnostic(
                        DIAGNOSTIC_CODE_NINETEEN, ctn.getRelation().getBase());
                }

            }
        }
        else {
            throw new SRWDiagnostic(
                DIAGNOSTIC_CODE_FOURTYSEVEN, "UnknownCQLNode: " + node + ")");
        }
        if (query != null) {
            log.info("Query : " + query.toString());
        }
        return query;
    }

}

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

package de.escidoc.sb.srw;

import gov.loc.www.zing.srw.ExtraDataType;
import gov.loc.www.zing.srw.SearchRetrieveRequestType;
import gov.loc.www.zing.srw.TermType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.axis.types.NonNegativeInteger;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RangeQuery;
import org.osuosl.srw.SRWDiagnostic;
import org.osuosl.srw.lucene.LuceneTranslator;
import org.z3950.zing.cql.CQLAndNode;
import org.z3950.zing.cql.CQLBooleanNode;
import org.z3950.zing.cql.CQLNode;
import org.z3950.zing.cql.CQLNotNode;
import org.z3950.zing.cql.CQLOrNode;
import org.z3950.zing.cql.CQLTermNode;

import ORG.oclc.os.SRW.QueryResult;

public abstract class EscidocTranslator extends LuceneTranslator {

	protected static Log log = LogFactory.getLog(EscidocTranslator.class);

	public static final String PROPERTY_DEFAULT_INDEX_FIELD = "cqlTranslator.defaultIndexField";

	public static final int DIAGNOSTIC_CODE_NINETEEN = 19;

	public static final int DIAGNOSTIC_CODE_TWENTY = 19;

	public static final int DIAGNOSTIC_CODE_FOURTYSEVEN = 47;
	
    public static final Pattern LUCENE_SPECIAL_CHAR_PATTERN = 
        Pattern.compile("([^\\\\])([\\+\\-\\&\\|!\\(\\)\\{\\}\\[\\]~:])"
                + "|([^\\\\]\\\\\\\\)([\\+\\-\\&\\|!\\(\\)\\{\\}\\[\\]~:])");
	
	private static Matcher luceneSpecialCharMatcher = 
	                LUCENE_SPECIAL_CHAR_PATTERN.matcher("");

	/**
	 * Default Index Field. Is static because it is used in overwritten static
	 * method
	 */
	protected String defaultIndexField;

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
	@Override
    public QueryResult search(final CQLNode queryRoot,
			final ExtraDataType extraDataType) throws SRWDiagnostic {
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
	public abstract QueryResult search(
            final CQLNode queryRoot, final ExtraDataType extraDataType,
            final SearchRetrieveRequestType request) throws SRWDiagnostic;

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
	@Override
    public abstract TermType[] scan(final CQLNode queryRoot,
			final ExtraDataType extraDataType) throws Exception;
	
    /**
     * Returns a list of all FieldNames currently in lucene-index
     * that are indexed.
     * 
     * @return Collection all FieldNames currently in lucene-index
     * that are indexed.
     * 
     * @sb
     */
    public abstract Collection<String> getIndexedFieldList();

    /**
     * Returns a list of all FieldNames currently in lucene-index
     * that are stored.
     * 
     * @return Collection all FieldNames currently in lucene-index
     * that are stored
     * 
     * @sb
     */
    public abstract Collection<String> getStoredFieldList();
	/**
	 * Counts the term. If the term matches an existing term it is added on to
	 * the count for that term. If
	 * 
	 * @param termMap -
	 *            map of terms already counted
	 * @param value -
	 *            value of the term
	 */
	protected void countTerm(final Map termMap, final String value) {
		TermType termType = (TermType) termMap.get(value);
		if (termType == null) {
			// not found, create
			termType = new TermType();
			termType.setValue(value);
			termType.setNumberOfRecords(new NonNegativeInteger(Integer
					.toString(1)));
			termMap.put(value, termType);
		} else {
			NonNegativeInteger count = termType.getNumberOfRecords();
			int newValue = count.intValue() + 1;
			termType.setNumberOfRecords(new NonNegativeInteger(Integer
					.toString(newValue)));
		}
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
	protected String replaceEnvVariables(final String property) {
		String replacedProperty = property;
		if (property.indexOf("${") > -1) {
			String[] envVariables = property.split("\\}.*?\\$\\{");
			if (envVariables != null) {
				for (int i = 0; i < envVariables.length; i++) {
					envVariables[i] = envVariables[i].replaceFirst(".*?\\$\\{",
							"");
					envVariables[i] = envVariables[i].replaceFirst("\\}.*", "");
					if (System.getProperty(envVariables[i]) != null
							&& !System.getProperty(envVariables[i]).equals("")) {
						String envVariable = System
								.getProperty(envVariables[i]);
						envVariable = envVariable.replaceAll("\\\\", "/");
						replacedProperty = property.replaceAll("\\$\\{"
								+ envVariables[i] + "}", envVariable);
					}
				}
			}
		}
		return replacedProperty;
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
	protected CQLTermNode getDefaultReplacedCqlTermNode(final CQLTermNode ctn)
			throws SRWDiagnostic {
		CQLTermNode replacedCtn = ctn;
		// eventually replace cql.serverChoice with defaultIndexField///////////
		String qualifier = ctn.getQualifier();
		if (qualifier.matches(".*cql\\.serverChoice.*")
				&& getDefaultIndexField() != null) {
			qualifier = qualifier.replaceAll("cql\\.serverChoice",
					getDefaultIndexField());
		}
		String term = ctn.getTerm();
		term = escapeSpecialCharacters(term);
		replacedCtn = new CQLTermNode(qualifier, ctn.getRelation(), term);

		return replacedCtn;
	}

	/**
	 * special characters that Lucene requires to escape: 
	 * + - & | ! ( ) { } [ ] ^ " ~ * ? : \
	 * cql already escaped *,?,",\ and ^ so escape the rest.
	 * 
	 * @param text
	 *            Umzuwandelnde Zeichenkette
	 * @return Zeichenkette, in der die betroffenen Sonderzeichen markiert sind
	 */
	public static String escapeSpecialCharacters(final String text) {
		String replacedText = text;
		replacedText = " " + replacedText;
        luceneSpecialCharMatcher.reset(replacedText);
        replacedText = luceneSpecialCharMatcher.replaceAll("$1$3\\\\$2$4");
		//workaround because cql-parser cant handle \"
		//see EscidocSRWDatabaseImpl.doRequest
        replacedText = StringUtils.replace(replacedText, "#quote#", "\\\"");
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
	@Override
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
	@Override
    public Query makeQuery(final CQLNode node, final Query leftQuery)
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

			} else if (node instanceof CQLNotNode) {

				query = new BooleanQuery();
				log.debug("  Notting left and right in new query");
				AndQuery((BooleanQuery) query, left);
				NotQuery((BooleanQuery) query, right);

			} else if (node instanceof CQLOrNode) {
				log.debug("  Or'ing left and right in new query");
				query = new BooleanQuery();
				OrQuery((BooleanQuery) query, left);
				OrQuery((BooleanQuery) query, right);
			} else {
				throw new RuntimeException("Unknown boolean");
			}

		} else if (node instanceof CQLTermNode) {
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
					query = createTermQuery(index, ctn.getTerm() + modifier,
							relation);
				} else if (relation.equals("<")) {
					Term term = new Term(index, ctn.getTerm() + modifier);
					if (term.text() == null || term.text().equals("")) {
					    term = term.createTerm("0");
					}
					// term is upperbound, exclusive
					query = new RangeQuery(new Term(term.field(), "0"), term,
							false);
				} else if (relation.equals(">")) {
					Term term = new Term(index, ctn.getTerm() + modifier);
                    if (term.text() == null || term.text().equals("")) {
                        term = term.createTerm("0");
                    }
					// term is lowerbound, exclusive
					query = new RangeQuery(term, new Term(term.field(),
							"ZZZZZZZZZZZZZZZ"), false);
				} else if (relation.equals("<=")) {
					Term term = new Term(index, ctn.getTerm() + modifier);
                    if (term.text() == null || term.text().equals("")) {
                        term = term.createTerm("0");
                    }
					// term is upperbound, inclusive
					query = new RangeQuery(new Term(term.field(), "0"), term,
							true);
				} else if (relation.equals(">=")) {
					Term term = new Term(index, ctn.getTerm() + modifier);
                    if (term.text() == null || term.text().equals("")) {
                        term = term.createTerm("0");
                    }
					// term is lowebound, inclusive
					query = new RangeQuery(term, new Term(term.field(),
							"ZZZZZZZZZZZZZZZ"), true);

				} else if (relation.equals("<>")) {
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
					} else {
						if (leftQuery instanceof BooleanQuery) {
							// left query is already a BooleanQuery use it
							query = leftQuery;
						} else {
							// left query was not a boolean,
							// create a boolean query
							// and AND the left query to it
							query = new BooleanQuery();
							AndQuery((BooleanQuery) query, leftQuery);
						}
					}
					// create a term query for the term
					// then NOT it to the boolean query
					Query termQuery = createTermQuery(index, ctn.getTerm()
							+ modifier, relation);
					NotQuery((BooleanQuery) query, termQuery);

				} else if (relation.equals("any")) {
					// implicit or
					query = createTermQuery(index, ctn.getTerm() + modifier,
							relation);

				} else if (relation.equals("all")) {
					// implicit and
					query = createTermQuery(index, ctn.getTerm() + modifier,
							relation);
				} else if (relation.equals("exact")) {
					/**
					 * implicit and. this query will only return accurate
					 * results for indexes that have been indexed using a
					 * non-tokenizing analyzer
					 */
					query = createTermQuery(index, ctn.getTerm() + modifier,
							relation);
				} else {
					// anything else is unsupported
					throw new SRWDiagnostic(DIAGNOSTIC_CODE_NINETEEN, ctn
							.getRelation().getBase());
				}

			}
		} else {
			throw new SRWDiagnostic(DIAGNOSTIC_CODE_FOURTYSEVEN,
					"UnknownCQLNode: " + node + ")");
		}
		if (query != null) {
			log.info("Query : " + query.toString());
		}
		return query;
	}

    /**
     * Get Collection of query-terms.
     * 
     * @param node
     *            CQLNode
     * @return Query query
     * @throws SRWDiagnostic
     *             e
     * 
     * @sb
     */
    public HashMap getQueryTerms(final CQLNode node) throws SRWDiagnostic {
        return getQueryTerms(node, null);
    }

    /**
     * Get Collection of query-terms.
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
    public HashMap getQueryTerms(final CQLNode node, final HashMap leftQuery)
            throws SRWDiagnostic {
        Query query = null;
        HashMap returnMap = leftQuery;
        if (returnMap == null) {
            returnMap = new HashMap();
        }

        if (node instanceof CQLBooleanNode) {
            CQLBooleanNode cbn = (CQLBooleanNode) node;

            HashMap left = getQueryTerms(cbn.left);
            returnMap.putAll(left);
            HashMap right = getQueryTerms(cbn.right, left);
            returnMap.putAll(right);
            if (node instanceof CQLAndNode) {
                query = new BooleanQuery();
                log.debug("  Anding left and right in new query");
                AndQuery((BooleanQuery) query, (Query)left.get("query"));
                AndQuery((BooleanQuery) query, (Query)right.get("query"));

            } else if (node instanceof CQLNotNode) {

                query = new BooleanQuery();
                log.debug("  Notting left and right in new query");
                AndQuery((BooleanQuery) query, (Query)left.get("query"));
                NotQuery((BooleanQuery) query, (Query)right.get("query"));

            } else if (node instanceof CQLOrNode) {
                log.debug("  Or'ing left and right in new query");
                query = new BooleanQuery();
                OrQuery((BooleanQuery) query, (Query)left.get("query"));
                OrQuery((BooleanQuery) query, (Query)right.get("query"));
            } else {
                throw new RuntimeException("Unknown boolean");
            }

        } else if (node instanceof CQLTermNode) {
            CQLTermNode ctn = (CQLTermNode) node;

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
            if (returnMap.get(index) == null) {
                returnMap.put(index, new ArrayList<String>());
            }
            ((ArrayList<String>)returnMap.get(index)).add(ctn.getTerm());

            if (!index.equals("")) {
                if (relation.equals("=") || relation.equals("scr")) {
                    query = createTermQuery(index, ctn.getTerm() + modifier,
                            relation);
                } else if (relation.equals("<")) {
                    Term term = new Term(index, ctn.getTerm() + modifier);
                    if (term.text() == null || term.text().equals("")) {
                        term = term.createTerm("0");
                    }
                    // term is upperbound, exclusive
                    query = new RangeQuery(new Term(term.field(), "0"), term,
                            false);
                } else if (relation.equals(">")) {
                    Term term = new Term(index, ctn.getTerm() + modifier);
                    if (term.text() == null || term.text().equals("")) {
                        term = term.createTerm("0");
                    }
                    // term is lowerbound, exclusive
                    query = new RangeQuery(term, new Term(term.field(),
                            "ZZZZZZZZZZZZZZZ"), false);
                } else if (relation.equals("<=")) {
                    Term term = new Term(index, ctn.getTerm() + modifier);
                    if (term.text() == null || term.text().equals("")) {
                        term = term.createTerm("0");
                    }
                    // term is upperbound, inclusive
                    query = new RangeQuery(new Term(term.field(), "0"), term,
                            true);
                } else if (relation.equals(">=")) {
                    Term term = new Term(index, ctn.getTerm() + modifier);
                    if (term.text() == null || term.text().equals("")) {
                        term = term.createTerm("0");
                    }
                    // term is lowebound, inclusive
                    query = new RangeQuery(term, new Term(term.field(),
                            "ZZZZZZZZZZZZZZZ"), true);

                } else if (relation.equals("<>")) {
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
                    } else {
                        if ((Query)leftQuery.get("query") instanceof BooleanQuery) {
                            // left query is already a BooleanQuery use it
                            query = (Query)leftQuery.get("query");
                        } else {
                            // left query was not a boolean,
                            // create a boolean query
                            // and AND the left query to it
                            query = new BooleanQuery();
                            AndQuery((BooleanQuery) query, (Query)leftQuery.get("query"));
                        }
                    }
                    // create a term query for the term
                    // then NOT it to the boolean query
                    Query termQuery = createTermQuery(index, ctn.getTerm()
                            + modifier, relation);
                    NotQuery((BooleanQuery) query, termQuery);

                } else if (relation.equals("any")) {
                    // implicit or
                    query = createTermQuery(index, ctn.getTerm() + modifier,
                            relation);

                } else if (relation.equals("all")) {
                    // implicit and
                    query = createTermQuery(index, ctn.getTerm() + modifier,
                            relation);
                } else if (relation.equals("exact")) {
                    /**
                     * implicit and. this query will only return accurate
                     * results for indexes that have been indexed using a
                     * non-tokenizing analyzer
                     */
                    query = createTermQuery(index, ctn.getTerm() + modifier,
                            relation);
                } else {
                    // anything else is unsupported
                    throw new SRWDiagnostic(DIAGNOSTIC_CODE_NINETEEN, ctn
                            .getRelation().getBase());
                }

            }
        } else {
            throw new SRWDiagnostic(DIAGNOSTIC_CODE_FOURTYSEVEN,
                    "UnknownCQLNode: " + node + ")");
        }
        if (query != null) {
            log.info("Query : " + query.toString());
        }
        returnMap.put("query", query);
        return returnMap;
    }

}

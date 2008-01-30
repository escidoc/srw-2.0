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
     * @return String highlight-string.
     * 
     * @sb
     */
    String getFragments(final Document doc, final String namespacePrefix)
        throws Exception;

}

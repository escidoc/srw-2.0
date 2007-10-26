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

package de.fiz.escidoc.sb.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;

/**
 * Constants for Search and Browse.
 * 
 * @author MIH
 */
public class Constants {

    /**
     * Constants for analyzing.
     */
    public static final HashMap<String, String> ANALYZABLE_FIELDS = new HashMap<String, String>() {
        {
            put("fulltext", "");
            put("metadata", "");
            put("title", "");
            put("description", "");
            put("alternative", "");
            put("abstract", "");
            put("subject", "");
            put("xml_metadata", "");
        }
    };

    public static final Collection ANALYZABLE_MATCH_FIELDS = new ArrayList<String>() {
        {
            add("stored_fulltext");
        }

    };

    public static final HashMap GERMAN_ANALYZING_PARAMETERS = new HashMap() {
        {
            put("snowballType", "German");
            put("stopwords", GermanAnalyzer.GERMAN_STOP_WORDS);
        }
    };

    public static final HashMap ENGLISH_ANALYZING_PARAMETERS = new HashMap() {
        {
            put("snowballType", "English");
            put("stopwords", StopAnalyzer.ENGLISH_STOP_WORDS);
        }
    };

    public static final HashMap<String, HashMap> SUPPORTED_ANALYZING_LANGUAGES = new HashMap<String, HashMap>() {
        {
            put("de", GERMAN_ANALYZING_PARAMETERS);
            put("en", ENGLISH_ANALYZING_PARAMETERS);
        }
    };

    public static final String ORG_UNIT_URL = "/oum/organizational-unit/";

    public static final String ORG_UNIT_PATH_LIST_URL_SUFFIX = "/resources/path-list";

    public static final String SEARCH_PASSWORD = "password";

}

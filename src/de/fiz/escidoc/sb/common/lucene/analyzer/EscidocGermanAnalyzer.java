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

package de.fiz.escidoc.sb.common.lucene.analyzer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Analyzer for german escidoc lucene index.
 * 
 * @author MIH
 * @sb
 */
public class EscidocGermanAnalyzer extends EscidocAnalyzer {
    private static Log log = LogFactory.getLog(EscidocGermanAnalyzer.class);

    /**
     * initializes the analyzer.
     * 
     * @sb
     */
    public EscidocGermanAnalyzer() {
        if (log.isDebugEnabled()) {
            log.debug("Initializing EscidocGermanAnalyzer");
        }
        super.setLanguage("de");
    }
}

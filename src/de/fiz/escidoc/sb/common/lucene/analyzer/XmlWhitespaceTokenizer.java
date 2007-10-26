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

import java.io.Reader;

import org.apache.lucene.analysis.CharTokenizer;

/**
 * @author mih
 * 
 * Class tokenizes Strings at Whitespace and at <,>,&.
 * 
 * @sb
 */
public class XmlWhitespaceTokenizer extends CharTokenizer {
    /**
     * Constructor with Reader.
     * 
     * @param in
     *            Reader
     * 
     * @sb
     */
    public XmlWhitespaceTokenizer(final Reader in) {
        super(in);
    }

    /**
     * Return false if character is Whitespace or <,>,&.
     * 
     * @param c
     *            character
     * @return true if whitespace-character, else false.
     * 
     * @sb
     */
    protected boolean isTokenChar(final char c) {
        if (Character.isWhitespace(c) || c == '\u003c' || c == '\u003e'
            || c == '\u0026') {
            return false;
        }
        return true;
    }
}

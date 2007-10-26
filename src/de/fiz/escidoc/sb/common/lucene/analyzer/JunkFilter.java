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

import java.io.IOException;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;

/**
 * @author rha
 * 
 * JunkFilter filters out (parts of) strings that contain the specified
 * substrings.
 * 
 */
public class JunkFilter extends TokenFilter {
    private String s = "";
    
    private static final int THREE = 3;

    private static final int FOUR = 4;

    private static final int FIVE = 5;

    /**
     * Constructor.
     * 
     * @param in
     *            TokenStream
     * 
     */
    public JunkFilter(final TokenStream in) {
        super(in);
    }

    /**
     * Returns next Token without junk-signs.
     * 
     * @return Token token
     * @throws IOException
     *             e
     * 
     */
    public final Token next() throws IOException {
        Token t = input.next();
        if (t == null) {
            return null;
        }
        else {
            s = t.termText();
            s = substituteJunk(s);

            // If not stemmed, don't waste the time creating a new token
            if (!s.equals(t.termText())) {
                return new Token(s, t.startOffset(), t.endOffset(), t.type());
            }
            return t;
        }
    }

    /**
     * Returns substituted String.
     * 
     * @param string
     *            String string
     * @return String substituted String
     * 
     */
    private String substituteJunk(final String string) {
        String replacedString  = string;
        if (replacedString != null
            && (replacedString.equals("lt") || replacedString.equals("gt;")
                || replacedString.equals("amp;") 
                || replacedString.equals("quot;") || replacedString
                .equals("apos;"))) {
            replacedString = " ";
        }
        if (replacedString.startsWith("[nl]")) {
            if (replacedString.length() > FOUR) {
                replacedString = replacedString.substring(FOUR);
            }
            else {
                replacedString = " ";
            }
        }
        if (replacedString.endsWith("[nl]")) {
            if (replacedString.length() > FOUR) {
                replacedString = 
                    replacedString.substring(0, replacedString.length() - FOUR);
            }
            else {
                replacedString = " ";
            }
        }
        if (replacedString.startsWith("\"") || replacedString.startsWith(",")
            || replacedString.startsWith(";") || replacedString.startsWith("'")
            || replacedString.startsWith("<") || replacedString.startsWith(">")
            || replacedString.startsWith("=") || replacedString.startsWith("/")
            || replacedString.startsWith(".") || replacedString.startsWith("(")) {
            if (replacedString.length() > 1) {
                replacedString = replacedString.substring(1);
            }
            else {
                replacedString = " ";
            }
        }
        if (replacedString.endsWith("\"") || replacedString.endsWith(",")
            || replacedString.endsWith(";") || replacedString.endsWith("'")
            || replacedString.endsWith("<") || replacedString.endsWith(">")
            || replacedString.endsWith("=") || replacedString.endsWith("/")
            || replacedString.endsWith(".") || replacedString.endsWith(":")
            || replacedString.endsWith(")") || replacedString.endsWith("?")
            || replacedString.endsWith("!")) {
            if (replacedString.length() > 1) {
                replacedString = 
                    replacedString.substring(0, replacedString.length() - 1);
            }
            else {
                replacedString = " ";
            }
        }
        if (replacedString.startsWith("quot;") 
            || replacedString.startsWith("apos;")) {
            if (replacedString.length() > FIVE) {
                replacedString = replacedString.substring(FIVE);
            }
            else {
                replacedString = " ";
            }
        }
        if (replacedString.endsWith("quot;") || replacedString.endsWith("apos;")) {
            if (replacedString.length() > FIVE) {
                replacedString = 
                    replacedString.substring(0, replacedString.length() - FIVE);
            }
            else {
                replacedString = " ";
            }
        }
        if (replacedString.startsWith("amp;")) {
            if (replacedString.length() > FOUR) {
                replacedString = replacedString.substring(FOUR);
            }
            else {
                replacedString = " ";
            }
        }
        if (replacedString.endsWith("amp;")) {
            if (replacedString.length() > FOUR) {
                replacedString = 
                    replacedString.substring(0, replacedString.length() - FOUR);
            }
            else {
                replacedString = " ";
            }
        }
        if (replacedString.startsWith("gt;") || replacedString.startsWith("lt;")) {
            if (replacedString.length() > THREE) {
                replacedString = replacedString.substring(THREE);
            }
            else {
                replacedString = " ";
            }
        }
        if (replacedString.endsWith("gt;") || replacedString.endsWith("lt;")) {
            if (replacedString.length() > THREE) {
                replacedString = 
                    replacedString.substring(0, replacedString.length() - THREE);
            }
            else {
                replacedString = " ";
            }
        }
        return replacedString;
    }
}

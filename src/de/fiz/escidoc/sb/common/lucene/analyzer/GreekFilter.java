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
 * This class converts greek letters to their latin transcriptions.
 * 
 * --- Unicode Greek --- Covers all glyphs from 0391 to 03C9 except 03AA to 03B0
 * and 03A2
 * 
 */

public class GreekFilter extends TokenFilter {
    private StringBuffer sb = new StringBuffer();

    private String s = "";

    /**
     * Constructor.
     * 
     * @param in
     *            TokenStream
     * 
     */
    public GreekFilter(final TokenStream in) {
        super(in);
    }

    /**
     * Returns next Token without greek letters.
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

            sb.delete(0, sb.length());
            sb.insert(0, s);
            s = substitute(sb);

            // If not stemmed, dont waste the time creating a new token
            if (!s.equals(t.termText())) {
                return new Token(s, t.startOffset(), t.endOffset(), t.type());
            }
            return t;
        }
    }

    /**
     * Returns substituted String.
     * 
     * @param buffer
     *            StringBuffer buffer
     * @return String substituted String
     * 
     */
    private String substitute(final StringBuffer buffer) {
        StringBuffer substituteString = buffer;
        for (int c = 0; c < substituteString.length(); c++) {
            if (substituteString.charAt(c) == '\u03B1'
                || substituteString.charAt(c) == '\u0391') {
                substituteString.setCharAt(c, 'a'); // alpha
            }
            else if (substituteString.charAt(c) == '\u03B2'
                || substituteString.charAt(c) == '\u0392') {
                substituteString.setCharAt(c, 'b'); // beta
            }

            else if (substituteString.charAt(c) == '\u03B3'
                || substituteString.charAt(c) == '\u0393') {
                substituteString.setCharAt(c, 'g'); // gamma
            }

            else if (substituteString.charAt(c) == '\u03B4'
                || substituteString.charAt(c) == '\u0394') {
                substituteString.setCharAt(c, 'd'); // delta
            }

            else if (substituteString.charAt(c) == '\u03B5'
                || substituteString.charAt(c) == '\u0395') {
                substituteString.setCharAt(c, 'e'); // epsilon
            }

            else if (substituteString.charAt(c) == '\u03B6'
                || substituteString.charAt(c) == '\u0396') {
                substituteString.setCharAt(c, 'z'); // zeta
            }

            else if (substituteString.charAt(c) == '\u03B7'
                || substituteString.charAt(c) == '\u0397') {
                substituteString.setCharAt(c, 'e'); // eta
            }

            else if (substituteString.charAt(c) == '\u03B8'
                || substituteString.charAt(c) == '\u0398') {
                substituteString.setCharAt(c, 't'); // theta
                substituteString.insert(c + 1, 'h');
            }

            else if (substituteString.charAt(c) == '\u03B9'
                || substituteString.charAt(c) == '\u0399') {
                substituteString.setCharAt(c, 'j'); // iota
            }

            else if (substituteString.charAt(c) == '\u03BA'
                || substituteString.charAt(c) == '\u039A') {
                substituteString.setCharAt(c, 'k'); // kappa
            }

            else if (substituteString.charAt(c) == '\u03BB'
                || substituteString.charAt(c) == '\u039B') {
                substituteString.setCharAt(c, 'l'); // lambda
            }

            else if (substituteString.charAt(c) == '\u03BC'
                || substituteString.charAt(c) == '\u039C') {
                substituteString.setCharAt(c, 'm'); // my
            }

            else if (substituteString.charAt(c) == '\u03BD'
                || substituteString.charAt(c) == '\u039D') {
                substituteString.setCharAt(c, 'n'); // ny
            }

            else if (substituteString.charAt(c) == '\u03BE'
                || substituteString.charAt(c) == '\u039E') {
                substituteString.setCharAt(c, 'k'); // xi
                substituteString.insert(c + 1, 's');
            }

            else if (substituteString.charAt(c) == '\u03BF'
                || substituteString.charAt(c) == '\u039F') {
                substituteString.setCharAt(c, 'o'); // omikron
            }

            else if (substituteString.charAt(c) == '\u03A0'
                || substituteString.charAt(c) == '\u03C0') {
                substituteString.setCharAt(c, 'p'); // pi
            }

            else if (substituteString.charAt(c) == '\u03A1'
                || substituteString.charAt(c) == '\u03C1') {
                substituteString.setCharAt(c, 'r'); // rho
            }

            else if (substituteString.charAt(c) == '\u03A3'
                || substituteString.charAt(c) == '\u03C2'
                || substituteString.charAt(c) == '\u03C3') {
                substituteString.setCharAt(c, 's'); // sigma
            }

            else if (substituteString.charAt(c) == '\u03A4'
                || substituteString.charAt(c) == '\u03C4') {
                substituteString.setCharAt(c, 't'); // tau
            }

            else if (substituteString.charAt(c) == '\u03A5'
                || substituteString.charAt(c) == '\u03C5') {
                substituteString.setCharAt(c, 'y'); // ypsilon
            }

            else if (substituteString.charAt(c) == '\u03A6'
                || substituteString.charAt(c) == '\u03C6') {
                substituteString.setCharAt(c, 'f'); // phi
            }

            else if (substituteString.charAt(c) == '\u03A7'
                || substituteString.charAt(c) == '\u03C7') {
                substituteString.setCharAt(c, 'x'); // chi
            }

            else if (substituteString.charAt(c) == '\u03A8'
                || substituteString.charAt(c) == '\u03C8') {
                substituteString.setCharAt(c, 'p'); // psi
                substituteString.insert(c + 1, 's');
            }

            else if (substituteString.charAt(c) == '\u03A9'
                || substituteString.charAt(c) == '\u03C9') {
                substituteString.setCharAt(c, 'o'); // omega
            }
        }
        return substituteString.toString();
    }
}

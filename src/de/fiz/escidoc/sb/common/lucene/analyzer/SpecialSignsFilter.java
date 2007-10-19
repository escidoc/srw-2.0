package de.fiz.escidoc.sb.common.lucene.analyzer;

import java.io.IOException;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;

/**
 * This class converts special letters to the specified equivalents.
 * 
 * 
 * Covers glyphs: 00B1 plus-minus --> 002B and 002D +- plus minus 00B7 middle
 * dot --> 005F _ underscore 2032 prime --> 0027 ' apostrophe 2033 double prime
 * --> 0027 and 0027 '' double apostrophe 2192 rightwards arrow --> - and - and >
 * 2260 not equal to --> !=
 * 
 */

public class SpecialSignsFilter extends TokenFilter {
    private StringBuffer sb = new StringBuffer();

    private String s = "";

    /**
     * Constructor.
     * 
     * @param in
     *            TokenStream
     * 
     */
    public SpecialSignsFilter(final TokenStream in) {
        super(in);
    }

    /**
     * Returns next Token without special letters.
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
            if (substituteString.charAt(c) == '\u00B1') {
                substituteString.setCharAt(c, '+');
                substituteString.insert(c + 1, '-');
            }

            else if (substituteString.charAt(c) == '\u00B7') {
                substituteString.setCharAt(c, '_');
            }

            else if (substituteString.charAt(c) == '\u2032') {
                substituteString.setCharAt(c, '\'');
            }

            else if (substituteString.charAt(c) == '\u2033') {
                substituteString.setCharAt(c, '\'');
                substituteString.insert(c + 1, '\'');
            }

            else if (substituteString.charAt(c) == '\u2192') {
                substituteString.setCharAt(c, '-');
                substituteString.insert(c + 1, '-');
                substituteString.insert(c + 2, '>');
            }

            else if (substituteString.charAt(c) == '\u2260') {
                substituteString.setCharAt(c, '!');
                substituteString.insert(c + 1, '=');
            }

        }
        return substituteString.toString();
    }
}

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

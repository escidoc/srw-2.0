package de.fiz.escidoc.sb.srw.lucene.highlighting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Class makes xml out of highlight-data.
 * 
 * @author MIH
 * @sb
 */
public class HighlightXmlizer {
    private Collection highlightFragmentDatas = new ArrayList();

    private String fragmentSeparator;

    private String highlightStartMarker;

    private String highlightEndMarker;

    /**
     * Initialize Class with separator-informations.
     * 
     * @param fragmentSeparator
     *            separator between different text-fragments.
     * @param highlightStartMarker
     *            indicator for highlight-term start.
     * @param highlightEndMarker
     *            indicator for highlight-term end.
     * @throws Exception
     *             e
     * @sb
     */
    public HighlightXmlizer(final String fragmentSeparator,
        final String highlightStartMarker, final String highlightEndMarker)
        throws Exception {
        if (fragmentSeparator == null || highlightStartMarker == null
            || highlightEndMarker == null) {
            throw new Exception("constructor parameter may not be null");
        }
        this.fragmentSeparator = fragmentSeparator;
        this.highlightStartMarker = highlightStartMarker;
        this.highlightEndMarker = highlightEndMarker;

    }

    /**
     * Make xml out of highlightFragmentDatas Array. 1. Write head of xml 2.
     * Iterate over highlightFragmentDatas 3. Get one highlightFragmentData
     * HashMap HahsMap contains the following elements: -highlightLocator (path
     * to fulltext where highlight-snippet comes from) -type (fulltext or
     * metadata , only fulltext gets path to content) -highlightSnippet
     * (text-snippet with highlight start + end information) 4. Write
     * head-information for the highlight-snippet 5. Write text-information for
     * the highlight-snippet by calling method xmlizeTextFragment
     * 
     * @param namespacePrefix
     *            namespacePrefix to use for xml.
     * @return String xml
     * @throws Exception
     *             e
     * @sb
     */
    public String xmlize(final String namespacePrefix) throws Exception {
        StringBuffer xml = new StringBuffer("");
        if (highlightFragmentDatas != null && !highlightFragmentDatas.isEmpty()) {
            xml.append("<").append(getNamespacePrefix(namespacePrefix)).append(
                "highlight>");
            for (Iterator iter = highlightFragmentDatas.iterator(); iter
                .hasNext();) {
                HashMap highlightFragmentData = (HashMap) iter.next();
                String type = (String) highlightFragmentData.get("type");
                String objid =
                    (String) highlightFragmentData.get("highlightLocator");
                if (objid != null) {
                    objid = objid.replaceAll("\\/content", "");
                    objid = objid.replaceAll(".*\\/", "");
                }
                String textFragmentData =
                    (String) highlightFragmentData.get("highlightSnippet");
                if (textFragmentData != null && !textFragmentData.equals("")) {
                    if (type == null || type.equals("")) {
                        throw new Exception("type may not be null");
                    }
                    textFragmentData =
                        replaceSpecialCharacters(textFragmentData);
                    xml
                        .append("<")
                        .append(getNamespacePrefix(namespacePrefix)).append(
                            "search-hit type=\"").append(type).append("\"");
                    if (type.equals("fulltext")) {
                        if (objid == null || objid.equals("")) {
                            throw new Exception(
                                "highlightLocator may not be null");
                        }
                        xml.append(" objid=\"").append(objid).append("\"");
                    }
                    xml.append(">");
                    String[] textFragments =
                        textFragmentData.split(fragmentSeparator);
                    if (textFragments != null) {
                        for (int i = 0; i < textFragments.length; i++) {
                            xml.append(xmlizeTextFragment(textFragments[i],
                                namespacePrefix));
                        }
                    }
                    xml
                        .append("</").append(
                            getNamespacePrefix(namespacePrefix)).append(
                            "search-hit>");
                }

            }
            xml
                .append("</").append(getNamespacePrefix(namespacePrefix))
                .append("highlight>");
        }
        return xml.toString();
    }

    /**
     * Make xml out of one text-fragment. Replace highlight-start + end-marker,
     * calculate start + end-index of the hit-words with help of the
     * highlight-start + end-marker.
     * 
     * @param textFragment
     *            textFragment.
     * @param namespacePrefix
     *            namespacePrefix to use for xml.
     * @return String xml
     * @sb
     */
    private String xmlizeTextFragment(
        final String textFragment, final String namespacePrefix) {
        StringBuffer xml = new StringBuffer("");
        StringBuffer replacedFragment = new StringBuffer("");
        if (textFragment != null) {
            String[] highlightFragments =
                textFragment.split(highlightStartMarker);
            if (highlightFragments != null && highlightFragments.length > 1) {
                xml
                    .append("<").append(getNamespacePrefix(namespacePrefix))
                    .append("text-fragment>");
                int offset = highlightFragments[0].length();
                replacedFragment.append(highlightFragments[0]);
                StringBuffer indexFields = new StringBuffer("");
                for (int i = 1; i < highlightFragments.length; i++) {
                    String highlightFragment = highlightFragments[i];
                    int highlightEndMarkerIndex =
                        highlightFragment.indexOf(highlightEndMarker);
                    int end = offset + highlightEndMarkerIndex - 1;
                    highlightFragment =
                        highlightFragment.replaceAll(highlightEndMarker, "");
                    replacedFragment.append(highlightFragment);
                    indexFields
                        .append("<")
                        .append(getNamespacePrefix(namespacePrefix)).append(
                            "hit-word>");
                    indexFields.append("<").append(
                        getNamespacePrefix(namespacePrefix)).append(
                        "start-index>").append(offset);
                    indexFields.append("</").append(
                        getNamespacePrefix(namespacePrefix)).append(
                        "start-index>");
                    indexFields.append("<").append(
                        getNamespacePrefix(namespacePrefix)).append(
                        "end-index>").append(end);
                    indexFields.append("</").append(
                        getNamespacePrefix(namespacePrefix)).append(
                        "end-index>");
                    indexFields
                        .append("</").append(
                            getNamespacePrefix(namespacePrefix)).append(
                            "hit-word>");
                    offset = offset + highlightFragment.length();
                }
                xml
                    .append("<").append(getNamespacePrefix(namespacePrefix))
                    .append("text-fragment-data>");
                xml.append("<![CDATA[").append(replacedFragment).append("]]>");
                xml
                    .append("</").append(getNamespacePrefix(namespacePrefix))
                    .append("text-fragment-data>");
                xml.append(indexFields);
                xml
                    .append("</").append(getNamespacePrefix(namespacePrefix))
                    .append("text-fragment>");
            }
        }
        return xml.toString();
    }

    /**
     * Sometimes, hit-words start with ( or < etc. Replace this
     * 
     * @param textFragment
     *            textFragment.
     * @return String text with replaced special characters
     * @sb
     */
    private String replaceSpecialCharacters(final String textFragment) {
        String text = textFragment;
        if (text != null) {
            text =
                text.replaceAll("(" + highlightStartMarker + ")([\\(\\)]+)",
                    "$2$1");
            text =
                text.replaceAll("([\\(\\)]+)(" + highlightEndMarker + ")",
                    "$2$1");
            text =
                text.replaceAll("(" + highlightStartMarker + ")(<[^<>]*?>)",
                    "$2$1");
            text =
                text.replaceAll("(<[^<>]*?>)(" + highlightEndMarker + ")",
                    "$2$1");
        }
        return text;
    }

    /**
     * Returns namespacePrefix with : if namespacePrefix is not null or empty.
     * 
     * @param namespacePrefix
     *            namespacePrefix to use for xml.
     * @return String namespacePrefix
     * @sb
     */
    private String getNamespacePrefix(final String namespacePrefix) {
        if (namespacePrefix != null && !namespacePrefix.equals("")) {
            return namespacePrefix + ":";
        }
        else {
            return "";
        }

    }

    /**
     * Adds highlightFragmentData to Collection of all highlifgtFragmentDatas.
     * 
     * @param highlightFragmentData
     *            HashMap.
     * @sb
     */
    public void addHighlightFragmentData(final HashMap highlightFragmentData) {
        highlightFragmentDatas.add(highlightFragmentData);
    }

}

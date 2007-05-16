/*
   Copyright 2006 OCLC Online Computer Library Center, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
 */
/*
 * SRUServerTester.java
 *
 * Created on November 1, 2005, 8:40 AM
 */

package ORG.oclc.os.SRW;

import gov.loc.www.zing.srw.interfaces.ExplainPort;
import gov.loc.www.zing.srw.interfaces.SRWPort;
import gov.loc.www.zing.srw.ExplainRequestType;
import gov.loc.www.zing.srw.ExplainResponseType;
import gov.loc.www.zing.srw.RecordType;
import gov.loc.www.zing.srw.RecordsType;
import gov.loc.www.zing.srw.ScanRequestType;
import gov.loc.www.zing.srw.ScanResponseType;
import gov.loc.www.zing.srw.SearchRetrieveRequestType;
import gov.loc.www.zing.srw.SearchRetrieveResponseType;
import gov.loc.www.zing.srw.service.SRWSampleServiceLocator;
import gov.loc.www.zing.srw.StringOrXmlFragment;
import gov.loc.www.zing.srw.TermType;
import gov.loc.www.zing.srw.TermsType;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Hashtable;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder; 
import javax.xml.parsers.DocumentBuilderFactory;  
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;

import org.apache.axis.message.MessageElement;
import org.apache.axis.types.NonNegativeInteger;
import org.apache.axis.types.PositiveInteger;

import org.apache.xpath.XPathAPI;

import org.w3c.dom.Document;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException; 	
import org.xml.sax.SAXParseException;

/**
 *
 * @author  levan
 */
public class SRUServerTester {

    boolean      good=true, runningAsMain=false, scanSupported=true;
    Element      ns = null;
    Hashtable    stylesheets=new Hashtable(), transformers=new Hashtable();
    int          numFailed=0, numTests=0, numWarns=0;
    String       baseURL=null;
    StringBuffer sb=new StringBuffer();
    Vector       terms=new Vector();

    public SRUServerTester(String baseURL) {
        if(baseURL.endsWith("?"))
            baseURL=baseURL.substring(0, baseURL.length()-1);
        this.baseURL=baseURL;

        // set up a document purely to hold the namespace mappings
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);   
            DocumentBuilder builder = factory.newDocumentBuilder();    
            DOMImplementation impl = builder.getDOMImplementation();
            Document namespaceHolder = impl.createDocument(
             "http://namespaces.cafeconleche.org/xmljava/ch3/", 
             "f:namespaceMapping", null);
            ns = namespaceHolder.getDocumentElement();
            ns.setAttributeNS("http://www.w3.org/2000/xmlns/",
                                "xmlns:srw",
                                "http://www.loc.gov/zing/srw/");
            ns.setAttributeNS("http://www.w3.org/2000/xmlns/",
                                "xmlns:exp",
                                "http://explain.z3950.org/dtd/2.0/");
            ns.setAttributeNS("http://www.w3.org/2000/xmlns/",
                                "xmlns:diag",
                                "http://www.loc.gov/zing/srw/diagnostic/");
        }
        catch(Exception e) {
            System.out.println("unable to build namespace record)");
            e.printStackTrace();
        }
    }
    
    public String test() {
        int i;
        String explainResponse;

        out("running SRU tests for baseURL: ");out(baseURL);out('\n');

        out("tests of Explain");out('\n');
        if(!isExplainResponse(explainResponse=sruRead(baseURL)))
            failed();
        Document explainDoc=null;
        if(explainResponse!=null) {
            explainDoc=renderXML(explainResponse);
            if(explainDoc!=null) {
                out("        successfully parsed Explain record");out('\n');
            }
            else
                failed();
        }
        if(explainResponse.indexOf("=\"http://www.loc.gov/zing/srw/\"")<0) {
            out("  **** Fatal **** explain record does not reference namespace: \"http://www.loc.gov/zing/srw/\"");out('\n');
            failed();
        }
        if(explainResponse.indexOf("=\"http://explain.z3950.org/dtd/2.0/\"")<0) {
            out("  **** Fatal **** explain record does not reference namespace: \"http://explain.z3950.org/dtd/2.0/\"");out('\n');
            return sb.toString();
        }
        if(numFailed!=0)
            return sb.toString();
        if(!isExplainResponse(sruRead(baseURL+"?")))
            failed();
        if(!isExplainResponse(sruRead(baseURL+"?operation=explain")))
            failed();
        if(!isExplainResponse(sruRead(baseURL+"?version=1.1")))
            failed();
        if(!isExplainResponse(sruRead(baseURL+"?operation=explain&version=1.1")))
            failed();
        
        if(explainDoc!=null) {
            // use the host:port/database to construct an explain request
            String ExplainResponsePath="/srw:explainResponse/srw:record/srw:recordData/exp:explain";
            String host=getNodeText(explainDoc, ExplainResponsePath+"/exp:serverInfo/exp:host");
            String port=getNodeText(explainDoc, ExplainResponsePath+"/exp:serverInfo/exp:port");
            String database=getNodeText(explainDoc, ExplainResponsePath+"/exp:serverInfo/exp:database");
            if(database.startsWith("/")) {
                out("  ** Warning: explain/serverInfo/database should not begin with a slash ('/')");out('\n');
                out("  ** explain/serverInfo/database = ");out(database);out('\n');
                numWarns++;
                database=database.substring(1);
            }
            baseURL="http://"+host+":"+port+"/"+database+"?";
            if(!isExplainResponse(sruRead(baseURL)))
                failed();
            // construct a list of indexes from the explainResponse
            NodeList indexes=getNodeList(explainDoc, ExplainResponsePath+"/exp:indexInfo/exp:index");
            baseURL=baseURL+"version=1.1";
            out('\n');


            // scan tests
            out("tests of Scan");out('\n');
            Document scanDoc=null;
            Node   node;
            String boilerPlate="&operation=scan&responsePosition=1&maximumTerms=5",
                   index, scanResponse, set, term;
            for(i=0; i<indexes.getLength() && scanSupported; i++) {
                node=indexes.item(i);
                set=getNodeText(node,  "exp:map/exp:name/@set");
                if(set!=null && set.length()>0)
                    index=set+"."+getNodeText(node,  "exp:map/exp:name");
                else
                    index=getNodeText(node,  "exp:map/exp:name");
                if(!index.equals("cql.resultSetId")) {
                    if(!scanIndex(index, "=", "dog"))
                        failed();
                    if(!scanIndex(index, "exact", "dog"))
                        failed();
                }
            }
            out('\n');

            // searchRetrieve tests
            out("tests of searchRetrieve");out('\n');
            if(terms.size()>0) { // yay!  we have a list of good terms from the scan tests!
                for(i=0; i<terms.size(); i++) {
                    if(!search((String)terms.elementAt(i)))
                        failed();
                }
            }
            else { // we'll walk through the index list again
                for(i=0; i<indexes.getLength(); i++) {
                    node=indexes.item(i);
                    set=getNodeText(node,  "exp:map/exp:name/@set");
                    if(set!=null && set.length()>0)
                        index=set+"."+getNodeText(node,  "exp:map/exp:name");
                    else
                        index=getNodeText(node,  "exp:map/exp:name");
                    if(!index.equals("cql.resultSetId")) {
                        if(!search(index, "=", "dog"))
                            failed();
                        if(!search(index, "+exact+", "dog"))
                            failed();
                    }
                }
            }
        }

        out('\n');
        out(numWarns+" warnings given");out('\n');
        out(numTests+" tests given");out('\n');
        out(numFailed+" tests failed");out('\n');
        if(good)
            out("all test passed!");
        else
            out("problems detected!");
        out('\n');
        String ret=sb.toString();
        return ret;
    }

    private void failed() {
        good=false;
        numFailed++;
    }

    public boolean isExplainResponse(String record) {
        int offset=-1;
        if(record!=null) {
            offset=record.indexOf(":explainResponse ");
            if(offset==-1)
                offset=record.indexOf("<explainResponse ");
        }
        if(offset<0 || offset>50) {
            out("test failed: expected an explainResponse, but got:");out('\n');
            out(record);out('\n');
            return false;
        }
        return true;
    }

    public boolean isScanResponse(String record) {
        int offset=-1;
        if(record!=null) {
            offset=record.indexOf(":scanResponse ");
            if(offset==-1)
                offset=record.indexOf("<scanResponse ");
        }
        if(offset<0 || offset>50) {
            out("test failed: expected a scanResponse, but got:");out('\n');
            out(record);out('\n');
            return false;
        }
        return true;
    }

    public boolean isSearchRetrieveResponse(String record) {
        int offset=-1;
        if(record!=null) {
            offset=record.indexOf(":searchRetrieveResponse ");
            if(offset==-1)
                offset=record.indexOf("<searchRetrieveResponse ");
        }
        if(offset<0 || offset>50) {
            out("test failed: expected a searchRetrieveResponse, but got:");out('\n');
            out(record);out('\n');
            return false;
        }
        return true;
    }

    private NodeList getNodeList(Node doc, String xPath) {
        try {
            return XPathAPI.selectNodeList(doc, xPath, ns);
        }
        catch(TransformerException e) {
            return null;
        }
    }

    private String getNodeText(Node doc, String xPath) {
        Node node=doc;
        if(xPath!=null) try {
            node=XPathAPI.selectSingleNode(doc, xPath, ns);
        }
        catch(TransformerException e) {
            return null;
        }
        String strRet = "";

        if (null != node) {
            if(node.getNodeType()==Node.ATTRIBUTE_NODE)
                return node.getNodeValue();
            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); ++i) {
                Node item = children.item(i);
                switch (item.getNodeType())
                {
                case Node.TEXT_NODE:
                case Node.CDATA_SECTION_NODE:
                        strRet += item.getNodeValue().trim();
                }
            }
        }

        return strRet;
    }
    
    public String getTerm(String index, String relation, String term, Document scanDoc) {
        NodeList terms=getNodeList(scanDoc, "/srw:scanResponse/srw:terms/srw:term/srw:value");
        if(terms==null || terms.getLength()==0) { // no terms found
            String diagURI=getNodeText(scanDoc, "/srw:scanResponse/srw:diagnostics/diag:diagnostic/diag:uri");
            if(diagURI!=null && diagURI.length()>0) {
                String diagDetails=getNodeText(scanDoc, "/srw:scanResponse/srw:diagnostics/diag:diagnostic/diag:details");
                if(diagURI.equals("info:srw/diagnostic/1/4")) {
                    out("        Unsupported operation: scan");
                    scanSupported=false;
                    return "";
                }
                if(diagURI.equals("info:srw/diagnostic/1/16")) {
                    out("        Unsupported index: "+index);
                    return "";
                }
                if(diagURI.equals("info:srw/diagnostic/1/22")) {
                    out("        Unsupported combination of index and relation: ");out(index);out(" and ");out(relation);out('\n');
                    return "";
                }
                out("            Diagnostic: ");out(diagURI);out(", details=");out(diagDetails);out('\n');
            }
            out("  ** Warning: scan for ");out(index);out(relation);out(term);out(" resulted in no terms");out('\n');
            out("  ** You should probably either issue a diagnostic or remove this index from the Explain record");out('\n');
            numWarns++;
            return "";
        }
        return "\""+getNodeText(terms.item(0), null)+"\"";
    }

    private void out(char c) {
        if(runningAsMain)
            System.out.print(c);
        else
            sb.append(c);
    }

    private void out(int i) {
        if(runningAsMain)
            System.out.print(i);
        else
            sb.append(i);
    }

    private void out(String s) {
        if(runningAsMain)
            System.out.print(s);
        else
            sb.append(s);
    }

    public Document renderXML(String record) {
        Document document;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        //factory.setValidating(true);
        factory.setNamespaceAware(true);
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new org.xml.sax.ErrorHandler() {	
                // ignore fatal errors (an exception is guaranteed)	
                public void fatalError(SAXParseException exception)	
                throws SAXException {	
                }	
                // treat validation errors as fatal	
                public void error(SAXParseException e)	
                throws SAXParseException	
                {	
                 throw e;	
                }	

                // dump warnings too	
                public void warning(SAXParseException err)	
                throws SAXParseException	
                {	
                 out("** Warning");
                 out(", line ");out(err.getLineNumber());
                 out(", uri ");out(err.getSystemId());out('\n');
                 out("   ");out(err.getMessage());out('\n');
                }
            });
            document = builder.parse(new InputSource(new StringReader(record)));
        }
        catch (java.io.IOException e) {
            out("test failed: unable to parse record: ");out(e.getMessage());out('\n');
            out(record);out('\n');
            return null;
        }
        catch (javax.xml.parsers.ParserConfigurationException e) {
            out("test failed: unable to parse record: ");out(e.getMessage());out('\n');
            out(record);out('\n');
            return null;
        }
        catch (org.xml.sax.SAXException e) {
            out("test failed: unable to parse record: ");out(e.getMessage());out('\n');
            out(record);out('\n');
            return null;
        }
        return document;
    }

    public boolean scanIndex(String index, String relation, String browseTerm) {
        if(!scanSupported)
            return true;
        Document scanDoc;
        String   scanResponse;
        if(!isScanResponse(scanResponse=sruRead(baseURL+"&scanClause="+
          index+"+"+relation+"+"+browseTerm+
          "&operation=scan&responsePosition=1&maximumTerms=5"))) {
            return false;
        }
        if((scanDoc=renderXML(scanResponse))==null)
            return false;
        out("        successfully parsed Scan record");out('\n');
        String foundTerm=getTerm(index, relation, browseTerm, scanDoc);
        if(foundTerm==null)
            return false;
        if(foundTerm.length()>0) {
            String  searchTerm=index+"+"+relation+"+"+foundTerm;
            terms.add(searchTerm);
            out("        scan returned ");out(searchTerm);out('\n');
        }
        return true;
    }

    public boolean search(String query) {
        Document doc;
        String   resp;
        if(!isSearchRetrieveResponse(resp=sruRead(baseURL+"&query="+query
          +"&operation=searchRetrieve&maximumRecords=1"))) {
            return false;
        }
        if((doc=renderXML(resp))==null)
            return false;
        out("        successfully parsed searchRetrieve record");out('\n');
        return true;
    }

    public boolean search(String index, String relation, String term) {
        return search(index+"+"+relation+"+"+term);
    }

    public void setRunningAsMain(boolean val) {
        runningAsMain=val;
    }

    public String sruRead(String initialURL) {
        out("    trying: ");out(initialURL);out('\n');
        numTests++;
        URL url=null;
        try {
            url=new URL(initialURL);
        }
        catch(java.net.MalformedURLException e) {
            out("test failed: using URL: ");out(e.getMessage());out('\n');
            return null;
        }
        HttpURLConnection huc=null;
        try {
            huc=(HttpURLConnection)url.openConnection();
        }
        catch(IOException e) {
            out("test failed: using URL: ");out(e.getMessage());out('\n');
            return null;
        }
        String contentType=huc.getContentType();
        if(contentType.indexOf("text/xml")<0) {
            out("*** Warning ***  Content-Type not set to text/xml");out('\n');
            out("    Content-type: ");out(contentType);out('\n');
            numWarns++;
        }
        InputStream urlStream=null;
        try {
            urlStream=huc.getInputStream();
        }
        catch(java.io.IOException e) {
            out("test failed: opening URL: ");out(e.getMessage());out('\n');
            return null;
        }
        BufferedReader in = new BufferedReader(
                                new InputStreamReader(
                                urlStream));
        boolean xml=true;
        String href=null, inputLine=null;
        StringBuffer content=new StringBuffer(), stylesheet=null;
        Transformer transformer=null;
        try {
            inputLine=in.readLine();
        }
        catch(java.io.IOException e) {
            out("test failed: reading first line of response: ");out(e.getMessage());out('\n');
            return null;
        }
        if(inputLine==null) {
            out("test failed: No input read from URL");out('\n');
            return null;
        }
        if(!inputLine.startsWith("<?xml ")) {
            xml=false;
            content.append(inputLine);
        }

        if(xml) {
            // normally, you'd expect to read the next line of input here
            // but some servers don't put a newline after the initial <?xml ?>
            int offset=inputLine.indexOf('>');
            if(offset+2<inputLine.length()) {
                inputLine=inputLine.substring(offset+1);
                offset=inputLine.indexOf('<');
                if(offset>0)
                    inputLine=inputLine.substring(offset);
            }
            else try {
                inputLine=in.readLine();
            }
            catch(java.io.IOException e) {
                out("test failed: reading response: ");out(e.getMessage());out('\n');
                return null;
            }

            if(inputLine.startsWith("<?xml-stylesheet ")) {
                offset=inputLine.indexOf("href=");
                href=(inputLine.substring(inputLine.indexOf("href=")+6));
                href=href.substring(0, href.indexOf('"'));
                transformer=(Transformer)transformers.get(href);
                if(stylesheets.get(href)==null) try { // never seen this stylesheet before
                    out("        reading stylesheet: ");out(href);out('\n');
                    out("           from source: ");out(url.toString());out('\n');
                    StreamSource source = new StreamSource(url.toString()); 
                    TransformerFactory tFactory=TransformerFactory.newInstance();
                    Source stylesht = tFactory.getAssociatedStylesheet(
                        source, null, null, null);
                    transformer=tFactory.newTransformer(stylesht);
                    transformers.put(href, transformer);
                }
                catch(Exception e) {
                    e.printStackTrace();
                    out("unable to load stylesheet: ");out(e.getMessage());out('\n');
                }
                stylesheets.put(href, href);
            }
            else
                content.append(inputLine);out('\n');
        }

        try {
            while ((inputLine = in.readLine()) != null)
                content.append(inputLine);out('\n');
        }
        catch(java.io.IOException e) {
            out("test failed: reading response: ");out(e.getMessage());out('\n');
            return null;
        }


//        if(!xml) {
//            out("test failed: response was not an XML record");out('\n');
//            out(content.toString());out('\n');
//            return null;
//        }

        String contentStr=content.toString();
        if(transformer!=null) {
            StreamSource streamXMLRecord=new StreamSource(new StringReader(contentStr));
            StringWriter xmlRecordWriter=new StringWriter();
            try {
                transformer.transform(streamXMLRecord,
                    new StreamResult(xmlRecordWriter));
                out("        successfully applied stylesheet '");out(href);out("'");out('\n');
            }
            catch(javax.xml.transform.TransformerException e) {
                out("unable to apply stylesheet '");out(href);out("'to response: ");out(e.getMessage());out('\n');
                e.printStackTrace();
            }
        }
        return contentStr;
    }

    public String SRWTest(String urlString) {
        try {
            SRWSampleServiceLocator service=new SRWSampleServiceLocator();
            URL url=new URL(urlString);
            ExplainPort explain=service.getExplainSOAP(url);
            ExplainRequestType explainRequest=new ExplainRequestType();
            explainRequest.setRecordPacking("xml");
            explainRequest.setVersion("1.1");
            ExplainResponseType explainResponse=explain.explainOperation(explainRequest);
            System.out.println("explainResponse="+explainResponse);
            
            SRWPort port=service.getSRW(url);
            ScanRequestType scanRequest=new ScanRequestType();
            scanRequest.setVersion("1.1");
            scanRequest.setScanClause("education");
            ScanResponseType scanResponse=port.scanOperation(scanRequest);
            if(scanResponse!=null) {
                TermsType terms=scanResponse.getTerms();
                if(terms!=null) {
                    TermType[] term=terms.getTerm();
                    System.out.println(term.length+" terms returned");
                    for(int i=0; i<term.length; i++)
                        System.out.println(term[i].getValue()+"("+term[i].getNumberOfRecords().intValue()+")");
                }
                else
                    System.out.println("0 terms returned");
            }
            else
                System.out.println("no scan response returned");

            SearchRetrieveRequestType request=new SearchRetrieveRequestType();
            request.setVersion("1.1");
            request.setQuery("en and education");
            //request.setQuery("dc.title any sword");
            request.setRecordSchema("info:srw/schema/1/dc-v1.1");
            request.setStartRecord(new PositiveInteger("1"));
            request.setMaximumRecords(new NonNegativeInteger("1"));
            request.setRecordPacking("xml");
            SearchRetrieveResponseType response=
                port.searchRetrieveOperation(request);
            System.out.println("postings="+response.getNumberOfRecords());
            RecordType[] record;
            RecordsType records=response.getRecords();
            if(records==null || (record=records.getRecord())==null)
                System.out.println("0 records returned");
            else {
                System.out.println(record.length+" records returned");
                System.out.println("record="+record);
                System.out.println("record[0] has record number "+
                record[0].getRecordPosition());
                StringOrXmlFragment frag=record[0].getRecordData();
                System.out.println("frag="+frag);
                MessageElement[] elems=frag.get_any();
                System.out.println("elems="+elems);
                System.out.println("value="+elems[0].getValue());
            }
            System.out.println("nextRecordPosition="+response.getNextRecordPosition());
        }
        catch(Exception e) {
            return null;
        }
        return null;
    }

    public static void main(String[] args) {
        if(args.length==0) {
            System.out.println("usage: SRUServerTester SRUBaseURL");
            return;
        }
        SRUServerTester tester=new SRUServerTester(args[0]);
        tester.setRunningAsMain(true);
        tester.test();
    }
}

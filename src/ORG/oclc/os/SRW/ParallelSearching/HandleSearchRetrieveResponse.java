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
 * HandleSearchRetrieveResponse.java
 *
 * Created on July 12, 2005, 9:32 AM
 */

package ORG.oclc.os.SRW.ParallelSearching;

import gov.loc.www.zing.srw.diagnostic.DiagnosticType;
import gov.loc.www.zing.srw.DiagnosticsType;
import gov.loc.www.zing.srw.RecordType;
import gov.loc.www.zing.srw.RecordsType;
import gov.loc.www.zing.srw.SearchRetrieveResponseType;
import gov.loc.www.zing.srw.StringOrXmlFragment;
import java.text.ParseException;
import java.io.CharConversionException;
import java.io.EOFException;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.axis.message.MessageElement;
import org.apache.axis.message.Text;
import org.apache.axis.types.NonNegativeInteger;
import org.apache.axis.types.PositiveInteger;
import org.apache.axis.types.URI;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
//import org.w3c.dom.Document;
//import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import ORG.oclc.ber.DataDir;
import ORG.oclc.ber.DataDirTree;
import ORG.oclc.os.SRW.SRWDatabase;
import ORG.oclc.os.SRW.Utilities;
import ORG.oclc.RecordHandler.HandleSGML;
import ORG.oclc.RecordHandler.MalformedRecordException;

/**
 *
 * @author  levan
 */
public class HandleSearchRetrieveResponse extends HandleSGML {
    static Log log=LogFactory.getLog(HandleSearchRetrieveResponse.class);
    public static final int numberOfRecordsTag=11;
    public static final int recordsTag=12;
    public static final int recordTag=13;
    public static final int recordSchemaTag=14;
    public static final int recordPackingTag=15;
    public static final int recordDataTag=16;
    public static final int recordPositionTag=17;
    public static final int diagnosticsTag=19;
    public static final int nextRecordPositionTag=21;
    public static final int resultSetIdTag=26;
    public static final int resultSetIdleTimeTag=27;
    public static final int diagnosticTag=50;
    public static final int uriTag=51;
    public static final int detailsTag=52;

    /** Creates a new instance of HandleSearchRetrieveResponse */
    public HandleSearchRetrieveResponse() {
            StringBuffer tags=new StringBuffer();
            setIgnoreNamespaces(true);
            tags.append("searchRetrieveResponse 0 recordTag\n");
            tags.append("version                10\n");
            tags.append("numberOfRecords        ").append(numberOfRecordsTag).append("\n");
            tags.append("records                ").append(recordsTag).append("\n");
            tags.append("record                 ").append(recordTag).append("\n");
            tags.append("recordSchema           ").append(recordSchemaTag).append("\n");
            tags.append("recordPacking          ").append(recordPackingTag).append("\n");
            tags.append("recordData             ").append(recordDataTag).append(" contains_SGML\n");
            tags.append("recordPosition         ").append(recordPositionTag).append("\n");
            tags.append("echoedSearchRetrieveRequest 18 contains_SGML\n");
            tags.append("diagnostics            ").append(diagnosticsTag).append("\n");
            tags.append("diagnostic             ").append(diagnosticTag).append("\n");
            tags.append("uri                    ").append(uriTag).append("\n");
            tags.append("details                ").append(detailsTag).append("\n");
            tags.append("xmlns                  20\n");
            tags.append("nextRecordPosition     ").append(nextRecordPositionTag).append("\n");
            tags.append("xsi:nil                22\n");
            tags.append("lowestSetBit           23\n");
            tags.append("ber                    24\n");
            tags.append("rawBer                 25\n");
            tags.append("resultSetId            ").append(resultSetIdTag).append("\n");
            tags.append("resultSetIdleTime      ").append(resultSetIdleTimeTag).append("\n");
            tags.append("ns0                   100\n");
            tags.append("ns1                   101\n");
            tags.append("ns2                   102\n");
            tags.append("ns3                   103\n");
            tags.append("ns4                   104\n");
            tags.append("ns5                   105\n");
            tags.append("ns6                   106\n");
            tags.append("ns7                   107\n");
            tags.append("ns8                   108\n");
            tags.append("ns9                   109\n");
            try {
                loadTags(new StringReader(tags.toString()));
            }
            catch(IOException e) {
            }
            catch(ParseException e) {
            }
            try {
                setByteToCharConverter("utf8");
            }
            catch(UnsupportedEncodingException e) {
            }
    }

    public SearchRetrieveResponseType getNextSearchRetrieveResponse(String recordPacking) throws CharConversionException, EOFException, IOException, MalformedRecordException {
        DataDir dir=getNextRecord();
        int i;
        SearchRetrieveResponseType response=new SearchRetrieveResponseType();
        if(log.isDebugEnabled())
            log.debug("remote response:\n"+dir);
        String numRecs=getNumberOfRecords(dir);
        response.setNumberOfRecords(new NonNegativeInteger(numRecs));
            
        DataDir recordsDir=dir.find(recordsTag);
        if(recordsDir!=null) {
            DataDir recordDir, tempDir;
            int         recordCount=recordsDir.count();
            RecordsType records=new RecordsType();
            String      recordStr;
            recordDir=recordsDir.child();
            if(recordDir.fldid()==2) { // attribute
                recordDir=recordDir.next();
                recordCount--;
            }
            RecordType  record, recordArray[]=new RecordType[recordCount];
            for(i=0; i<recordCount; i++, recordDir=recordDir.next()) {
                recordArray[i]=record=new RecordType();
                StringOrXmlFragment frag=new StringOrXmlFragment();
                MessageElement[] elems=new MessageElement[1];
                    recordStr=null;
                try {
                    recordStr=Utilities.unXmlEncode(recordDir.find(recordDataTag).find(1).getUTFString());
                    if(log.isDebugEnabled())
                        log.debug("unencoded record:\n"+ORG.oclc.util.Util.byteArrayToString(
                            recordStr.getBytes("utf-8")));
                }
                catch(java.io.UnsupportedEncodingException e) {} // can't happen
                catch(NullPointerException e) {
                    log.error("bad dir=\n"+dir);
                    log.error("bad recordDir=\n"+recordDir);
                }
                if(recordPacking.equals("xml")) {
                    DocumentBuilderFactory dbf=
                        DocumentBuilderFactory.newInstance();
                    dbf.setNamespaceAware(true);
                    try {
                        DocumentBuilder db=dbf.newDocumentBuilder();
                        StringReader sr=new StringReader(recordStr);
                        elems[0]=new MessageElement(db.parse(new InputSource(sr)).getDocumentElement());
                        sr.close();
                    }
                    catch(ParserConfigurationException e) {
                        log.error(e, e);
                    }
                    catch(SAXException e) {
                        log.error(e, e);
                    }
                }
                else { // string
                    elems[0]=new MessageElement(new Text(recordStr));
                }
                frag.set_any(elems);
                record.setRecordData(frag);
                record.setRecordPacking(recordPacking);
                if((tempDir=recordDir.find(recordSchemaTag))!=null)
                    record.setRecordSchema(tempDir.find(1).getUTFString());
                if((tempDir=recordDir.find(recordPositionTag))!=null)
                    record.setRecordPosition(new PositiveInteger(tempDir.find(1).getUTFString()));
            }
            records.setRecord(recordArray);
            response.setRecords(records);
        }

        DataDir diagnosticsDir=dir.find(diagnosticsTag);
        if(diagnosticsDir!=null) {
            log.info("diagnosticsDir:\n"+diagnosticsDir);
            DataDir diagnosticDir, tempDir;
            DiagnosticsType diagnostics=new DiagnosticsType();
            int diagnosticsCount=diagnosticsDir.count();
            diagnosticDir=diagnosticsDir.child();
            if(diagnosticDir.fldid()==2) {
                diagnosticDir=diagnosticDir.next();
                diagnosticsCount--;
            }
            DiagnosticType diagnostic, diagnosticArray[]=new DiagnosticType[diagnosticsCount];
            for(i=0; i<diagnosticsCount; i++, diagnosticDir=diagnosticDir.next()) {
                diagnosticArray[i]=diagnostic=new DiagnosticType();
                if((tempDir=diagnosticDir.find(uriTag))!=null)
                    diagnostic.setUri(new URI(tempDir.find(1).getUTFString()));
                if((tempDir=diagnosticDir.find(detailsTag))!=null)
                    diagnostic.setDetails(tempDir.find(1).getUTFString());
            }
            diagnostics.setDiagnostic(diagnosticArray);
            response.setDiagnostics(diagnostics);
        }
        
        String resultSetId=getResultSetId(dir);
        if(resultSetId!=null) {
            response.setResultSetId(resultSetId);
            String resultSetIdleTime=getResultSetIdleTime(dir);
            if(resultSetIdleTime!=null)
                response.setResultSetIdleTime(new PositiveInteger(resultSetIdleTime));
        }

        DataDir nextRecordPosition=dir.find(nextRecordPositionTag);
        if(nextRecordPosition!=null)
            response.setNextRecordPosition(new PositiveInteger(nextRecordPosition.find(1).getUTFString()));
        if(dir instanceof DataDirTree)
            DataDirTree.freeTree((DataDirTree)dir);
        return response;
    }

    public static String getNumberOfRecords(DataDir dir) {
        DataDir numRecsDir=dir.find(numberOfRecordsTag);
        if(numRecsDir!=null) {
            String numRecsStr=numRecsDir.find(1).getUTFString();
            if(numRecsStr!=null && numRecsStr.length()>0)
                return numRecsStr;
        }
        return "0";
    }

    public static String getResultSetId(DataDir dir) {
        DataDir resultSetId=dir.find(resultSetIdTag);
        if(resultSetId!=null)
            return resultSetId.find(1).getUTFString();
        return null;
    }

    public static String getResultSetIdleTime(DataDir dir) {
        DataDir resultSetIdleTime=dir.find(resultSetIdleTimeTag);
        if(resultSetIdleTime!=null)
            return resultSetIdleTime.find(1).getUTFString();
        return null;
    }
}

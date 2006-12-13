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
 * HandleScanResponse.java
 *
 * Created on July 13, 2005, 4:05 PM
 */

package ORG.oclc.os.SRW.ParallelSearching;

import ORG.oclc.os.SRW.Utilities;
import gov.loc.www.zing.srw.diagnostic.DiagnosticType;
import gov.loc.www.zing.srw.DiagnosticsType;
import gov.loc.www.zing.srw.ScanResponseType;
import gov.loc.www.zing.srw.TermType;
import gov.loc.www.zing.srw.TermsType;
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
import ORG.oclc.RecordHandler.HandleSGML;
import ORG.oclc.RecordHandler.MalformedRecordException;

/**
 *
 * @author  levan
 */
public class HandleScanResponse extends HandleSGML {
    static Log log=LogFactory.getLog(HandleScanResponse.class);
    public static final int termsTag=11;
    public static final int termTag=12;
    public static final int valueTag=13;
    public static final int numberOfRecordsTag=14;
    public static final int diagnosticsTag=19;
    public static final int diagnosticTag=50;
    public static final int uriTag=51;
    public static final int detailsTag=52;

    /** Creates a new instance of HandleScanResponse */
    public HandleScanResponse() {
            StringBuffer tags=new StringBuffer();
            setIgnoreNamespaces(true);
            tags.append("scanResponse 0 recordTag\n");
            tags.append("version                10\n");
            tags.append("terms                    ").append(termsTag).append("\n");
            tags.append("term                     ").append(termTag).append("\n");
            tags.append("value                    ").append(valueTag).append("\n");
            tags.append("numberOfRecords          ").append(numberOfRecordsTag).append("\n");
            tags.append("echoedScanRequest 15 contains_SGML\n");
            tags.append("xmlns                  16\n");
            tags.append("xsi:nil                17\n");
            tags.append("lowestSetBit           18\n");
            tags.append("diagnostics            ").append(diagnosticsTag).append("\n");
            tags.append("diagnostic             ").append(diagnosticTag).append("\n");
            tags.append("uri                    ").append(uriTag).append("\n");
            tags.append("details                ").append(detailsTag).append("\n");
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

    public ScanResponseType getNextScanResponse() throws CharConversionException, EOFException, IOException, MalformedRecordException {
        byte[] rec=loadRecord();
        if(rec==null) {
            log.error("got a null response back from Scan!");
            return null;
        }
        log.info("raw record:\n"+Utilities.byteArrayToString(rec));
        DataDir dir=toDataDir(rec);
        int i;
        ScanResponseType response=new ScanResponseType();
        if(log.isDebugEnabled())
            log.debug("remote response:\n"+dir);
        log.info("remote response:\n"+dir);
        DataDir termsDir=dir.find(termsTag);
        if(termsDir!=null) {
            DataDir termDir, tempDir;
            int       termCount=termsDir.count();
            TermsType terms=new TermsType();
            String      termStr;
            termDir=termsDir.child();
            if(termDir.fldid()==2) { // attribute
                termDir=termDir.next();
                termCount--;
            }
            TermType  term, termArray[]=new TermType[termCount];
            for(i=0; i<termCount; i++, termDir=termDir.next()) {
                termArray[i]=term=new TermType();
                term.setNumberOfRecords(new NonNegativeInteger(termDir.find(numberOfRecordsTag).find(1).getUTFString()));
                termStr=termDir.find(valueTag).find(1).getUTFString();
                term.setValue(termStr);
            }
            terms.setTerm(termArray);
            response.setTerms(terms);
        }

        DataDir diagnosticsDir=dir.find(diagnosticsTag);
        if(diagnosticsDir!=null) {
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
        if(dir instanceof DataDirTree)
            DataDirTree.freeTree((DataDirTree)dir);
        return response;
    }
}

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
 * SRWMergeDatabase.java
 *
 * Created on July 30, 2004, 4:17 PM
 */

package ORG.oclc.os.SRW.ParallelSearching;

import ORG.oclc.os.SRW.*;
import ORG.oclc.os.SRW.QueryResult;
import gov.loc.www.zing.srw.DiagnosticsType;
import gov.loc.www.zing.srw.RecordType;
import gov.loc.www.zing.srw.RecordsType;
import gov.loc.www.zing.srw.ScanRequestType;
import gov.loc.www.zing.srw.ScanResponseType;
import gov.loc.www.zing.srw.SearchRetrieveRequestType;
import gov.loc.www.zing.srw.SearchRetrieveResponseType;
import gov.loc.www.zing.srw.StringOrXmlFragment;
import gov.loc.www.zing.srw.TermType;
import gov.loc.www.zing.srw.TermsType;
import gov.loc.www.zing.srw.diagnostic.DiagnosticType;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.servlet.ServletException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;

import org.apache.axis.message.MessageElement;
import org.apache.axis.types.NonNegativeInteger;
import org.apache.axis.types.PositiveInteger;
import org.apache.axis.utils.XMLUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.w3c.dom.Document;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.z3950.zing.cql.CQLNode;
import org.z3950.zing.cql.CQLTermNode;


/**
 *
 * @author  levan
 */
public class SRWMergeDatabase extends SRWDatabaseImpl {
    static Log log=LogFactory.getLog(SRWMergeDatabase.class);

    Hashtable info=new Hashtable();
    int                 counter=0;
    RequestBucket       rb;
    SRWDatabaseThread[] db;

    public void addRenderer(String schemaName, String schemaID, Properties props) throws InstantiationException {
    }
    
    public static void addRecordToVector(RecordType originalRecord, long position,
      String recordPacking, Vector recordV)
      throws IOException, ParserConfigurationException, SAXException, SOAPException {
        RecordType record=new RecordType();
        record.setRecordPacking(recordPacking);
        record.setRecordPosition(new PositiveInteger(Long.toString(position)));
        record.setRecordSchema(originalRecord.getRecordSchema());
        MessageElement elems[]=originalRecord.getRecordData().get_any();
        String stringRecord=elems[0].toString();
        if(log.isDebugEnabled())
            log.debug("stringRecord="+stringRecord);
        log.debug("recordPacking"+recordPacking);
        StringOrXmlFragment frag=new StringOrXmlFragment();
        if(recordPacking==null || recordPacking.equals("xml")) {
            Document domDoc=XMLUtils.newDocument(
                new InputSource(
                new StringReader(Utilities.unXmlEncode(stringRecord))));
            elems=new MessageElement[1];
            elems[0]=new MessageElement(
                domDoc.getDocumentElement());
            frag.set_any(elems);
        }
        else { // srw
//            elems=new MessageElement[1];
//            elems[0]=new MessageElement();
//            elems[0].addTextNode(stringRecord);
            frag.set_any(elems);
        }
        record.setRecordData(frag);
        recordV.add(record);
    }

    private int compare(Object a, TermType b) {
        if(a instanceof String)
            return ((String)a).compareTo(b.getValue());
        return ((TermType)a).getValue().compareTo(b.getValue());
    }

    public ScanResponseType doRequest(ScanRequestType request)
      throws ServletException {
        long startTime=System.currentTimeMillis();
        rb.setRequest(request, ++counter);
        rb.waitUntilDone();
        int         bestFit, comp=1,
                    maximumTerms=request.getMaximumTerms().intValue(),
                    responsePosition=request.getResponsePosition().intValue()-1,
                    // make it zero ordinal
                    vectorIndex;
        long        count;
        String scanTerm="";
        try {
            CQLNode node=parser.parse(request.getScanClause());
            if(node instanceof CQLTermNode) {
                scanTerm=((CQLTermNode)node).getTerm();
                log.info("scanTerm="+scanTerm);
            }
        }
        catch(org.z3950.zing.cql.CQLParseException e) {
            log.error(e, e);
            throw new ServletException(e.getMessage());
        }
        catch(java.io.IOException e) {
            log.error(e, e);
            throw new ServletException(e.getMessage());
        }
        DiagnosticsType diagnostics, newDiagnostics=null;
        ScanResponseType scanResponse=null, newResponse=new ScanResponseType();
        TermType term, termArray[], tterm;
        TermsType terms;
        Vector termsV=new Vector();
        for(int i=0; i<db.length; i++) {
            if(db[i].scanResponse==null) {
                log.error("db["+i+"] ("+db[i].getName()+") returned: "+db[i].scanResponse);
                continue;
            }
            scanResponse=(ScanResponseType)db[i].scanResponse;
            diagnostics=scanResponse.getDiagnostics();
            if(diagnostics!=null) {
                if(newDiagnostics==null)
                    newDiagnostics=new DiagnosticsType();
                newDiagnostics.setDiagnostic(diagnostics.getDiagnostic());
            }
            terms=scanResponse.getTerms();
            if(terms==null) // no terms returned; probably got a diagnostic
                continue;
            termArray=terms.getTerm();
            vectorIndex=0;
            for(int j=0; j<termArray.length; j++) {
                term=termArray[j];
                if(vectorIndex>=termsV.size()) {
                    log.info("adding to end: "+term.getValue());
                    termsV.add(term);
                    vectorIndex++;
                }
                else {
                    while(vectorIndex<termsV.size() &&
                      (comp=compare(termsV.get(vectorIndex), term))<0)
                        vectorIndex++;
                    if(vectorIndex>=termsV.size()) {
                        log.info("adding to end: "+term.getValue());
                        termsV.add(term);
                        vectorIndex++;
                    }
                    else if(comp==0) {
                        tterm=(TermType)termsV.get(vectorIndex);
                        log.info("changing count: "+tterm.getValue());
                        count=tterm.getNumberOfRecords().longValue()+term.getNumberOfRecords().longValue();
                        tterm.setNumberOfRecords(new NonNegativeInteger(Long.toString(count)));
                    }
                    else {
                        log.info("inserting at "+vectorIndex+": "+term.getValue());
                        termsV.add(vectorIndex++, term);
                    }
                }
            }
        }
        if(maximumTerms>=termsV.size()) { // just return what we have
            log.info("number of terms returned: "+termsV.size());
            termArray=new TermType[termsV.size()];
            termArray=(TermType[])termsV.toArray(termArray);
        }
        else { // pick the ones to return
            log.info("number of terms returned: "+maximumTerms);
            for(bestFit=0; bestFit<termsV.size(); bestFit++)
                if(compare(scanTerm, (TermType)termsV.get(bestFit))<=0)
                    break;
            log.info("bestFit="+bestFit);
            int start=bestFit-responsePosition;
            if(start<0)
                start=0;
            if(start+maximumTerms>termsV.size())
                maximumTerms=termsV.size()-start;
            termArray=new TermType[maximumTerms];
            for(int i=0; i<maximumTerms; i++)
                termArray[i]=(TermType)termsV.get(start++);
        }
        if(termArray!=null & termArray.length>0) {
            terms=new TermsType();
            terms.setTerm(termArray);
            newResponse.setTerms(terms);
        }
        if(newDiagnostics!=null) {
            log.info(newDiagnostics.getDiagnostic(0).getUri());
            newResponse.setDiagnostics(newDiagnostics);
        }
        log.error("scan "+scanTerm+": ("+(System.currentTimeMillis()-startTime)+"ms)");
        return newResponse;
    }


//    public SearchRetrieveResponseType doRequest(
//      SearchRetrieveRequestType request) throws ServletException {
//        log.debug("enter doRequest(search)");
//        int                i, recordNum, resultSetTTL;
//        long               numRecs=defaultNumRecs, postings, startPoint=1;
//        NonNegativeInteger maxRecs=request.getMaximumRecords();
//        PositiveInteger startRec=request.getStartRecord();
//        if(startRec!=null) {
//            startPoint=startRec.longValue();
//            request.setStartRecord(null);
//        }
//        if(maxRecs!=null) {
//            if(maximumRecords>0)
//                numRecs=java.lang.Math.min(maxRecs.longValue(), maximumRecords);
//            else // never got set
//                numRecs=maxRecs.longValue();
//            // we don't want the db's to be fetching any records yet
//            request.setMaximumRecords(new NonNegativeInteger("0"));
//        }
//        if(request.getResultSetTTL()!=null)
//            resultSetTTL=request.getResultSetTTL().intValue();
//        else
//            resultSetTTL=defaultResultSetTTL;
//
//        SearchRetrieveResponseType newResponse=new SearchRetrieveResponseType(), response;
//        String query=request.getQuery(), sortKeys=request.getSortKeys();
//        try{
//            log.info("query:\n"+ORG.oclc.util.Util.byteArrayToString(query.getBytes("UTF-8")));
//        }catch(Exception e){}
//        if(sortKeys!=null && resultSetTTL==0)
//            resultSetTTL=defaultResultSetTTL;
//
//        CQLNode root;
//        try {
//            root = parser.parse(Utilities.escapeBackslash(query));
//        }
//        catch(java.io.IOException e) {
//            log.error(e);
//            return diagnostic(SRWDiagnostic.QuerySyntaxError, e.getMessage(),
//                newResponse);
//        }
//        catch(org.z3950.zing.cql.CQLParseException e) {
//            log.error(e);
//            return diagnostic(SRWDiagnostic.QuerySyntaxError, e.getMessage(),
//                newResponse);
//        }
//        DiagnosticsType diagnostics, newDiagnostics=null;
//        MergedQueryResult result=null;
//        String resultSetID=root.getResultSetName();
//        if(resultSetID!=null) {
//            log.info("resultSetID="+resultSetID);
//            result=(MergedQueryResult)oldResultSets.get(resultSetID);
//            if(result==null)
//                return diagnostic(SRWDiagnostic.ResultSetDoesNotExist,
//                    resultSetID, newResponse);
//        }
//        else {
//            rb.setRequest(request, ++counter);
//            rb.waitUntilDone();
//            result=new MergedQueryResult(db);
//            for(i=0; i<db.length; i++) {
//                if(db[i]==null)
//                    log.error("db["+i+"] is null!");
//                else {
//                    response=(SearchRetrieveResponseType)db[i].response;
//                    if(response==null)
//                        log.error("db["+i+"].response is null!");
//                    else {
//                        diagnostics=response.getDiagnostics();
//                        if(diagnostics!=null) {
//                            if(newDiagnostics==null)
//                                newDiagnostics=new DiagnosticsType();
//                            newDiagnostics.setDiagnostic(diagnostics.getDiagnostic());
//                        }
//                        result.setPartialResult(i, response);
//                    }
//                }
//            }
//            log.info("mergedResultSet: "+result);
//            if(result.getPostings()>0 && resultSetTTL>0) {
//                resultSetID=makeResultSetID();
//                oldResultSets.put(resultSetID, result);
//            }
//        }
//
//        if(resultSetID!=null) {
//            log.info("keeping resultSet '"+resultSetID+
//                "' for "+resultSetTTL+" seconds");
//            timers.put(resultSetID, new Long(
//                System.currentTimeMillis()+(resultSetTTL*1000)));
//            newResponse.setResultSetId(resultSetID);
//            newResponse.setResultSetIdleTime(
//                new PositiveInteger(Integer.toString(resultSetTTL)));
//        }
//        postings=result.getPostings();
//        log.info("'"+query+"'==>"+postings);
//        newResponse.setNumberOfRecords(new NonNegativeInteger(Long.toString(postings)));
//
//        if(postings>0) {
//            if(startPoint>postings)
//                diagnostic(SRWDiagnostic.FirstRecordPositionOutOfRange,
//                        null, newResponse);
//            else {
//                if((startPoint-1+numRecs)>postings)
//                    numRecs=postings-(startPoint-1);
//                if((startPoint+numRecs)<=postings)
//                    newResponse.setNextRecordPosition(new PositiveInteger(Long.toString(startPoint+numRecs)));
//                if(numRecs>0) { // now let's get some records
//                    request.setStartRecord(new PositiveInteger(Long.toString(startPoint)));
//                    request.setMaximumRecords(new NonNegativeInteger(Long.toString(numRecs)));
//                    rb.setRequest(request, result, ++counter);
//                    rb.waitUntilDone();
//                    RecordType[] recordArray;
//                    RecordsType records;
//                    String recordPacking=request.getRecordPacking();
//                    Vector recordV=new Vector();
//                    for(i=0; i<db.length; i++) {
//                        log.info("getting records from db #"+i);
//                        if(db[i].response!=null && (records=((SearchRetrieveResponseType)db[i].response).getRecords())!=null) {
//                            // got some records for us
//                            recordArray=records.getRecord();
//                            for(recordNum=0; recordNum<recordArray.length; recordNum++) {
//                                try {
//                                    addRecordToVector(recordArray[recordNum],
//                                        startPoint++, recordPacking, recordV);
//                                }
//                                catch(Exception e) {
//                                    log.error(e,e);
//                                }
//                            }
//                            for(i=i+1; i<db.length; i++) { // see if there are more
//                                if(db[i].response!=null && (records=((SearchRetrieveResponseType)db[i].response).getRecords())!=null) {
//                                    recordArray=records.getRecord();
//                                    for(recordNum=0; recordNum<recordArray.length; recordNum++) {
//                                        try {
//                                            addRecordToVector(recordArray[recordNum],
//                                                startPoint++, recordPacking, recordV);
//                                        }
//                                        catch(Exception e) {
//                                            log.error(e,e);
//                                        }
//                                    }
//                                }
//                            }
//                            break;
//                        }
//                    }
//                    records=new RecordsType();
//                    records.setRecord((RecordType[])recordV.toArray(new RecordType[recordV.size()]));
//                    newResponse.setRecords(records);
//                }
//            }
//        }
//
//        if(newDiagnostics!=null)
//            newResponse.setDiagnostics(newDiagnostics);
//        log.debug("exit doRequest(search)");
//        return newResponse;
//    }


    public String getConfigInfo() {
        return db[0].getDB().getConfigInfo();
    }

    public String getExtraResponseData(QueryResult result, SearchRetrieveRequestType request) {
        return null;
    }
    
    public String getIndexInfo() {
        return db[0].getDB().getIndexInfo();
    }


    public QueryResult getQueryResult(String query,
      SearchRetrieveRequestType request) throws InstantiationException {
        long startTime=System.currentTimeMillis();
        rb.setRequest(request, ++counter);
        rb.waitUntilDone();
        MergedQueryResult result=new MergedQueryResult(db, rb);
        QueryResult qr;
        for(int i=0; i<db.length; i++) {
            if(db[i]==null)
                log.error("db["+i+"] is null!");
            else {
                qr=(QueryResult)db[i].result;
                if(qr==null)
                    log.error("db["+i+"].result is null!");
                else {
                    if(qr.hasDiagnostics())
                        result.addDiagnostics(qr.getDiagnostics()); // if there were any
                    result.setPartialResult(i, qr);
                }
            }
        }
        log.error("search "+query+": ("+(System.currentTimeMillis()-startTime)+"ms)");
        return result;
    }
    
    public String getSchemaInfo() {
        return db[0].getDB().getSchemaInfo();
    }


    public void init(final String dbname, String srwHome, String dbHome,
      String dbPropertiesFileName, Properties dbProperties) {
        log.info("entering init, dbname="+dbname);
        initDB(dbname, srwHome, dbHome, dbPropertiesFileName, dbProperties);
        
        String dbList=dbProperties.getProperty("DBList");
        if (dbList==null) {
            log.error("DBList not specified");
            log.error(".props filename is " + dbPropertiesFileName);
            return;
        }
        log.info("dbList="+dbList);
        
        StringTokenizer st=new StringTokenizer(dbList, ", \t");
        int numThreads=st.countTokens();
        rb=new RequestBucket(numThreads);
        db=new SRWDatabaseThread[numThreads];
        String dbName;
        for(int i=0; i<numThreads; i++) {
            dbName=st.nextToken();
            try {
                db[i]=new SRWDatabaseThread();
                db[i].init(rb, i, (SRWDatabaseImpl)SRWDatabase.getDB(dbName, srwProperties));
                db[i].test("bogus");
                db[i].start();
            }
            catch(Exception e) {
                log.error(e);
            }
        }
        useSchemaInfo(getSchemaInfo());
        useConfigInfo(getConfigInfo());
        log.info("leaving init");
        return;
    }

    public boolean supportsSort() {
        return true;
    }
    
}

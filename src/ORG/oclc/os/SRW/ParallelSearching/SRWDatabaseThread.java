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
 * SRWDatabaseThread.java
 *
 * Created on November 19, 2002, 1:53 PM
 */

package ORG.oclc.os.SRW.ParallelSearching;

import ORG.oclc.os.SRW.QueryResult;
import ORG.oclc.os.SRW.SRWDatabase;
import ORG.oclc.os.SRW.SRWDatabaseImpl;
import gov.loc.www.zing.srw.ScanRequestType;
import gov.loc.www.zing.srw.ScanResponseType;
import gov.loc.www.zing.srw.SearchRetrieveRequestType;
import gov.loc.www.zing.srw.SearchRetrieveResponseType;

import org.apache.axis.types.NonNegativeInteger;
import org.apache.axis.types.PositiveInteger;
import org.apache.commons.logging.Log; 
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author  levan
 */
public class SRWDatabaseThread extends Thread {
    static Log log=LogFactory.getLog(SRWDatabaseThread.class);
    static int numInstances=0;

//    public Object response;

    int           instanceNum, lastRequestID, whoAmI;
    QueryResult result;
    RequestBucket rb;
    ScanResponseType scanResponse;
    SRWDatabaseImpl   db;

    public SRWDatabase getDB() {
        return db;
    }

    private void handleScan(ScanRequestType request) throws Exception {
        scanResponse=db.doRequest(request);
    }

    private void handleSearch(SearchRetrieveRequestType request) throws Exception {
            result=db.getQueryResult(request.getQuery(), request);
            log.info(getName()+": postings="+result.getNumberOfRecords());
//        }
//
//        PositiveInteger startRecord=request.getStartRecord();
//        if(startRecord!=null) { // a request for records
//            // do i have any of the records?
//            log.info("rb="+rb);
//            log.info("rb.result="+rb.result);
//            QueryResult result=(MergedQueryResult)rb.result;
//            long postings[]=mqr.getPostingsArray();
//            if(postings[whoAmI]>0) { // i might
//                long numRecs=request.getMaximumRecords().longValue();
//                long startRec=startRecord.longValue();
//                for(int i=0; i<whoAmI; i++)
//                    startRec-=postings[i];
//                //log.info("startRec="+startRec+", numRecs="+numRecs);
//                SearchRetrieveRequestType newRequest=new SearchRetrieveRequestType();
//                String resultSetId=mqr.getResultSetId(whoAmI);
//                if(resultSetId!=null)
//                    newRequest.setQuery("cql.resultSetId="+resultSetId);
//                else
//                    newRequest.setQuery(request.getQuery());
//                newRequest.setRecordPacking(request.getRecordPacking());
//                newRequest.setRecordSchema(request.getRecordSchema());
//                newRequest.setResultSetTTL(request.getResultSetTTL());
//                if(startRec<1) {
//                    numRecs=numRecs+startRec-1;
//                    startRec=1;
//                }
//                newRequest.setStartRecord(new PositiveInteger(
//                    Long.toString(startRec)));
//                if(startRec+numRecs>1 && startRec<=postings[whoAmI]) {
//                    newRequest.setStartRecord(new PositiveInteger(
//                        Long.toString(startRec)));
//                    newRequest.setMaximumRecords(new NonNegativeInteger(
//                        Long.toString(numRecs)));
//                }
//                else { // no records, but refresh the resultSet
//                    newRequest.setStartRecord(new PositiveInteger("1"));
//                    newRequest.setMaximumRecords(new NonNegativeInteger("0"));
//                }
//                response=db.doRequest(newRequest);
//            }
//            else
//                response=null; // nothing to say
//        }
    }

    public void init(RequestBucket rb, int whoAmI, SRWDatabaseImpl db) throws Exception {
        log.info("init for db: "+db);
        this.rb=rb;
        rb.register(this);
        this.whoAmI=whoAmI;
        lastRequestID=rb.requestID;
        instanceNum=numInstances++;
        setName(db.dbname+"("+instanceNum+")");
        this.db=db;
    }

    public void run() {
        log.info("enter run");
        while(true) {
            rb.waitForNewRequest(lastRequestID);
            if(rb.quit) {
                log.info("exit run");
                return;
            }
            lastRequestID=rb.requestID;
            try {
                if(rb.request instanceof SearchRetrieveRequestType)
                    handleSearch((SearchRetrieveRequestType)rb.request);
                else
                    handleScan((ScanRequestType)rb.request);
            }
            catch(Exception e) {
                log.error(e, e);
            }
            finally {
                rb.done(this);
            }
        }
    }

    public void test(String query) {
        try {
            SearchRetrieveRequestType request=new SearchRetrieveRequestType();
            request.setQuery(query);
            request.setResultSetTTL(new NonNegativeInteger("0"));
            request.setMaximumRecords(new NonNegativeInteger("0"));
            result=db.getQueryResult(request.getQuery(), request);
//            response=db.doRequest(request);
            log.info(getName()+": postings="+result.getNumberOfRecords());
        }
        catch(Exception e) {
            log.error(e, e);
        }
    }
}

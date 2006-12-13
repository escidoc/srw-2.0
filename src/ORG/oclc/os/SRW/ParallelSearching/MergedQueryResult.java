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
 * MergedResultSet.java
 *
 * Created on July 19, 2005, 10:18 AM
 */

package ORG.oclc.os.SRW.ParallelSearching;

import ORG.oclc.os.SRW.ParallelSearching.SRWDatabaseThread;
import ORG.oclc.os.SRW.QueryResult;
import ORG.oclc.os.SRW.RecordIterator;
import gov.loc.www.zing.srw.SearchRetrieveResponseType;

/**
 *
 * @author  levan
 */
public class MergedQueryResult extends QueryResult {
    long /*postings[],*/ totalPostings=0;
    RequestBucket rb;
//    String resultSetIds[];
    SRWDatabaseThread[] db;

    /**
     * Creates a new instance of MergedQueryResult
     */
    public MergedQueryResult(SRWDatabaseThread[] db, RequestBucket rb) {
        this.db=db;
        this.rb=rb;
//        postings=new long[db.length];
//        resultSetIds=new String[db.length];
    }

    public long getNumberOfRecords() {
        return totalPostings;
    }

//    public long getPostings(int index) {
//        return postings[index];
//    }

//    public long[] getPostingsArray() {
//        return postings;
//    }

//    public String getResultSetId(int index) {
//        return resultSetIds[index];
//    }

    public int getResultSetIdleTime() {
        return 0;
    }

    public QueryResult findResult(long startPoint) {
        for(int i=0; i<db.length; i++) {
            if(db[i].result.getNumberOfRecords()>startPoint)
                return db[i].result;
        }
        return null;
    }

    public String toString() {
        StringBuffer sb=new StringBuffer();
        sb.append("MergedQueryResult: numDBs=").append(db.length);
        sb.append(", totalPostings=").append(totalPostings).append('\n');
        for(int i=0; i<db.length; i++) {
            sb.append("i=").append(i).append(": ");
            sb.append("db=").append(db[i].getDB().dbname);
            sb.append(", postings=").append(db[i].result.getNumberOfRecords());
//            sb.append(", resultSetId=").append(resultSetIds[i]);
            sb.append('\n');
        }
        return sb.toString();
    }

    public RecordIterator newRecordIterator(long index, int numRecs, String schemaId) throws InstantiationException {
        return new MergedRecordIterator(this, index, numRecs, schemaId);
    }
    
    public void setPartialResult(int whichDb, QueryResult result) {
        setPartialResult(whichDb, result.getNumberOfRecords(), null);
    }

    public void setPartialResult(int whichDb, long postings, String resultSetId) {
//        this.postings[whichDb]=postings;
        totalPostings+=postings;
//        resultSetIds[whichDb]=resultSetId;
    }
}

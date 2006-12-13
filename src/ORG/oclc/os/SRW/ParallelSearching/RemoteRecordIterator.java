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
 * RemoteRecordIterator.java
 *
 * Created on November 10, 2005, 3:17 PM
 */

package ORG.oclc.os.SRW.ParallelSearching;

import ORG.oclc.ber.DataDir;
import ORG.oclc.ber.DataDirTree;
import ORG.oclc.os.SRW.Record;
import ORG.oclc.os.SRW.RecordIterator;
import ORG.oclc.os.SRW.Utilities;
import java.util.NoSuchElementException;
import java.util.Vector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author levan
 */
public class RemoteRecordIterator implements RecordIterator {
    static Log log=LogFactory.getLog(RemoteRecordIterator.class);
    DataDir recordDir[]=null, response;
    int     offset, recordCount=0;
    long    postings, startPoint;
    RemoteQueryResult result;
    String resultSetID=null, resultSetIdleTime, schemaID;
    
    /** Creates a new instance of RemoteRecordIterator */
    public RemoteRecordIterator(RemoteQueryResult result, long startPoint,
      int numRecs, String schemaID) throws InstantiationException {
        this.result=result;
        this.startPoint=startPoint;
        this.schemaID=schemaID;
        response=result.remoteResponse;
        offset=0;
        postings=Long.parseLong(HandleSearchRetrieveResponse.getNumberOfRecords(response));
        getRecordsFromResponse(response);
        DataDir dir=response.find(HandleSearchRetrieveResponse.resultSetIdTag);
        if(dir!=null)
            resultSetID=Utilities.urlEncode(dir.find(1).getUTFString());
        dir=response.find(HandleSearchRetrieveResponse.resultSetIdleTimeTag);
        if(dir!=null)
            resultSetIdleTime=dir.find(1).getUTFString();
    }

    public void close() {
        if(response instanceof DataDirTree)
            DataDirTree.freeTree((DataDirTree)response);
    }

    void getRecordsFromResponse(DataDir response) {
        long startTime=System.currentTimeMillis();
        DataDir dir=response.find(HandleSearchRetrieveResponse.recordsTag);
        if(dir!=null) {
            recordCount=dir.count();
            DataDir recDir=dir.child();
            if(recDir.fldid()==2) { // attribute
                recDir=recDir.next();
                recordCount--;
            }
            recordDir=new DataDir[recordCount];
            for(int i=0; i<recordCount; i++, recDir=recDir.next())
                recordDir[i]=recDir;
        }
        else
            log.info("no records:\n"+response);
        log.error("got records from response in "+(System.currentTimeMillis()-startTime)+"ms");
    }

    public boolean hasNext() {
        if(recordDir==null) {
            log.info("recordDir==null");
            return false;
        }
        if(offset+1>recordDir.length) {
            log.info("offset+1="+(offset+1)+", recordDir.length="+recordDir.length);
            return false;
        }
        if(startPoint+offset<=postings)
            return true;

        log.info("startPoint="+startPoint+", offset="+offset+", postings="+postings);
        return false;
    }

    public Object next() throws NoSuchElementException {
        return nextRecord();
    }

    public Record nextRecord() throws NoSuchElementException {
        long startTime=System.currentTimeMillis();
        if(!hasNext())
            throw new NoSuchElementException("offset="+offset+", recordDir.length="+recordDir.length);
        DataDir recDir=recordDir[offset++];
        if(log.isDebugEnabled())
            log.debug("recDir="+recDir);
        String record=Utilities.unXmlEncode(recDir.find(HandleSearchRetrieveResponse.recordDataTag).find(1).getUTFString());
        DataDir schemaDir=recDir.find(HandleSearchRetrieveResponse.recordSchemaTag);
        String schema=null;
        if(schemaDir!=null)
            schema=schemaDir.find(1).getUTFString();
        Record rec=new Record(record, schema);
        log.error("got nextRecord in "+(System.currentTimeMillis()-startTime)+"ms");
        return rec;
    }
    
    public void remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
}

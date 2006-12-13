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
 * RemoteQueryResult.java
 *
 * Created on November 10, 2005, 1:58 PM
 */

package ORG.oclc.os.SRW.ParallelSearching;

import ORG.oclc.ber.DataDir;
import ORG.oclc.os.SRW.QueryResult;
import ORG.oclc.os.SRW.RecordIterator;

/**
 *
 * @author levan
 */
public class RemoteQueryResult extends QueryResult {
    DataDir remoteResponse;
    HandleSearchRetrieveResponse searchHandler;
    String  baseURL;

    /** Creates a new instance of RemoteQueryResult */
    public RemoteQueryResult(String baseURL, DataDir remoteResponse, HandleSearchRetrieveResponse searchHandler) {
        this.baseURL=baseURL;
        this.remoteResponse=remoteResponse;
        this.searchHandler=searchHandler;
    }
    public long getNumberOfRecords() {
        if(remoteResponse!=null)
            return Long.parseLong(HandleSearchRetrieveResponse.getNumberOfRecords(remoteResponse));
        return 0;
    }
    public int getResultSetIdleTime() {
        if(remoteResponse!=null) {
            String resultSetIdleTime=HandleSearchRetrieveResponse.getResultSetIdleTime(remoteResponse);
            if(resultSetIdleTime!=null)
                return Integer.parseInt(HandleSearchRetrieveResponse.getResultSetIdleTime(remoteResponse));
        }
        return 0;
    }

    public RecordIterator newRecordIterator(long startPoint, int numRecs, String schemaID) throws InstantiationException {
        return new RemoteRecordIterator(this, startPoint, numRecs, schemaID);
    }
}

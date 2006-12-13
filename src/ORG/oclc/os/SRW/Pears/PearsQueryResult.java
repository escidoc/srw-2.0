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
    PearsQueryResult.java
    Created November 1, 2005, 2:11 PM
 */

package ORG.oclc.os.SRW.Pears;

import ORG.oclc.Newton.db.RestrictorSummary;
import ORG.oclc.os.gwen.Result;
import ORG.oclc.os.SRW.QueryResult;
import ORG.oclc.os.SRW.RecordIterator;
import gov.loc.www.zing.srw.SearchRetrieveRequestType;
import java.util.Hashtable;
import java.util.Iterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author levan
 */
public class PearsQueryResult extends QueryResult {
    static Log log=LogFactory.getLog(PearsQueryResult.class);

    private boolean restrictorSummary=false;
    Hashtable extraRequestDataElements;
    Result result=null;
    SRWPearsDatabase spdb;
    String cqlQuery;

    public PearsQueryResult(SRWPearsDatabase spdb) {
        this.spdb=spdb;
    }

    /** Creates a new instance of PearsQueryResult */
    public PearsQueryResult(SRWPearsDatabase spdb, Result result, String cqlQuery, Hashtable extraRequestDataElements) {
        this.spdb=spdb;
        this.cqlQuery=cqlQuery;
        this.result=result;
        this.extraRequestDataElements=extraRequestDataElements;
        String s=(String)extraRequestDataElements.get("restrictorSummary");
        if(s!=null && !s.equals("false")) {
            restrictorSummary=true;
            log.info("turned restrictorSummary on");
        }
        log.info(cqlQuery+"==>"+result.getPostings());
    }
    
    public long getNumberOfRecords() {
        if(result==null) // probably just holding diagnostics
            return 0;
        return result.getPostings();
    }

    public RestrictorSummary getRestrictorSummary() {
        return (RestrictorSummary)result.getRestrictorSummary();
    }

    public boolean isRestrictorSummary() {
        return restrictorSummary;
    }

    public RecordIterator newRecordIterator(long startPoint, int numRecs,
      String schemaID)
      throws InstantiationException {
        return new PearsRecordIterator(spdb, result, startPoint, schemaID, extraRequestDataElements);
    }
}

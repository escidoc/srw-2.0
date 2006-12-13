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
 * PearsRecordIterator.java
 *
 * Created on November 1, 2005, 2:29 PM
 */

package ORG.oclc.os.SRW.Pears;

import ORG.oclc.RecordHandler.MalformedRecordException;
import ORG.oclc.RecordHandler.RecordHandler;
import ORG.oclc.RecordHandler.RecordRenderer;
import ORG.oclc.ber.BerString;
import ORG.oclc.ber.DataDir;
import ORG.oclc.ber.DataDirSource;
import ORG.oclc.os.SRW.SRWDiagnostic;
import ORG.oclc.os.SRW.Utilities;
import ORG.oclc.os.gwen.Database;
import gov.loc.www.zing.srw.SearchRetrieveRequestType;
import java.io.UnsupportedEncodingException;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ORG.oclc.os.gwen.DocumentIterator;
import ORG.oclc.os.gwen.Result;
import ORG.oclc.os.SRW.Record;
import ORG.oclc.os.SRW.RecordIterator;
import ORG.oclc.z39.Diagnostic1;


/**
 *
 * @author levan
 */
public class PearsRecordIterator implements RecordIterator {
    static final Log log=LogFactory.getLog(PearsRecordIterator.class);
    private boolean oaiHeaderInfo=false;
    DocumentIterator list=null;
    long startPoint;
    Result result;
    SearchRetrieveRequestType request;
    SRWPearsDatabase spdb;
    String schemaID;

    /** Creates a new instance of PearsRecordIterator */
    public PearsRecordIterator(SRWPearsDatabase spdb, Result result,
      long startPoint, String schemaID, Hashtable extraRequestDataElements)
      throws InstantiationException {
        if(log.isDebugEnabled())
            log.debug("startPoint="+startPoint+", schemaID="+schemaID);
        this.spdb=spdb;
        this.result=result;
        this.startPoint=startPoint;
        this.schemaID=schemaID;
        this.request=request;
        String s=(String)extraRequestDataElements.get("OaiHeader");
        if(s!=null && !s.equals("false")) {
            oaiHeaderInfo=true;
            log.info("turned oaiHeaderInfo on");
        }
        try {
            if (startPoint == 1)
                list = (DocumentIterator)result.getDocumentIdList();
            else
                list = (DocumentIterator)result.getDocumentIdList((int)startPoint);
        }
        catch(Diagnostic1 e) {
            log.error(e, e);
            throw new InstantiationException(e.getMessage());
        }
    }

    public void close() {
    }
    
    public boolean hasNext() {
        log.debug("startPoint="+startPoint+", postings="+result.getPostings());
        if(startPoint<=result.getPostings())
            return true;
        return false;
    }

    Record makeRecord(DataDir doc)
      throws Diagnostic1, InstantiationException, MalformedRecordException,
      SRWDiagnostic, UnsupportedEncodingException {
        Record record=formatRecord(doc, schemaID);
        if(oaiHeaderInfo) {
            record.setExtraRecordInfo(formatRecord(doc, "http://www.openarchives.org/OAI/2.0/#header").getRecord());
        }
        return record;
    }

    Record formatRecord(DataDir doc, String schemaID)
      throws Diagnostic1, InstantiationException, MalformedRecordException,
      SRWDiagnostic, UnsupportedEncodingException {
        RecordHandler  rh;
        RecordRenderer rr;
        if(log.isDebugEnabled())
            log.debug("raw document:\n"+doc);
        if(schemaID.startsWith("info:srw/schema/1/xer") || spdb.renderXER.get(schemaID)!=null) {
            log.info("renderXER");
            DataDirSource dds=new DataDirSource(doc);
            String s=dds.toString();
            if(log.isDebugEnabled())
                log.debug("XML from DataDirSource:\n"+Utilities.byteArrayToString(
                    s.getBytes("UTF8")));
            Record rec=new Record(s, schemaID);
            if(schemaID.startsWith("info:srw/schema/1/xer"))
                return rec;
            return spdb.transform(rec, schemaID);
        }

        if((rh=(RecordHandler)spdb.recordHandlers.get(schemaID))!=null) {
            log.info("RecordHandler");
            byte[] byteRecord=rh.fromDataDir(doc);
            if(log.isDebugEnabled()) {
                log.debug("XML version as provided by HandleSGML:");
                log.debug("\n"+Utilities.byteArrayToString(byteRecord));
            }
            return new Record(new String(byteRecord, "UTF8"), schemaID);
        }

        if((rr=(RecordRenderer)spdb.renderers.get(schemaID))!=null) {
            log.info("RecordRenderer");
            String s=rr.render(doc);
            if(log.isDebugEnabled())
                log.debug("Rendered XML:\n"+Utilities.byteArrayToString(
                    s.getBytes("UTF8")));
            return new Record(s, rr.getSchemaID());
        }

        throw new InstantiationException("no record transformation rule specified");
    }


    public Object next() throws NoSuchElementException {
        return nextRecord();
    }

    public Record nextRecord() throws NoSuchElementException {
        try {
            int listEntry=list.nextInt();
            startPoint++;
            DataDir recDir=new DataDir((BerString)spdb.pdb.getDocument(listEntry));
            return makeRecord(recDir);
        }
        catch(Diagnostic1 e) {
            log.error(e, e);
            throw new NoSuchElementException(e.getMessage());
        }
        catch(Exception e) {
            log.error(e, e);
            log.error("startPoint="+startPoint+", postings="+result.getPostings());
            throw new NoSuchElementException(e.getMessage());
        }
    }
    
    public void remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
}

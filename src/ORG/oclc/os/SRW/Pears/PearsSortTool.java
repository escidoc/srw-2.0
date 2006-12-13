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
 * PearsSortTool.java
 *
 * Created on October 18, 2005, 10:15 AM
 */

package ORG.oclc.os.SRW.Pears;

import java.util.Enumeration;
import java.util.Hashtable;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ORG.oclc.ber.DataDir;
//import ORG.oclc.os.SRW.BerElementExtractor;
import ORG.oclc.os.SRW.SortElementExtractorException;
import ORG.oclc.os.SRW.SortTool;
import ORG.oclc.os.SRW.SRWDiagnostic;
import ORG.oclc.RecordHandler.MalformedRecordException;
import ORG.oclc.z39.Diagnostic1;

/**
 *
 * @author levan
 */
public class PearsSortTool extends SortTool {
    static Log log=LogFactory.getLog(PearsSortTool.class);

    Transformer transformer=null;

    public PearsSortTool(String sortkey, Hashtable transformers) throws SRWDiagnostic {
        super(sortkey);
        if(!prefix.equalsIgnoreCase("xer")) {
            transformer=(Transformer)transformers.get(prefix);
            if(transformer==null) {
                log.error("no handler for schema "+prefix);
                if(log.isInfoEnabled()) {
                    for(Enumeration enum2=transformers.keys();
                      enum2.hasMoreElements();)
                        log.info("handler name="+(String)enum2.nextElement());
                }
                throw new SRWDiagnostic(SRWDiagnostic.UnsupportedSchemaForSort,
                    prefix);
            }
        }
    }

    public String extract(DataDir record) throws SortElementExtractorException {
//        if(extractor instanceof BerElementExtractor)
//            return extractor.extract(record);
//        try {
//            return extractor.extract(SRWPearsDatabase.makeRecord(record, transformer, schema));
//        }
//        catch(Diagnostic1 e) {
//            throw new SortElementExtractorException(e);
//        }
//        catch(MalformedRecordException e) {
//            throw new SortElementExtractorException(e);
//        }
//        catch(TransformerException e) {
//            throw new SortElementExtractorException(e);
//        }
        return null;
    }

    public void makeSortElementExtractor() throws SortElementExtractorException {
//        if(prefix.equals("xer")) {
//            extractor=new BerElementExtractor();
//            extractor.init(xpath, prefix, schema);
//        }
//        else {
//            super.makeSortElementExtractor();
//        }
    }

    
}

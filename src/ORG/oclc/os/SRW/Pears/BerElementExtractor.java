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
 * BerElementExtractor.java
 *
 * Created on June 1, 2005, 3:56 PM
 */

package ORG.oclc.os.SRW;

import java.util.StringTokenizer;
import java.util.Vector;
import org.apache.commons.logging.Log; 
import org.apache.commons.logging.LogFactory;
import ORG.oclc.ber.DataDir;
import ORG.oclc.os.SRW.SortElementExtractor;
/**
 *
 * @author  levan
 */
public class BerElementExtractor implements SortElementExtractor {
    static Log log=LogFactory.getLog(BerElementExtractor.class);
    int tags[]=null;
    String tagpath=null;

    public void init(String xpath, String prefix, String schema) throws SortElementExtractorException {
        tagpath=xpath;
        String          token;
        StringTokenizer st=new StringTokenizer(tagpath, "/");
        int count=st.countTokens(), i;
        if(count<3)
            throw new IllegalArgumentException("tagpath too short: tagpath=\""+
                tagpath+"\"");
        token=st.nextToken();
        if(!token.equals("ber"))
            throw new IllegalArgumentException(
                "tagpath must start with /ber/tag0: tagpath=\""+tagpath+"\"");
        token=st.nextToken();
        if(!token.equals("tag0"))
            throw new IllegalArgumentException(
                "tagpath must start with /ber/tag0: tagpath=\""+tagpath+"\"");
        tags=new int[count-2];
        for(i=0; i<tags.length; i++) {
            token=st.nextToken();
            log.info("tagpath token="+token);
            if(!token.startsWith("tag"))
            throw new IllegalArgumentException(
                    "element "+(i+3)+
                    " of the tagpath doesn't start with \"tag\": tagpath=\""+
                    tagpath+"\"");
            tags[i]=Integer.parseInt(token.substring(3));
        }
    }

    
    public String extract(Object record) {
        if(!(record instanceof DataDir))
            throw new IllegalArgumentException("Expected a DataDir");
        DataDir dir=(DataDir)record;
        int i;
        for(i=0; i<tags.length; i++) {
            dir=dir.find(tags[i]);
            if(dir==null) {
                if(log.isDebugEnabled())
                    log.debug("didn't find a matching element for tag: "+tags[i]);
                return null;
            }
        }
        String element=dir.getUTFString();
        return element;
    }
}

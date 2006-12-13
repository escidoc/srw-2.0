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
 * SRWRemoteDatabase.java
 *
 * Created on September 7, 2004, 4:24 PM
 */

package ORG.oclc.os.SRW.ParallelSearching;

import ORG.oclc.RecordHandler.MalformedRecordException;
import ORG.oclc.ber.DataDir;
import ORG.oclc.os.SRW.QueryResult;
import ORG.oclc.os.SRW.SRWDatabaseImpl;
import ORG.oclc.os.SRW.SRWDiagnostic;
import ORG.oclc.os.SRW.Utilities;
import gov.loc.www.zing.srw.ExtraDataType;
import gov.loc.www.zing.srw.ScanRequestType;
import gov.loc.www.zing.srw.ScanResponseType;
import gov.loc.www.zing.srw.SearchRetrieveRequestType;
import gov.loc.www.zing.srw.SearchRetrieveResponseType;

import java.io.BufferedReader;
import java.io.CharConversionException;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Properties;
import javax.servlet.ServletException;
import org.apache.axis.message.MessageElement;
import org.apache.axis.MessageContext;
import org.apache.axis.types.NonNegativeInteger;
import org.apache.axis.types.PositiveInteger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//import ORG.oclc.ber.DataDir;

/**
 *
 * @author  levan
 */
public class SRWRemoteDatabase extends SRWDatabaseImpl {
    static Log log=LogFactory.getLog(SRWRemoteDatabase.class);
    HandleScanResponse scanHandler=new HandleScanResponse();
    HandleSearchRetrieveResponse searchHandler=new HandleSearchRetrieveResponse();
    String configInfo, indexInfo, schemaInfo, url;
    
    public void addRenderer(String schemaName, String schemaID, Properties props)
      throws InstantiationException {
    }

    
    public ScanResponseType doRequest(ScanRequestType request)
      throws javax.servlet.ServletException {
        try {
            long startTime=System.currentTimeMillis();
            String scanClause=request.getScanClause();
            String urlStr=url+"&operation=scan&scanClause="+Utilities.urlEncode(scanClause);
            NonNegativeInteger responsePosition=request.getResponsePosition();
            if(responsePosition!=null)
                urlStr=urlStr+"&responsePosition="+responsePosition;
            PositiveInteger maximumTerms=request.getMaximumTerms();
            if(maximumTerms!=null)
                urlStr=urlStr+"&maximumTerms="+maximumTerms;
            log.info("urlStr="+urlStr);
            URL u=new URL(urlStr);
            scanHandler.Input(u.openStream());
            ScanResponseType response=scanHandler.getNextScanResponse();
            log.error("scan "+scanClause+": "+(System.currentTimeMillis()-startTime)+"ms");
            return response;
        }
        catch(Exception e) {
            log.error(e, e);
            throw new ServletException(e.getMessage());
        }
    }

    public String getConfigInfo() {
        return configInfo;
    }
    
    public String getIndexInfo() {
        return indexInfo;
    }
    
    public String getExtraResponseData(QueryResult result, SearchRetrieveRequestType request) {
        return null;
    }
    
    public static DataDir getRemoteResponse(String urlStr,
      HandleSearchRetrieveResponse searchHandler)
      throws CharConversionException, EOFException, IOException,
      MalformedRecordException {
        URL u;
        u=new URL(urlStr);
        log.info("before searchHandler.Input"+"; "+System.currentTimeMillis());
        InputStream is=null;
        int errorCount=0;
        IOException lastException=null;
        while(is==null && errorCount<100) {
            try {
                is=u.openStream();
            }
            catch(IOException e) {
                lastException=e;
                log.error(e);
                errorCount++;
                    u=new URL(urlStr);
            }
        }
        if(is==null)
            throw lastException;
        searchHandler.Input(is);
        log.info("after searchHandler.Input"+"; "+System.currentTimeMillis());
        return searchHandler.getNextRecord();
    }

    public QueryResult getQueryResult(String query, SearchRetrieveRequestType request) throws InstantiationException {
        long startTime=System.currentTimeMillis();
        String urlStr=url+"&operation=searchRetrieve&recordPacking=string&query="+Utilities.urlEncode(query);

        NonNegativeInteger maximumRecords=request.getMaximumRecords();
        if(maximumRecords!=null)
            urlStr=urlStr+"&maximumRecords="+maximumRecords;

        NonNegativeInteger startRecord=request.getStartRecord();
        if(startRecord!=null)
            urlStr=urlStr+"&startRecord="+startRecord;

        NonNegativeInteger resultSetTTL=request.getResultSetTTL();
        if(resultSetTTL!=null)
            urlStr=urlStr+"&resultSetTTL="+resultSetTTL;

        Hashtable extraRequestDataElements=parseElements(request.getExtraRequestData());
        String s=(String)extraRequestDataElements.get("restrictorSummary");
        if(s!=null && !s.equals("false")) {
//                restrictorSummary=true;
                urlStr=urlStr+"&x-info-5-restrictorSummary";
                log.info("turned restrictorSummary on");
            }
        s=(String)extraRequestDataElements.get("returnSortKeys");
        if(s!=null && !s.equals("false")) {
//                returnSortKeys=true;
            urlStr=urlStr+"&x-info-5-returnSortKeys";
            log.info("turned returnSortKeys on");
        }
        log.error("urlStr="+urlStr+"; "+System.currentTimeMillis());
        try {
            RemoteQueryResult rqr=new RemoteQueryResult(url, getRemoteResponse(urlStr, searchHandler), searchHandler);
            log.error("search "+query+": "+(System.currentTimeMillis()-startTime)+"ms");
            return rqr;
        }
        catch(CharConversionException e) {
            log.error(e, e);
            throw new InstantiationException(e.getMessage());
        }
        catch(EOFException e) {
            log.error(e, e);
            throw new InstantiationException(e.getMessage());
        }
        catch(IOException e) {
            log.error(e, e);
            throw new InstantiationException(e.getMessage());
        }
        catch(MalformedRecordException e) {
            log.error(e, e);
            throw new InstantiationException(e.getMessage());
        }
//            if(recordPacking==null) {
//                MessageContext msgContext=MessageContext.getCurrentContext();
//                if(msgContext!=null && msgContext.getProperty("sru")!=null)
//                    recordPacking="xml"; // default for sru
//                else
//                    recordPacking="string"; // default for srw
//            }
//            log.info("calling getNextSearchRetrieveResponse"+"; "+System.currentTimeMillis());
//            SearchRetrieveResponseType response=searchHandler.getNextSearchRetrieveResponse(recordPacking);
//            log.info("exit doRequest"+"; "+System.currentTimeMillis());
//            return response;
    }

    public String getSchemaInfo() {
        return schemaInfo;
    }

    public void init(String dbname, String srwHome, String dbHome,
      String dbPropertiesFileName, Properties dbProperties)
      throws java.net.MalformedURLException {
        log.debug("entering SRWRemoteDatabase.init, dbname="+dbname);
        initDB(dbname, srwHome, dbHome, dbPropertiesFileName, dbProperties);
        
        String baseUrl=dbProperties.getProperty("remoteURL");
        url=baseUrl+"?version=1.1";

        try {
            URL explainURL=new URL(baseUrl);
            log.info("baseUrl="+baseUrl);
            BufferedReader in = new BufferedReader(new InputStreamReader(
                explainURL.openStream()));
            log.debug("got an explain response from the remote database!");
            String inputLine;
            StringBuffer explainResponse=new StringBuffer();
            for(int i=0; (inputLine = in.readLine()) != null; i++)
                explainResponse.append(inputLine).append('\n');
            in.close();
            if(log.isDebugEnabled())
                log.debug(explainResponse);
            String explain=explainResponse.toString();
            int start=explain.indexOf("<indexInfo>");
            int stop=explain.indexOf("</indexInfo>")+12;
            indexInfo=explain.substring(start, stop);
            start=explain.indexOf("<schemaInfo>");
            stop=explain.indexOf("</schemaInfo>")+13;
            schemaInfo=explain.substring(start, stop);
            useSchemaInfo(schemaInfo);
            start=explain.indexOf("<configInfo>");
            stop=explain.indexOf("</configInfo>")+13;
            configInfo=explain.substring(start, stop);
            useConfigInfo(getConfigInfo());
        }
        catch(Exception e) {
            log.error(e,e);
        }
    }
    
    public void setMaximumRecords(int maximumRecords) {
    }
    
    public void setNumberOfRecords(int numberOfRecords) {
    }
    
    public boolean supportsSort() {
        return true;
    }
}

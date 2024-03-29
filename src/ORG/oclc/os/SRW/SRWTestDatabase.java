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
 * SRWTestDatabase.java
 *
 * Created on August 5, 2003, 4:17 PM
 */

package ORG.oclc.os.SRW;

import gov.loc.www.zing.srw.ScanRequestType;
import gov.loc.www.zing.srw.ScanResponseType;
import gov.loc.www.zing.srw.SearchRetrieveRequestType;
import gov.loc.www.zing.srw.TermType;
import gov.loc.www.zing.srw.TermsType;
import java.util.Properties;
import javax.servlet.ServletException;
import org.apache.axis.types.PositiveInteger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author  levan
 */
public class SRWTestDatabase extends SRWDatabaseImpl {
    Log log=LogFactory.getLog(SRWTestDatabase.class);
    int defaultNumRecs=0, maximumRecords=10;
    String schemaID="info:srw/schema/1/dc-v1.1";

    public void addRenderer(String schemaName, String schemaID, Properties props)
      throws InstantiationException {
    }
    
    public ScanResponseType doRequest(ScanRequestType request) throws ServletException {
        log.info("entering SRWTestDatabase.doRequest()");
        ScanResponseType response=new ScanResponseType();
        TermsType terms=new TermsType();
        TermType  term[]=new TermType[6];
        term[0]=new TermType();
        term[0].setValue("levan");
        term[0].setNumberOfRecords(new PositiveInteger("1"));
        term[1]=new TermType();
        term[1].setValue("ralph");
        term[1].setNumberOfRecords(new PositiveInteger("1"));
        term[2]=new TermType();
        term[2].setValue("record");
        term[2].setNumberOfRecords(new PositiveInteger("1"));
        term[3]=new TermType();
        term[3].setValue("srw");
        term[3].setNumberOfRecords(new PositiveInteger("1"));
        term[4]=new TermType();
        term[4].setValue("test");
        term[4].setNumberOfRecords(new PositiveInteger("1"));
        term[5]=new TermType();
        term[5].setValue("test001");
        term[5].setNumberOfRecords(new PositiveInteger("1"));
        terms.setTerm(term);
        response.setTerms(terms);
        log.info("leaving SRWTestDatabase.doRequest()");
        return response;
    }
    
    public String getDatabaseInfo() {
        StringBuffer sb=new StringBuffer();
        sb.append("        <databaseInfo>\n")
          .append("          <title>OCLC Research SRW/U Test Database</title>\n")
          .append("          <description>A database of one record used for testing an SRW/U installation</description>\n")
          .append("          <author>Ralph LeVan</author>\n")
          .append("          <contact>levan@oclc.org</contact>\n")
          .append("          <restrictions>None</restrictions>\n")
          .append("          </databaseInfo>\n");
        return sb.toString();
    }

    public String getExtraResponseData(QueryResult result,
      SearchRetrieveRequestType request) {
        return null;
    }

    public String getIndexInfo() {
        StringBuffer sb=new StringBuffer();
        sb.append("        <indexInfo>\n")
          .append("          <set identifier=\"info:srw/cql-context-set/1/cql-v1.1\"")
          .append(" name=\"cql\"/>\n")
          .append("          <index>\n")
          .append("            <title>Any</title>\n")
          .append("            <map>\n")
          .append("              <name set=\"cql\">any</name>\n")
          .append("              </map>\n")
          .append("            </index>\n")
          .append("          </indexInfo>\n");
        return sb.toString();
    }

    public int getMaximumRecords() {
        return maximumRecords;
    }

    public int getNumberOfRecords() {
        return defaultNumRecs;
    }

    public QueryResult getQueryResult(String query,
      SearchRetrieveRequestType request) {
        return new TestQueryResult();
    }

    public String getSchemaID(String schemaName) {
        return schemaID;
    }
    
    public String getSchemaInfo() {
        StringBuffer sb=new StringBuffer();
        sb.append("        <schemaInfo>\n")
          .append("          <schema identifier=\"").append(schemaID).append("\"\n")
          .append("              location=\"http://www.loc.gov/zing/srw/dc-schema.xsd\"\n")
          .append("              sort=\"false\" retrieve=\"true\" name=\"DC\">\n")
          .append("            <title>DC: Dublin Core Elements</title>\n")
          .append("            </schema>\n")
          .append("          </schemaInfo>\n");
        return sb.toString();
    }
    
    public boolean hasaConfigurationFile() {
        return false;
    }

    public void init(String dbname, String srwHome, String dbHome,
      String dbPropertiesFileName, Properties dbProperties) {
        log.debug("entering SRWTestDatabase.init, dbname="+dbname);
        super.initDB(dbname,  srwHome, dbHome, dbPropertiesFileName, dbProperties);
        explainStyleSheet="/SRW/explainResponse.xsl";
        scanStyleSheet="/SRW/scanResponse.xsl";
        searchStyleSheet="/SRW/searchRetrieveResponse.xsl";
        log.debug("leaving SRWTestDatabase.init");
        return;
    }

    public void setMaximumRecords(int maximumRecords) {
        this.maximumRecords=maximumRecords;
    }
    
    public void setNumberOfRecords(int numberOfRecords) {
        defaultNumRecs=numberOfRecords;
    }
    
    public boolean supportsSort() {
        return false;
    }
}

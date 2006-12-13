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
 * SRWDatabase.java
 *
 * Created on August 4, 2003, 1:49 PM
 */

package ORG.oclc.os.SRW;

//import ORG.oclc.os.SRW.Pears.SRWDatabasePool;
import gov.loc.www.zing.srw.diagnostic.DiagnosticType;
import gov.loc.www.zing.srw.DiagnosticsType;
import gov.loc.www.zing.srw.ExplainResponseType;
import gov.loc.www.zing.srw.ScanRequestType;
import gov.loc.www.zing.srw.ScanResponseType;
import gov.loc.www.zing.srw.SearchRetrieveRequestType;
import gov.loc.www.zing.srw.SearchRetrieveResponseType;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Timer;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.z3950.zing.cql.CQLParser;

/**
 *
 * @author  levan
 */
public abstract class SRWDatabase {
    static Log log=LogFactory.getLog(SRWDatabase.class);

    public static Hashtable dbs=new Hashtable(), oldResultSets=new Hashtable(),
                            pdbs=new Hashtable(), timers=new Hashtable();

    private static HouseKeeping houseKeeping=null;
    public static Properties srwProperties;
    public static String srwHome;
    private static Timer timer=new Timer();

    static {
        houseKeeping=new HouseKeeping(timers, oldResultSets, log);
        timer.schedule(houseKeeping, 60000L, 60000L);
    }
    private Random rand=new Random();
    public String  dbname, explainStyleSheet=null, scanStyleSheet=null,
                   searchStyleSheet=null;

    protected CQLParser  parser = new CQLParser();
    protected Hashtable nameSpaces=new Hashtable(), schemas=new Hashtable(),
                        transformers=new Hashtable();
    protected int defaultNumRecs=10, defaultResultSetTTL, maximumRecords=20;
    protected Properties dbProperties;
    protected String dbHome;
    String     dbPropertiesFileName, defaultSchema, defaultStylesheet=null,
               explainRecord=null, schemaInfo;
    
    public abstract void addRenderer(String schemaName, String schemaID,
        Properties props) throws InstantiationException;

    public abstract SearchRetrieveResponseType doRequest(
      SearchRetrieveRequestType request) throws ServletException;

    public abstract ScanResponseType doRequest(
      ScanRequestType request) throws ServletException;

    public abstract String getIndexInfo();

    //public abstract void init(String dbname, Properties properties);
    public abstract void init(String dbname, String srwHome, String dbHome,
        String dbPropertiesFileName, Properties dbProperties) throws Exception;

    public abstract boolean supportsSort();
    
    private void addSupports(String s, StringBuffer sb) {
        StringTokenizer st=new StringTokenizer(s, " =");
        if(st.countTokens()<2)
            return;
        sb.append("        <supports type=\"").append(st.nextToken()).append("\">");
        sb.append(s.substring(s.indexOf("=")+1).trim()).append("</supports>\n");
    }
    
    public void close() {
        timer.cancel();
    }

    public Transformer addTransformer(String schemaName, String schemaID,
      String transformerFileName) throws FileNotFoundException, TransformerConfigurationException {
        if(schemaID!=null) {
            schemas.put(schemaName, schemaID);
            schemas.put(schemaID, schemaID);
        }
        if(transformerFileName==null) {
            log.info(schemaName+".transformer not specified");
            log.info(".props filename is " + dbPropertiesFileName);
            return null;
        }
        if(transformerFileName.startsWith("Renderer=")) // old notation, ignore
            return null;
        StringTokenizer st=new StringTokenizer(transformerFileName, " \t=");
        String token=st.nextToken();
        Source             xslSource;
        TransformerFactory tFactory=
            TransformerFactory.newInstance();
        xslSource=new StreamSource(Utilities.openInputStream(
            transformerFileName, dbHome, srwHome));
        if(xslSource==null) {
            log.error("Unable to make StreamSource for: "+
                transformerFileName);
            log.error(".props filename is " + dbPropertiesFileName);
            return null;
        }
        Transformer t=tFactory.newTransformer(xslSource);
        transformers.put(schemaName, t);
        if(schemaID!=null)
            transformers.put(schemaID, t);
        log.info("added transformer for schemaName "+schemaName+", and schemaID "+schemaID);
        return t;
    }


    public ExplainResponseType diagnostic(final int code,
      final String details, final ExplainResponseType response) {
        boolean         addDiagnostics=false;
        DiagnosticsType diagnostics=response.getDiagnostics();
        if(diagnostics==null)
            addDiagnostics=true;
        diagnostics=newDiagnostic(code, details, diagnostics);
        if(addDiagnostics)
            response.setDiagnostics(diagnostics);
        return response;
    }


    public ScanResponseType diagnostic(final int code,
      final String details, final ScanResponseType response) {
        boolean         addDiagnostics=false;
        DiagnosticsType diagnostics=response.getDiagnostics();
        if(diagnostics==null)
            addDiagnostics=true;
        diagnostics=newDiagnostic(code, details, diagnostics);
        if(addDiagnostics)
            response.setDiagnostics(diagnostics);
        return response;
    }


    public SearchRetrieveResponseType diagnostic(final int code,
      final String details, final SearchRetrieveResponseType response) {
        boolean         addDiagnostics=false;
        DiagnosticsType diagnostics=response.getDiagnostics();
        if(diagnostics==null)
            addDiagnostics=true;
        diagnostics=newDiagnostic(code, details, diagnostics);
        if(addDiagnostics)
            response.setDiagnostics(diagnostics);
        return response;
    }


    public String extractSortField(Object record) {
        return null;
    }

    public String getConfigInfo() {
        StringBuffer sb=new StringBuffer();
        sb.append("        <configInfo>\n");
        sb.append("          <default type=\"maximumRecords\">").append(getMaximumRecords()).append("</default>\n");
        sb.append("          <default type=\"numberOfRecords\">").append(getNumberOfRecords()).append("</default>\n");
        sb.append("          <default type=\"retrieveSchema\">").append((String)schemas.get("default")).append("</default>\n");
        if(dbProperties!=null) {
            String s=dbProperties.getProperty("supports");
            if(s!=null)
                addSupports(s, sb);
            else
                for(int i=1; ; i++) {
                    s=dbProperties.getProperty("supports"+i);
                    if(s!=null)
                        addSupports(s, sb);
                    else
                        break;
                }
        }
        sb.append("          </configInfo>\n");
        return sb.toString();
    }


    public String getDatabaseInfo() {
        StringBuffer sb=new StringBuffer();
        sb.append("        <databaseInfo>\n");
        if(dbProperties!=null) {
            String t=dbProperties.getProperty("databaseInfo.title");
            if(t!=null)
                sb.append("          <title>").append(t).append("</title>\n");
            t=dbProperties.getProperty("databaseInfo.description");
            if(t!=null)
                sb.append("          <description>").append(t).append("</description>\n");
            t=dbProperties.getProperty("databaseInfo.author");
            if(t!=null)
                sb.append("          <author>").append(t).append("</author>\n");
            t=dbProperties.getProperty("databaseInfo.contact");
            if(t!=null)
                sb.append("          <contact>").append(t).append("</contact>\n");
            t=dbProperties.getProperty("databaseInfo.restrictions");
            if(t!=null)
                sb.append("          <restrictions>").append(t).append("</restrictions>\n");
        }
        sb.append("          <implementation version='1.1' indentifier='http://www.oclc.org/research/software/srw'>\n");
        sb.append("            <title>OCLC Research SRW Server version 1.1</title>\n");
        sb.append("            </implementation>\n");
        sb.append("          </databaseInfo>\n");
        return sb.toString();
    }


    public static SRWDatabase getDB(String dbname, Properties properties) {
        log.debug("enter SRWDatabase.getDB");
        LinkedList queue=(LinkedList)dbs.get(dbname);
        SRWDatabase db=null;
        try {
        if(queue==null)
            log.info("No databases created yet for database "+dbname);
        else {
            log.debug("about to synchronize #1 on queue");
            synchronized(queue) {
                if(queue.isEmpty())
                    log.info("No databases available for database "+dbname);
                else {
                    db=(SRWDatabase)queue.removeFirst();
                    if(db==null)
                        log.debug("popped a null database off the queue for database "+dbname);
                }
            }
            log.debug("done synchronize #1 on queue");
        }
        if(db==null) {
            log.info("creating a database for "+dbname);
            try{
                while(db==null) {
                    initDB(dbname, properties);
                    queue=(LinkedList)dbs.get(dbname);
                    log.debug("about to synchronize #2 on queue");
                    synchronized(queue) {
                        if(!queue.isEmpty()) // crap, someone got to it before us
                            db=(SRWDatabase)queue.removeFirst();
                    }
                }
            log.debug("done synchronize #2 on queue");
            }
            catch(Exception e) { // database not available
                log.error(e, e);
                return null;
            }
        }
        }
        catch(Exception e) {
            log.error(e,e);
            log.error("shoot!");
        }
        if(log.isDebugEnabled())
            log.debug("getDB: db="+db);
        log.debug("exit SRWDatabase.getDB");
        return db;
    }

    
    public Properties getDbProperties() {
        return dbProperties;
    }

    
    public int getDefaultResultSetTTL() {
        return defaultResultSetTTL;
    }


    public String getDefaultSchema() {
        return defaultSchema;
    }


    public String getExplainRecord() {
        if(explainRecord==null)
            makeExplainRecord(null);
        return explainRecord;
    }


    public int getMaximumRecords() {
        return maximumRecords;
    }
    
    
    public String getMetaInfo() {
        StringBuffer sb=new StringBuffer();
        sb.append("        <metaInfo>\n");
        if(dbProperties!=null) {
            String t=dbProperties.getProperty("metaInfo.dateModified");
            if(t!=null)
                sb.append("          <dateModified>").append(t).append("</dateModified>\n");
            t=dbProperties.getProperty("metaInfo.aggregatedFrom");
            if(t!=null)
                sb.append("          <aggregatedFrom>").append(t).append("</aggregatedFrom>\n");
            t=dbProperties.getProperty("metaInfo.dateAggregated");
            if(t!=null)
                sb.append("          <dateAggregated>").append(t).append("</dateAggregated>\n");
        }
        sb.append("          </metaInfo>\n");
        return sb.toString();
    }


    public int getNumberOfRecords() {
        return defaultNumRecs;
    }


    public String getSchemaInfo() {
        return schemaInfo;
    }


    public boolean hasaConfigurationFile() {
        return true;  //  expect a configuration file unless overridden
    }


    public static void initDB(final String dbname, Properties properties)
      throws InstantiationException {
        log.debug("Enter: initDB, dbname="+dbname);
        String dbn="db."+dbname;

        srwProperties=properties;
        srwHome=properties.getProperty("SRW.Home");
        if(srwHome!=null && !srwHome.endsWith("/"))
            srwHome=srwHome+"/";
        log.debug("SRW.Home="+srwHome);
        Properties dbProperties=new Properties();
        String dbHome=properties.getProperty(dbn+".home"),
               dbPropertiesFileName=null;
        if(dbHome!=null) {
            if(!dbHome.endsWith("/"))
                dbHome=dbHome+"/";
            log.debug("dbHome="+dbHome);
        }

        String className=properties.getProperty(dbn+".class");
        log.debug("className="+className);
        if(className==null)
            throw new InstantiationException("No "+
                dbn+".class entry in properties file");
        if(className.equals("ORG.oclc.os.SRW.SRWPearsDatabase")) {
            log.info("** Warning ** the class ORG.oclc.os.SRW.SRWPearsDatabase has been replaced with ORG.oclc.os.SRW.Pears.SRWPearsDatabase");
            log.info("              Please correct the server's properties file");
            className="ORG.oclc.os.SRW.Pears.SRWPearsDatabase";
            log.debug("new className="+className);
        }
        else if(className.equals("ORG.oclc.os.SRW.SRWRemoteDatabase")) {
            log.info("** Warning ** the class ORG.oclc.os.SRW.SRWRemoteDatabase has been replaced with ORG.oclc.os.SRW.ParallelSearching.SRWRemoteDatabase");
            log.info("              Please correct the server's properties file");
            className="ORG.oclc.os.SRW.ParallelSearching.SRWRemoteDatabase";
            log.debug("new className="+className);
        }
        else if(className.equals("ORG.oclc.os.SRW.Pears.SRWRemoteDatabase")) {
            log.info("** Warning ** the class ORG.oclc.os.SRW.Pears.SRWRemoteDatabase has been replaced with ORG.oclc.os.SRW.ParallelSearching.SRWRemoteDatabase");
            log.info("              Please correct the server's properties file");
            className="ORG.oclc.os.SRW.ParallelSearching.SRWRemoteDatabase";
            log.debug("new className="+className);
        }
        else if(className.equals("ORG.oclc.os.SRW.SRWMergeDatabase")) {
            log.info("** Warning ** the class ORG.oclc.os.SRW.SRWMergeDatabase has been replaced with ORG.oclc.os.SRW.ParallelSearching.SRWMergeDatabase");
            log.info("              Please correct the server's properties file");
            className="ORG.oclc.os.SRW.ParallelSearching.SRWMergeDatabase";
            log.debug("new className="+className);
        }
        else if(className.equals("ORG.oclc.os.SRW.Pears.SRWMergeDatabase")) {
            log.info("** Warning ** the class ORG.oclc.os.SRW.Pears.SRWMergeDatabase has been replaced with ORG.oclc.os.SRW.ParallelSearching.SRWMergeDatabase");
            log.info("              Please correct the server's properties file");
            className="ORG.oclc.os.SRW.ParallelSearching.SRWMergeDatabase";
            log.debug("new className="+className);
        }
        else if(className.equals("ORG.oclc.os.SRW.SRWDLuceneDatabase")) {
            log.info("** Warning ** the class ORG.oclc.os.SRW.SRWLuceneDatabase has been replaced with ORG.oclc.os.SRW.DSpaceLucene.SRWLuceneDatabase");
            log.info("              Please correct the server's properties file");
            className="ORG.oclc.os.SRW.DSpaceLucene.SRWLuceneDatabase";
            log.debug("new className="+className);
        }
        SRWDatabase db=null;
        try {
            log.debug("creating class "+className);
            Class  dbClass=Class.forName(className);
            log.debug("creating instance of class "+dbClass);
            db=(SRWDatabase)dbClass.newInstance();
            log.debug("class created");
        }
        catch(Exception e) {
            log.error("Unable to create Database class "+className+
                " for database "+dbname);
            log.error(e, e);
            throw new InstantiationException(e.getMessage());
        }

        dbPropertiesFileName=properties.getProperty(dbn+".configuration");
        if(db.hasaConfigurationFile() || dbPropertiesFileName!=null) {
            if(dbPropertiesFileName==null) {
                throw new InstantiationException("No "+dbn+
                    ".configuration entry in properties file");
            }

            try {
                log.debug("Reading database configuration file: "+
                    dbPropertiesFileName);
                InputStream is=Utilities.openInputStream(dbPropertiesFileName, dbHome, srwHome);
                dbProperties.load(is);
                is.close();
            }
            catch(java.io.FileNotFoundException e) {
                log.error("Unable to open database configuration file!");
                log.error(e);
            }
            catch(Exception e) {
                log.error("Unable to load database configuration file!");
                log.error(e, e);
            }
            try {
                db.init(dbname, srwHome, dbHome, dbPropertiesFileName, dbProperties);
            }
            catch(Exception e) {
                log.error("Unable to initialize database "+dbname);
                log.error(e, e);
                throw new InstantiationException(e.getMessage());
            }
            String temp=dbProperties.getProperty("maximumRecords");
            if(temp!=null) {
                try {
                    db.setMaximumRecords(Integer.parseInt(temp));
                }
                catch(NumberFormatException e) {
                    log.error("bad value for maximumRecords: \""+temp+"\"");
                    log.error("maximumRecords parameter ignored");
                }
            }
            temp=dbProperties.getProperty("numberOfRecords");
            if(temp!=null) {
                try {
                    db.setNumberOfRecords(Integer.parseInt(temp));
                }
                catch(NumberFormatException e) {
                    log.error("bad value for numberOfRecords: \""+temp+"\"");
                    log.error("numberOfRecords parameter ignored");
                }
            }
            temp=dbProperties.getProperty("defaultResultSetTTL");
            if(temp!=null) {
                try {
                    db.setDefaultResultSetTTL(Integer.parseInt(temp));
                }
                catch(NumberFormatException e) {
                    log.error("bad value for defaultResultSetTTL: \""+temp+"\"");
                    log.error("defaultResultSetTTL parameter ignored");
                }
            }
            else
                db.setDefaultResultSetTTL(300);
        }
        else { // default settings
            try {
                db.init(dbname, srwHome, dbHome, null, null);
            }
            catch(Exception e) {
                log.error("Unable to create Database class "+className+
                    " for database "+dbname);
                log.error(e, e);
                throw new InstantiationException(e.getMessage());
            }
            db.setDefaultResultSetTTL(300);
            log.info("no configuration file needed or specified");
        }

        if(!(db instanceof SRWDatabasePool)) {
            LinkedList queue=(LinkedList)dbs.get(dbname);
            if(queue==null)
                queue=new LinkedList();
            queue.add(db);
            if(log.isDebugEnabled())
                log.debug(dbname+" has "+queue.size()+" copies");
            dbs.put(dbname, queue);
        }
        log.debug("Exit: initDB");
        return;
    }
    
    
    protected void initDB(final String dbname, String srwHome, String dbHome, String dbPropertiesFileName, Properties dbProperties) {
        log.debug("Enter: private initDB, dbname="+dbname);
        this.dbname=dbname;
        this.srwHome=srwHome;
        this.dbHome=dbHome;
        this.dbPropertiesFileName=dbPropertiesFileName;
        this.dbProperties=dbProperties;

        // get schema transformers
        if(dbProperties!=null) {
            String          firstSchema=null, xmlSchemaList=dbProperties.getProperty("xmlSchemas");
            StringBuffer    schemaInfoBuf=new StringBuffer("        <schemaInfo>\n");
            StringTokenizer st;
            if(xmlSchemaList!=null) {
                st=new StringTokenizer(xmlSchemaList, ", \t");
                String schemaIdentifier, schemaName, transformerName;
                log.info("xmlSchemaList="+xmlSchemaList);
                while(st.hasMoreTokens()) {
                    schemaName=st.nextToken();
                    log.debug("looking for schema "+schemaName);
                    if(firstSchema==null)
                        firstSchema=schemaName;
                    schemaIdentifier=dbProperties.getProperty(schemaName+".identifier");
                    transformerName=dbProperties.getProperty(schemaName+".transformer");
                    if(transformerName==null) {
                        // maybe this is an old .props file and the transformer name
                        // is associated with the bare schemaName
                        transformerName=dbProperties.getProperty(schemaName);
                    }

                    try {
                        addTransformer(schemaName, schemaIdentifier, transformerName);
                        addRenderer(schemaName, schemaIdentifier, dbProperties);
                        String schemaLocation=dbProperties.getProperty(schemaName+".location");
                        String schemaTitle=dbProperties.getProperty(schemaName+".title");
                        String schemaNamespace=dbProperties.getProperty(schemaName+".namespace");
                        if(schemaNamespace!=null)
                            nameSpaces.put(schemaName, schemaNamespace);
                        else
                            nameSpaces.put(schemaName, "NoNamespaceProvided");
                        schemaInfoBuf.append("          <schema sort=\"false\" retrieve=\"true\"")
                                     .append(" name=\"").append(schemaName)
                                     .append("\"\n              identifier=\"").append(schemaIdentifier)
                                     .append("\"\n              location=\"").append(schemaLocation).append("\">\n")
                                     .append("            <title>").append(schemaTitle).append("</title>\n")
                                     .append("            </schema>\n");
                    }
                    catch(Exception e) {
                        log.error("Unable to load schema "+schemaName);
                        log.error(e, e);
                    }
                }
                schemaInfoBuf.append("          </schemaInfo>\n");
                schemaInfo=schemaInfoBuf.toString();

                defaultSchema=dbProperties.getProperty("defaultSchema");
                if(defaultSchema==null)
                    defaultSchema=firstSchema;
                log.info("defaultSchema="+defaultSchema);
                schemaIdentifier=(String)schemas.get(defaultSchema);
                log.info("default schemaID="+schemaIdentifier);
                if(schemaIdentifier==null)
                    log.error("Default schema "+defaultSchema+" not loaded");
                else {
                    schemas.put("default", schemaIdentifier);
                    Transformer t=(Transformer)transformers.get(defaultSchema);
                    if(t!=null) {
                        transformers.put("default", t);
                    }
                }
            }

            explainStyleSheet=dbProperties.getProperty("explainStyleSheet");
            searchStyleSheet=dbProperties.getProperty("searchStyleSheet");
            scanStyleSheet=dbProperties.getProperty("scanStyleSheet");
        }
        log.debug("Exit: private initDB");
    }


    public void makeExplainRecord(HttpServletRequest request) {
        log.debug("Making an explain record for database "+dbname);
        StringBuffer sb=new StringBuffer();
sb.append("      <explain authoritative=\"true\" xmlns=\"http://explain.z3950.org/dtd/2.0/\">\n");
sb.append("        <serverInfo protocol=\"SRW/U\">\n");
if(request!=null) {
sb.append("          <host>"+request.getServerName()+"</host>\n");
sb.append("          <port>"+request.getServerPort()+"</port>\n");
sb.append("          <database>"+request.getContextPath().substring(1)+
  request.getServletPath()+request.getPathInfo()+"</database>\n");
}
sb.append("          </serverInfo>\n");
sb.append(getDatabaseInfo());
sb.append(getMetaInfo());
sb.append(getIndexInfo());
sb.append(getSchemaInfo());
sb.append(getConfigInfo());
sb.append("        </explain>\n");
        setExplainRecord(sb.toString());
    }


    protected String makeResultSetID() {
        int          i, j;
        StringBuffer sb=new StringBuffer();
        for(i=0; i<6; i++) {
            j=rand.nextInt(35);
            if(j<26)
                sb.append((char)('a'+j));
            else
                sb.append((char)('0'+j-26));
        }
        return sb.toString();
    }


    public static DiagnosticsType newDiagnostic(final int code,
      final String details, final DiagnosticsType diagnostics) {
        DiagnosticType  diags[];
        DiagnosticsType newDiagnostics=diagnostics;
        int numExistingDiagnostics=0;
        if(diagnostics!=null) {
            diags=diagnostics.getDiagnostic();
            numExistingDiagnostics=diags.length;
            DiagnosticType[] newDiags=
                new DiagnosticType[numExistingDiagnostics+1];
            System.arraycopy(diags, 0, newDiags, 0, numExistingDiagnostics);
            diags=newDiags;
            diagnostics.setDiagnostic(diags);
        }
        else {
            diags=new DiagnosticType[1];
            newDiagnostics=new DiagnosticsType();
            newDiagnostics.setDiagnostic(diags);
        }
        diags[numExistingDiagnostics]=SRWDiagnostic.newDiagnosticType(code, details);
        return newDiagnostics;
    }


    public static void putDb(String dbname, SRWDatabase db) {
        LinkedList queue=(LinkedList)dbs.get(dbname);
        log.debug("about to synchronize #3 on queue");
        synchronized(queue) {
            queue.add(db);
            if(log.isDebugEnabled())
                log.debug("returning "+dbname+" database to the queue; "+queue.size()+" available");
        }
        log.debug("done synchronize #3 on queue");
    }


    public void setDefaultResultSetTTL(int defaultResultSetTTL) {
        this.defaultResultSetTTL=defaultResultSetTTL;
    }


    public void setExplainRecord(String explainRecord) {
        this.explainRecord=explainRecord;
    }


    public void setMaximumRecords(int maximumRecords) {
        this.maximumRecords=maximumRecords;
    }
    

    public void setNumberOfRecords(int numberOfRecords) {
        this.defaultNumRecs=numberOfRecords;
    }
    

    public String toString() {
        StringBuffer sb=new StringBuffer();
        sb.append("Database ").append(dbname).append(" of type ")
          .append(this.getClass().getName());
        return sb.toString();
    }

}

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
 * SRWServletInfo.java
 *
 * Created on December 5, 2002, 2:19 PM
 */

package ORG.oclc.os.SRW;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Properties;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.xml.soap.SOAPBody;
import org.apache.axis.Message;
import org.apache.axis.MessageContext;
import org.apache.axis.transport.http.HTTPConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author  levan
 */
public class SRWServletInfo {
    public static final boolean isDebug=false;
    public static Log log=LogFactory.getLog(SRWServletInfo.class);
    public static String srwHome;

    private boolean      madeIndexDotHtml=false, makeIndexDotHtml=false;
    private Hashtable    extensions=new Hashtable(), namespaces=new Hashtable();
    public int           resultSetIdleTime=300; // time in seconds
    private Properties   properties=new Properties();
    public String        databaseURL, defaultDatabase, propsfileName;

    public SRWServletInfo() {
    }

    
    private static void buildDbList(Properties properties, Vector dbVector, String path) {
        buildDbList(properties, dbVector, path, null);
    }

    private static void buildDbList(Properties properties, Vector dbVector, String path, String remote) {
        Enumeration enumer=properties.propertyNames();
        String      fileName, dbHome, dbName, description, hidden=null, t;
        while(enumer.hasMoreElements()) {
            t=(String)enumer.nextElement();
            if(t.startsWith("db.") && t.endsWith(".class")) {
                dbName=t.substring(3, t.length()-6);
                if(remote==null)
                    hidden=properties.getProperty("db."+dbName+".hidden");
                description=properties.getProperty("db."+dbName+".description");
                if(description==null  && remote==null) { // see if it is in the database props
                    dbHome=properties.getProperty("db."+dbName+".home");
                    fileName=properties.getProperty(
                        "db."+dbName+".configuration");
                    if(fileName!=null)
                        try {
                            InputStream is=Utilities.openInputStream(
                                fileName, dbHome, srwHome);
                            Properties dbProperties=new Properties();
                            dbProperties.load(is);
                            is.close();
                            description=dbProperties.getProperty("databaseInfo.description");
                        }
                        catch(FileNotFoundException e) {
                            if(remote==null)
                                log.error(e);
                        }
                        catch(Exception e) {
                            if(remote==null)
                                log.error(e, e);
                        }
                }
                if(remote!=null || hidden==null || !hidden.equalsIgnoreCase("true")) {
                    dbVector.add(new DbEntry(dbName, remote, path, description));
                }
            }
            else if(remote==null && t.startsWith("remote.") && t.endsWith(".configuration")) {
                remote=t.substring(7, t.length()-14);
                try {
                    fileName=properties.getProperty(t);
                    String remotePath=properties.getProperty("remote."+remote+".path");
                    InputStream is=new URL(properties.getProperty(t)).openStream();
                    Properties srwProperties=new Properties();
                    srwProperties.load(is);
                    is.close();
                    buildDbList(srwProperties, dbVector, remotePath, remote);
                }
                catch(IOException e) {
                    log.error(e);
                }
                remote=null;
            }
        }
    }
    
    public String getDBName(final HttpServletRequest request) {
        String         dbname=defaultDatabase,
                       path=(String)request.getPathInfo();
//        log.info("pathInfo="+path);
        if(path!=null && path.indexOf('/')>=0)
            dbname=new StringTokenizer(path, "/").nextToken();
        log.info("dbname="+dbname);
        return dbname;
    }

    public String getExtension(String sruParm) {
        return (String)extensions.get(sruParm);
    }

    public String getNamespace(String sruParm) {
        return (String)namespaces.get(sruParm);
    }

    public Properties getProperties() {
        return properties;
    }
    
    public boolean handleExplain(final HttpServletRequest request,
      final HttpServletResponse response, final MessageContext msgContext)
      throws org.apache.axis.AxisFault, IOException {
        String dbname=getDBName(request),
               recordPacking=request.getParameter("recordPacking"),
               stylesheet=request.getParameter("stylesheet");
        log.info("Got an explain request for database "+dbname);
        SRWDatabase db=SRWDatabase.getDB(dbname, properties);
        if(db==null) {
            log.error("Non-existant database "+dbname);
            response.setStatus(404);
            return true;
        }
        if(recordPacking==null) {
                recordPacking="xml"; // default for sru
        }
        if(stylesheet==null)
            stylesheet=db.explainStyleSheet;
        PrintWriter    writer = response.getWriter();
        response.setContentType("text/xml");
        try{
            writeXmlHeader(writer, msgContext, request, stylesheet);
            writer.write("<SRW:explainResponse "+
            "xmlns:SRW=\"http://www.loc.gov/zing/srw/\">\n");
            writer.write("  <SRW:version>1.1</SRW:version>\n");
            writer.write("  <SRW:record>\n");
            writer.write("    <SRW:recordSchema>http://explain.z3950.org/dtd/2.0/</SRW:recordSchema>\n");
            writer.write("    <SRW:recordPacking>"+recordPacking+"</SRW:recordPacking>\n");
            writer.write("    <SRW:recordData>\n");
            String explainRecord=db.getExplainRecord();
            if(explainRecord==null) {
                db.makeExplainRecord(request);
                explainRecord=db.getExplainRecord();
            }
            if(recordPacking.equals("string"))
                Utilities.writeEncoded(writer, explainRecord);
            else
                writer.write(explainRecord);
            writer.write("      </SRW:recordData>\n");
            writer.write("    </SRW:record>\n");
            writer.write("  </SRW:explainResponse>\n");
        }
        catch(Exception e) {
            log.error(e, e);
        }
        writer.close();
        SRWDatabase.putDb(dbname, db);
        return true;
    }

    public void init(final ServletConfig config) {
        try {
            propsfileName=config.getInitParameter("PropertiesFile");
            if(propsfileName==null) {
                log.info("PropertiesFile not specified as an <init-param> in the web.xml");
                log.info("Using the default value of SRWServer.props instead");
                propsfileName="SRWServer.props";
            }
            log.info("Reading properties file: "+propsfileName);
            InputStream is;
            try {
                is=Utilities.openInputStream(propsfileName, "../", 
                  System.getProperties().getProperty("catalina.home"));
                properties.load(is);
                is.close();
            }
            catch(java.io.FileNotFoundException e) {
                log.error("Unable to load properties file: "+propsfileName);
                log.error(e);
                throw new ServletException("Unable to load properties file: "+
                  propsfileName);
            }
            srwHome=properties.getProperty("SRW.Home");
            log.info("SRW.Home="+srwHome);
            resultSetIdleTime=Integer.parseInt(
              properties.getProperty("resultSetIdleTime",
              Integer.toString(resultSetIdleTime)));
            log.info("resultSetIdleTime="+resultSetIdleTime+" seconds");
            defaultDatabase=properties.getProperty("default.database");
            if(defaultDatabase==null)
                defaultDatabase=
                    "default.database not specified in properties file";
            log.info("default.database="+defaultDatabase);
            String s=properties.getProperty("makeIndex.html");
            if(s!=null)
                if(s.equalsIgnoreCase("true"))
                    makeIndexDotHtml=true;

            // any dbs to open automatically?
            s=properties.getProperty("SRW.OpenAllDatabasesOnStartup");
            if(s==null) { // maybe a short list to open?
                s=properties.getProperty("SRW.OpenDatabasesInListOnStartup");
                if(s!=null) {
                    log.info("Opening databases: "+s);
                    initDBs(s);
                }
                else
                    log.info("Not opening databases yet");
            }
            else
                if(s.equalsIgnoreCase("true") || s.equalsIgnoreCase("t") ||
                  s.equals("1")) {
                    Enumeration enumer=properties.propertyNames();
                    int         offset;
                    String      dbname, list=new String(","), t;
                    while(enumer.hasMoreElements()) {
                        t=(String)enumer.nextElement();
                        if(t.startsWith("db.")) {
                            offset=t.indexOf(".", 3);
                            dbname=t.substring(3, offset);
                            if(list.indexOf(", "+dbname+",")==-1) { // not yet in list
                                list=list+" "+dbname+",";
                            }
                        }
                    }
                    log.info("Opening all databases :"+list);
                    initDBs(list);
                }

            // load the sru-srw extension mapping table
            Enumeration enumer=properties.propertyNames();
            String      extension, namespace, sruParm, t;
            while(enumer.hasMoreElements()) {
                t=(String)enumer.nextElement();
                if(t.startsWith("extension.") && !t.endsWith(".namespace")) {
                    sruParm=t.substring(10);
                    extension=properties.getProperty(t);
                    extensions.put(sruParm, extension);
                    namespace=properties.getProperty("extension."+extension+".namespace");
                    namespaces.put(sruParm, namespace);
                }
            }

            log.info("SRWServletInfo initialization complete");
        }
        catch(Exception e) {
            log.error(e, e);
        }
    }
    
    
    private void initDBs(final String list) throws InstantiationException {
        if(list==null || list.equals(""))
            return;
        String          dbname;
        StringTokenizer st=new StringTokenizer(list, ", ");
        while(st.hasMoreTokens()) {
            dbname=st.nextToken();
            SRWDatabase.initDB(dbname, properties);
        }
    }

    public static void makeIndexDotHtml(Properties properties, String srwHome,
      Log log, HttpServletRequest request) {
        Enumeration enumer;
        String      dbname, t;
        String      path=request.getContextPath()+request.getServletPath();
        try {
        PrintStream ps=new PrintStream(new FileOutputStream(srwHome+"index.html"));
        log.debug("writing index.html to: "+srwHome+"index.html");
        ps.println("<!doctype html public \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">");
        ps.println("<html>");
        ps.println("<head>");
        ps.println("<title>SRW/U Databases</title>");
        ps.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\" />");
        ps.println("<link href=\"http://www.oclc.org/common/css/basic_oclc.css\" rel=\"stylesheet\" type=\"text/css\" />");
        ps.println("<link href=\"http://www.oclc.org/common/css/researchproject_oclc.css\" rel=\"stylesheet\" type=\"text/css\" />");
        ps.println("<style type=\"text/css\">");
        ps.println("<!--");
        ps.println("table.layout { border: none; margin: 0; padding: 0; width: 100%; }");
        ps.println("table.layout td { border: none; margin: 0; padding: 0; width: 50%; }");
        ps.println("table.formtable th, table.formtable td { border-top: 1px solid #999; border-left: 1px solid #999; color: #333; padding: 4px; text-align: left; vertical-align: top; }");
        ps.println("input.button { margin: 0; }");
        ps.println("-->");
        ps.println("</style>");
        ps.println("</head>");
        ps.println("<body>");
        ps.println("<div align=\"center\">");
        ps.println("<table cellspacing=\"0\" id=\"bnrResearch\">");
        ps.println("<tr>");
        ps.println("<td id=\"tdResearch\"><a href=\"http://www.oclc.org/research/\">A Project of OCLC Research</a></td>");
        ps.println("<td id=\"tdOclc\"><a href=\"http://www.oclc.org/\">OCLC Online Computer Library Center</a></td>");
        ps.println("</tr>");
        ps.println("<tr>");
        ps.println("<td id=\"tdProject\"><h2><a href=\"index.html\">SRW/U Databases</a></h2></td>");
        ps.println("<td id=\"tdLogo\"><a href=\"http://www.oclc.org/research/software/srw\"><img height=\"15\" width=\"80\" alt=\"Powered by OCLC SRW/U\" src=\"http://www.oclc.org/research/images/badges/oclc_srwu.gif\"/></a></td>");
        ps.println("</tr>");
        ps.println("</table>");
        ps.println("</div>");
        ps.println("<table class=\"formtable\">");
        Vector dbVector=new Vector();
        buildDbList(properties, dbVector, path);
        Object[] dbList=dbVector.toArray();
        Arrays.sort(dbList);
        DbEntry de;
        for(int i=0; i<dbList.length; i++) {
            de=(DbEntry)dbList[i];
            ps.print("<tr><th><a href=\""+de.getPath()+"/"+de.getName()+"\">");
            if(de.getHost().length()>0)
                ps.print(de.getHost()+": ");
            ps.print(de.getName()+"</a></th>");
            if(de.getDescription()!=null) {
                ps.println("<td>"+de.getDescription()+"</td>");
            }
        }
        ps.println("</table>");
        ps.println("<a href=\"http://www.oclc.org/research/software/srw\">");
        ps.println("<img height=\"15\" width=\"80\" alt=\"Powered by OCLC SRW/U\" src=\"http://oaweb4server:8001/DesignDept/sandbox/osborne/badges/badge_srwu.gif\"/></p>");
        ps.println("</a>");
        ps.println("</body>");
        ps.println("</html>");
        ps.close();
        }
        catch(IOException e){log.error(e,e);}
    }
    
    public boolean setSRWStuff(final HttpServletRequest request,
      final HttpServletResponse response, final MessageContext msgContext) 
      throws org.apache.axis.AxisFault {
        log.debug("entering SRWServletInfo.setSRWStuff");
        databaseURL=request.getRequestURL().toString();
        String      dbname=getDBName(request);
        SRWDatabase db=SRWDatabase.getDB(dbname, properties);
        if(db==null) {
            log.error("Non-existant database "+dbname);
            response.setStatus(404);
            return false;
        }
        if(db.getExplainRecord()==null) {
            db.makeExplainRecord(request);
        }
        if(makeIndexDotHtml && !madeIndexDotHtml) {
            makeIndexDotHtml(properties, srwHome, log, request);
            makeIndexDotHtml=false;  // once is enough
        }
        
        msgContext.setProperty("dbname", dbname);
        msgContext.setProperty("db", db);
        msgContext.setProperty("databaseURL", databaseURL);
        msgContext.setProperty("resultSetIdleTime",
            new Integer(resultSetIdleTime));
        Message m=msgContext.getCurrentMessage();
        String msg=null;
        if(m!=null) {
            msg=m.getSOAPPartAsString();
            int i=msg.indexOf("Body");
            msg=msg.substring(i+5, i+35);
        }
//        log.info("msg part='"+msg+"'");
        if(msg!=null && msg.indexOf("explainRequest")>=0) {
//            log.info("set service to ExplainSoap");
            msgContext.setTargetService("ExplainSOAP");
        }
        else {
//            log.info("set service to SRW");
            msgContext.setTargetService("SRW");
        }
        log.debug("exiting SRWServletInfo.setSRWStuff");
        return true;
    }
    
    public void writeXmlHeader(final PrintWriter printWriter,
      final MessageContext msgContext, final HttpServletRequest req,
      final String defaultXsl) {
        printWriter.println("<?xml version=\"1.0\" ?> ");
        String xsl=req.getParameter("xsl"); // version 1.0
        if(xsl==null)
            xsl=req.getParameter("stylesheet"); // version 1.1
        if(xsl==null)
            xsl=defaultXsl;
        if(xsl!=null)
            printWriter.println("<?xml-stylesheet type=\"text/xsl\" "+
                "href=\""+xsl+"\"?>");
    }        


    public void writeXmlHeader(final javax.servlet.ServletOutputStream sos,
      final MessageContext msgContext, final HttpServletRequest req,
      final String defaultXsl) {
        StringBuffer sb=new StringBuffer();
        sb.append("<?xml version=\"1.0\" ?> \n");
        String xsl=req.getParameter("xsl"); // version 1.0
        if(xsl==null)
            xsl=req.getParameter("stylesheet"); // version 1.1
        if(xsl==null)
            xsl=defaultXsl;
        if(xsl!=null)
            sb.append("<?xml-stylesheet type=\"text/xsl\" href=\"")
              .append(xsl).append("\"?>\n");
        try {
            sos.write(sb.toString().getBytes("utf-8"));
        }
        catch(Exception e){}
    }        
}

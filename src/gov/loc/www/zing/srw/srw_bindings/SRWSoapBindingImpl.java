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

package gov.loc.www.zing.srw.srw_bindings;

import ORG.oclc.os.SRW.Utilities;
import gov.loc.www.zing.cql.xcql.BooleanType;
import gov.loc.www.zing.cql.xcql.OperandType;
import gov.loc.www.zing.cql.xcql.RelationType;
import gov.loc.www.zing.cql.xcql.SearchClauseType;
import gov.loc.www.zing.cql.xcql.TripleType;
import gov.loc.www.zing.srw.EchoedScanRequestType;
import gov.loc.www.zing.srw.EchoedSearchRetrieveRequestType;
import gov.loc.www.zing.srw.ScanRequestType;
import gov.loc.www.zing.srw.ScanResponseType;
import gov.loc.www.zing.srw.SearchRetrieveRequestType;
import gov.loc.www.zing.srw.SearchRetrieveResponseType;
import gov.loc.www.zing.srw.interfaces.SRWPort;
import java.io.IOException;
import java.rmi.RemoteException;

import ORG.oclc.os.SRW.SRWDatabase;
import ORG.oclc.os.SRW.SRWDiagnostic;
import org.apache.axis.MessageContext;
import org.apache.axis.types.NonNegativeInteger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.z3950.zing.cql.CQLAndNode;
import org.z3950.zing.cql.CQLBooleanNode;
import org.z3950.zing.cql.CQLNode;
import org.z3950.zing.cql.CQLNotNode;
import org.z3950.zing.cql.CQLOrNode;
import org.z3950.zing.cql.CQLParseException;
import org.z3950.zing.cql.CQLParser;
import org.z3950.zing.cql.CQLTermNode;

public class SRWSoapBindingImpl implements SRWPort {
    Log log=LogFactory.getLog(SRWSoapBindingImpl.class);

    public SearchRetrieveResponseType searchRetrieveOperation(
      SearchRetrieveRequestType request) throws RemoteException {
        log.debug("Enter: searchRetrieveOperation");
        long startTime=System.currentTimeMillis();
        MessageContext             msgContext=MessageContext.getCurrentContext();
        SearchRetrieveResponseType response;
        int resultSetIdleTime=((Integer)msgContext.getProperty("resultSetIdleTime")).intValue();
        NonNegativeInteger nni=request.getResultSetTTL();
        if(log.isDebugEnabled())
            log.debug("resultSetTTL()="+nni);
        if(nni!=null) {
            int ttl=nni.intValue();
            log.debug("ttl="+ttl);
            if(ttl<resultSetIdleTime)
                resultSetIdleTime=ttl;
        }
        String dbname=(String)msgContext.getProperty("dbname");
        SRWDatabase db=(SRWDatabase)msgContext.getProperty("db");
        if(log.isDebugEnabled())
            log.debug("db="+db);
        String query=request.getQuery();
        if(query.indexOf('%')>=0)
            try {
                request.setQuery(java.net.URLDecoder.decode(query, "utf-8"));
            }
            catch (java.io.UnsupportedEncodingException e) {
            }
        String sortKeys=request.getSortKeys();
        if(sortKeys!=null && sortKeys.indexOf('%')>=0)
            try {
                request.setSortKeys(java.net.URLDecoder.decode(sortKeys, "utf-8"));
            }
            catch (java.io.UnsupportedEncodingException e) {
            }
        if(query==null) {
            response=new SearchRetrieveResponseType();
            db.diagnostic(SRWDiagnostic.MandatoryParameterNotSupplied,
                "query", response);
        }
        else if(request.getStartRecord()!=null &&
          request.getStartRecord().intValue()==Integer.MAX_VALUE) {
            response=new SearchRetrieveResponseType();
            db.diagnostic(SRWDiagnostic.UnsupportedParameterValue,
                "startRecord", response);
        }
        else if(request.getMaximumRecords()!=null &&
          request.getMaximumRecords().intValue()==Integer.MAX_VALUE) {
            response=new SearchRetrieveResponseType();
            db.diagnostic(SRWDiagnostic.UnsupportedParameterValue,
                "maximumRecords", response);
        }
        else if(request.getResultSetTTL()!=null &&
          request.getResultSetTTL().intValue()==Integer.MAX_VALUE) {
            response=new SearchRetrieveResponseType();
            db.diagnostic(SRWDiagnostic.UnsupportedParameterValue,
                "resultSetTTL", response);
        }
        else
        try {
            response=db.doRequest(request);
            if(response==null) {
                response=new SearchRetrieveResponseType();
                response.setVersion("1.1");
                setEchoedSearchRetrieveRequestType(request, response);
                db.diagnostic(SRWDiagnostic.GeneralSystemError, null, response);
                return response;
            }
            if(msgContext.getProperty("sru")!=null &&
              request.getStylesheet()!=null) // you can't ask for a stylesheet in srw!
                db.diagnostic(SRWDiagnostic.StylesheetsNotSupported, null, response);

            setEchoedSearchRetrieveRequestType(request, response);
            if(request.getRecordXPath()!=null)
                db.diagnostic(72, null, response);
            if(request.getSortKeys()!=null &&
              !request.getSortKeys().equals("") && !db.supportsSort())
                db.diagnostic(80, null, response);
        }
        catch(Exception e) {
            log.error(e, e);
            throw new RemoteException(e.getMessage(), e);
        }
        finally {
            SRWDatabase.putDb(dbname, db);
        }

        response.setVersion("1.1");
        log.info("\""+query+"\"==>"+response.getNumberOfRecords()+" ("+(System.currentTimeMillis()-startTime)+"ms)");
        log.debug("Exit: searchRetrieveOperation");
        return response;
    }


    public ScanResponseType scanOperation(ScanRequestType request)
      throws java.rmi.RemoteException {
        log.debug("Enter: scanOperation");
        MessageContext   msgContext=MessageContext.getCurrentContext();
        ScanResponseType response;
        String dbname=(String)msgContext.getProperty("dbname");
        SRWDatabase db=(SRWDatabase)msgContext.getProperty("db");
        if(log.isDebugEnabled())
            log.debug("db="+db);
        if(request.getScanClause()==null) {
            response=new ScanResponseType();
            db.diagnostic(SRWDiagnostic.MandatoryParameterNotSupplied,
                "scanClause", response);
        }
        else if(request.getResponsePosition()!=null &&
          request.getResponsePosition().intValue()==Integer.MAX_VALUE) {
            response=new ScanResponseType();
            db.diagnostic(SRWDiagnostic.UnsupportedParameterValue,
                "responsePosition", response);
        }
        else if(request.getMaximumTerms()!=null &&
          request.getMaximumTerms().intValue()==Integer.MAX_VALUE) {
            response=new ScanResponseType();
            db.diagnostic(SRWDiagnostic.UnsupportedParameterValue,
                "maximumTerms", response);
        }
        else
            try {
                response=db.doRequest(request);
            }
            catch(Exception e) {
                log.error(e, e);
                throw new RemoteException(e.getMessage(), e);
            }
            finally {
                SRWDatabase.putDb(dbname, db);
            }
        if(response!=null) {
                log.info("calling setEchoedScanRequestType");
            setEchoedScanRequestType(request, response);
                log.info("called setEchoedScanRequestType");
            response.setVersion("1.1");
        }
        log.debug("Exit: scanOperation");
        return response;
    }


    public void setEchoedScanRequestType(ScanRequestType request, ScanResponseType response) {
        EchoedScanRequestType ert=new EchoedScanRequestType();
        if(request.getVersion()!=null)
            ert.setVersion(request.getVersion());
        else
            ert.setVersion("1.1");
        ert.setMaximumTerms(request.getMaximumTerms());
        ert.setResponsePosition(request.getResponsePosition());

        String scanClause=request.getScanClause();
        log.info("scanClause="+scanClause);
        if(scanClause!=null) {
            ert.setScanClause(scanClause);
            try {
                CQLTermNode node=Utilities.getFirstTerm(new CQLParser().parse(scanClause));
                if(node!=null) {
                    SearchClauseType sct=new SearchClauseType();
                    sct.setTerm(node.getTerm());
                    RelationType rt=new RelationType();
                    rt.setValue(node.getRelation().getBase());
                    sct.setRelation(rt);
                    sct.setIndex(node.getQualifier());
                    ert.setXScanClause(sct);
                }
                else {
                    SearchClauseType sct=new SearchClauseType();
                    sct.setTerm("Unrecognized node");
                    RelationType rt=new RelationType();
                    rt.setValue("");
                    sct.setRelation(rt);
                    sct.setIndex("");
                    ert.setXScanClause(sct);
                }
            }
            catch(java.io.IOException e) {
                log.error(e);
            }
            catch(org.z3950.zing.cql.CQLParseException e) {
                log.error(e);
                SearchClauseType sct=new SearchClauseType();
                sct.setTerm("CQLParseException");
                RelationType rt=new RelationType();
                rt.setValue("");
                sct.setRelation(rt);
                sct.setIndex("");
                ert.setXScanClause(sct);
            }
        }
        else {
            SearchClauseType sct=new SearchClauseType();
            sct.setTerm("");
            RelationType rt=new RelationType();
            rt.setValue("");
            sct.setRelation(rt);
            sct.setIndex("");
            ert.setXScanClause(sct);
        }


        response.setEchoedScanRequest(ert);
    }


    public void setEchoedSearchRetrieveRequestType(SearchRetrieveRequestType request, SearchRetrieveResponseType response) {
        EchoedSearchRetrieveRequestType ert=response.getEchoedSearchRetrieveRequest();
        if(ert==null) {
            ert=new EchoedSearchRetrieveRequestType();
        }
        ert.setMaximumRecords(request.getMaximumRecords());
        String query=request.getQuery();
        if(query!=null)
            ert.setQuery(query);
        try {
            CQLNode root=new CQLParser().parse(query);
            ert.setXQuery(toOperandType(root));
        }
        catch (CQLParseException e) {
            log.error(e,e);
        }
        catch (IOException e) {
            log.error(e,e);
        }
        ert.setResultSetTTL(  request.getResultSetTTL());
        ert.setRecordPacking( request.getRecordPacking());
        ert.setSortKeys(      request.getSortKeys());
        ert.setStartRecord(   request.getStartRecord());
        if(request.getVersion()!=null)
            ert.setVersion(request.getVersion());
        else
            ert.setVersion("1.1");
        if(request.getRecordSchema()!=null)
            ert.setRecordSchema(request.getRecordSchema());
        else
            ert.setRecordSchema("default");
        response.setEchoedSearchRetrieveRequest(ert);
    }

    private OperandType toOperandType(CQLNode node) {
        OperandType ot=new OperandType();
        if(node instanceof CQLBooleanNode) {
            CQLBooleanNode cbn=(CQLBooleanNode)node;
            TripleType tt=new TripleType();
            if(cbn instanceof CQLAndNode)
                tt.set_boolean(new BooleanType("and", null));
            else if(cbn instanceof CQLOrNode)
                tt.set_boolean(new BooleanType("or", null));
            else if(cbn instanceof CQLNotNode)
                tt.set_boolean(new BooleanType("not", null));
            else tt.set_boolean(new BooleanType("prox", null));

            tt.setLeftOperand(toOperandType(cbn.left));
            tt.setRightOperand(toOperandType(cbn.right));
            ot.setTriple(tt);
        }
        else if(node instanceof CQLTermNode) {
            CQLTermNode ctn=(CQLTermNode)node;
            SearchClauseType sct=new SearchClauseType();
            sct.setIndex(ctn.getQualifier());
            RelationType rt=new RelationType();
            rt.setValue(ctn.getRelation().getBase());
            sct.setRelation(rt);
            sct.setTerm(ctn.getTerm());
            ot.setSearchClause(sct);
        }
        else {
            log.error("Found a node on the parse tree of type: "+node);
        }
        return ot;
    }
}


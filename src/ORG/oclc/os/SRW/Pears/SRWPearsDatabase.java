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
 * SRWPearsDatabase.java
 *
 * Created on August 5, 2003, 4:17 PM
 */

package ORG.oclc.os.SRW.Pears;

import gov.loc.www.zing.srw.ScanRequestType;
import gov.loc.www.zing.srw.ScanResponseType;
import gov.loc.www.zing.srw.SearchRetrieveRequestType;
import gov.loc.www.zing.srw.TermType;
import gov.loc.www.zing.srw.TermsType;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.servlet.ServletException;
import org.apache.axis.types.NonNegativeInteger;
import org.apache.axis.types.PositiveInteger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.z3950.zing.cql.CQLNode;
import org.z3950.zing.cql.CQLParseException;
import org.z3950.zing.cql.CQLTermNode;
import org.z3950.zing.cql.PQFTranslationException;
import ORG.oclc.Newton.db.RestrictorSummary;
import ORG.oclc.Newton.db.RestrictorSummaryEntry;
import ORG.oclc.os.gwen.Database;
import ORG.oclc.os.gwen.Result;
import ORG.oclc.os.gwen.Term;
import ORG.oclc.os.SRW.DataPair;
import ORG.oclc.os.SRW.QueryResult;
import ORG.oclc.os.SRW.SRWDatabaseImpl;
import ORG.oclc.os.SRW.SRWDiagnostic;
import ORG.oclc.os.SRW.Utilities;
import ORG.oclc.ber.BerString;
import ORG.oclc.ber.DataDir;
import ORG.oclc.RecordHandler.HandleSGML;
import ORG.oclc.RecordHandler.RecordHandler;
import ORG.oclc.RecordHandler.RecordRenderer;
import ORG.oclc.z39.Diagnostic1;
import org.z3950.zing.cql.UnknownQualifierException;

/**
 *
 * @author  levan
 */
public class SRWPearsDatabase extends SRWDatabaseImpl {
    static Log log=LogFactory.getLog(SRWPearsDatabase.class);
    Database  pdb;
    Hashtable recordHandlers=new Hashtable(), renderers=new Hashtable(),
              renderXER=new Hashtable(), sortTools=new Hashtable(),
              useToIndexMap=new Hashtable();

    public void addRenderer(String schemaName, String schemaID,
      Properties props) throws InstantiationException {
        boolean madeRenderer=false;
        log.info("schemaName="+schemaName+", schemaID="+schemaID);
        String tagsFileName=props.getProperty(schemaName+".tagsFile");
        if(tagsFileName!=null) {
            log.debug("looking for tagsFile="+tagsFileName);
            File f=Utilities.findFile(tagsFileName, dbHome, srwHome);
            if(f==null) {
                throw new InstantiationException("unable to find .tags file '"+
                    tagsFileName+"' for schema "+schemaName);
            }
            try {
                RecordHandler rh=RecordHandler.getHandler("SGML");
                ((HandleSGML)rh).setCharToByteConverter("UTF8");
                ((HandleSGML)rh).Input((String)null, f.getAbsolutePath());
                recordHandlers.put(schemaName, rh);
                if(schemaID!=null)
                    recordHandlers.put(schemaID, rh);
                madeRenderer=true;
            }
            catch(Exception e) {
                log.error(e,e);
                throw new InstantiationException(e.getMessage());
            }
        }

        String renderer=props.getProperty(schemaName+".renderer");
        if(renderer==null) { // maybe it's there using the old notation
            String s=props.getProperty(schemaName);
            if(s!=null && s.startsWith("Renderer=")) {
                log.info("schemaName=Renderer=<rendererClass> notation deprecated");
                log.error("use schemaName.renderer=<rendererClass> instead");
                renderer=s.substring(9);
                log.info("old style renderer="+renderer);
            }
        }

        if(renderer!=null) {
            try {
                RecordRenderer rr=RecordRenderer.getRenderer(renderer, schemaName, schemaID, props);
                renderers.put(schemaName, rr);
                if(schemaID!=null)
                    renderers.put(schemaID, rr);
                madeRenderer=true;
            }
            catch(Exception e) {
                log.error(e,e);
                throw new InstantiationException(e.getMessage());
            }
        }

        String nativeRendering=props.getProperty(schemaName+".nativeRendering");
        if(nativeRendering!=null) {
            if(nativeRendering.equalsIgnoreCase("xer") ||
              nativeRendering.equalsIgnoreCase("ber")) {
                renderXER.put(schemaName, schemaName);
                if(schemaID!=null)
                    renderXER.put(schemaID, schemaID);
                madeRenderer=true;
            }
            else
                throw new InstantiationException(
                    "unrecognized nativeRendering: "+nativeRendering);
        }

        if(!madeRenderer) {
            log.info("rendering schema \""+schemaName+" as XER");
            renderXER.put(schemaName, schemaName);
            if(schemaID!=null)
                renderXER.put(schemaID, schemaID);
        }
    }


    public ScanResponseType doRequest(
      ScanRequestType request) throws ServletException {
        CQLTermNode root=null;
        int maxTerms=9, position=5;
        long startTime=System.currentTimeMillis();
        ScanResponseType response=new ScanResponseType();
        try {
            PositiveInteger pi=request.getMaximumTerms();
            if(pi!=null) {
                maxTerms=pi.intValue();
                position=maxTerms/2+1;
            }
            NonNegativeInteger nni=request.getResponsePosition();
            if(nni!=null)
                position=nni.intValue();
            String scanTerm=request.getScanClause();
            try{
                if(scanTerm!=null)
                    log.info("scanTerm:\n"+ORG.oclc.util.Util.byteArrayToString(scanTerm.getBytes("UTF-8")));
            }catch(Exception e){}
            log.info("maxTerms="+maxTerms+", position="+position);
            root = Utilities.getFirstTerm(parser.parse(scanTerm));
            if(root.getTerm().length()==0) // they sent us an empty term!
                root=new CQLTermNode(root.getQualifier(), root.getRelation(), "$");
            String resultSetID=root.getResultSetName();
            if(resultSetID!=null) { // you can't scan on resultSetId!
                return diagnostic(SRWDiagnostic.UnsupportedIndex,
                        "cql.resultSetId", response);
            }
            byte[] q=CQLNode.makeQuery(root, dbProperties);
            DataDir dd=new DataDir(new BerString(q));
            DataDir apt=dd.child().next().child();
            Term[] gwenTerm=pdb.getTermList(apt, maxTerms, position);
            log.info("number of terms returned by gwen="+gwenTerm.length);
            TermsType terms=new TermsType();
            TermType  term[]=new TermType[gwenTerm.length];
            for(int i=0; i<gwenTerm.length; i++) {
                term[i]=new TermType();
                term[i].setValue(Utilities.hex07Encode(gwenTerm[i].getTerm()));
                term[i].setNumberOfRecords(new NonNegativeInteger(Integer.toString(gwenTerm[i].getPostings())));
                if(log.isDebugEnabled())
                    log.debug("term["+i+"]="+ORG.oclc.util.Util.byteArrayToString(gwenTerm[i].getTerm().getBytes("UTF-8")));
            }
            terms.setTerm(term);
            response.setTerms(terms);
            log.info("scan "+scanTerm+": ("+(System.currentTimeMillis()-startTime)+"ms)");
            return response;
        }
        catch(ORG.oclc.z39.Diagnostic1 t) {
            if(t.condition()==Diagnostic1.unsupportedAttributeCombination) {
                log.error(t);
                CQLTermNode node=(CQLTermNode)root;
                log.info("returning UnsupportedCombinationOfRelationAndIndex diagnostic");
                return diagnostic(SRWDiagnostic.UnsupportedCombinationOfRelationAndIndex,
                    node.getQualifier()+" "+node.getRelation().getBase(), response);
            }

            log.error(t);
            log.info("returning GeneralSystemError diagnostic");
            return diagnostic(SRWDiagnostic.GeneralSystemError,
                "z39.50 diagnostic code: "+t.condition()+", addinfo="+
                t.addinfo()+" z39.50 message="+t.msg(t.condition()), response);
        }
        catch(org.z3950.zing.cql.UnknownQualifierException e) {
            log.error(e);
            return diagnostic(SRWDiagnostic.UnsupportedIndex, e.getMessage(),
                response);
        }
        catch(IllegalArgumentException e) {
            if(e.getMessage().equals("Illegal positionInResponse"))
                return diagnostic(SRWDiagnostic.ResponsePositionOutOfRange,
                    Integer.toString(position), response);
            if(e.getMessage().equals("Not AttributesPlusTerm"))
                return diagnostic(SRWDiagnostic.QuerySyntaxError, null, response);
            log.error(e, e);
            return diagnostic(SRWDiagnostic.GeneralSystemError, e.getMessage(), response);
        }
        catch(Exception e) {
            log.error(e, e);
            return diagnostic(SRWDiagnostic.GeneralSystemError, e.getMessage(), response);
        }
    }

    
    public String getExtraResponseData(QueryResult result, SearchRetrieveRequestType request) {
        PearsQueryResult pqr=(PearsQueryResult)result;
        String response=null;
        if(pqr.isRestrictorSummary()) {
            DataPair dp;
            int use=0;
            RestrictorSummary rs=pqr.getRestrictorSummary();
            RestrictorSummaryEntry entry;
            String index;
            Vector indexes=new Vector(), restrictor=null, restrictors=new Vector();
            while((entry=rs.nextRestrictorEntry())!=null) {
                if(entry.getUse()!=use) {
                    use=entry.getUse();
                    index=(String)useToIndexMap.get(Integer.toString(use));
                    restrictor=new Vector();
                    restrictors.add(restrictor);
                    indexes.add(index);
                }
                dp=new DataPair(entry.getName(), entry.getCount());
                restrictor.add(dp);
            }
            StringBuffer sb=new StringBuffer();
            sb.append("<restrictorSummary ")
              .append("xmlns=\"info:srw/extension/5/restrictorSummary\"")
              .append(" count=\"").append(restrictors.size()).append("\">");
            for(int i=0; i<restrictors.size(); i++) {
                restrictor=(Vector)restrictors.get(i);
                Collections.sort(restrictor, Collections.reverseOrder());
                sb.append("<restrictor index=\"")
                  .append((String)indexes.get(i))
                  .append("\" count=\"").append(restrictor.size())
                  .append("\">");
                for(int j=0; j<restrictor.size(); j++) {
                    dp=(DataPair)restrictor.get(j);
                    sb.append("<entry count=\"").append(dp.getCount()).append("\">");
                    sb.append(dp.getName()).append("</entry>");
                }
                sb.append("</restrictor>\n");
            }
            sb.append("</restrictorSummary>");
            response=sb.toString();
            if(log.isDebugEnabled())
                log.debug("ExtraResponseData:\n"+response);
        }
        return response;
    }

    public QueryResult getQueryResult(String query, SearchRetrieveRequestType request) {
        log.debug("enter");
        long startTime=System.currentTimeMillis();
        boolean rankByHoldings=false, restrictorSummary=false, returnSortKeys=false;
        Hashtable extraRequestDataElements=parseElements(request.getExtraRequestData());
        String s=(String)extraRequestDataElements.get("restrictorSummary");
        if(s!=null && !s.equals("false")) {
                restrictorSummary=true;
                log.info("turned restrictorSummary on");
            }
        s=(String)extraRequestDataElements.get("returnSortKeys");
        if(s!=null && !s.equals("false")) {
                returnSortKeys=true;
                log.info("turned returnSortKeys on");
            }
        String sortKeys=request.getSortKeys();
        if(sortKeys!=null && sortKeys.equals("holdingscount")) {
            sortKeys=null;
            rankByHoldings=true;
        }
        CQLNode root;
        try {
            root=parser.parse(Utilities.escapeBackslash(query));
        }
        catch(CQLParseException e) {
            log.error(e);
            PearsQueryResult pqr=new PearsQueryResult(this);
            pqr.addDiagnostic(SRWDiagnostic.QuerySyntaxError, e.getMessage());
            return pqr;
        }
        catch(IOException e) {
            log.error(e);
            PearsQueryResult pqr=new PearsQueryResult(this);
            pqr.addDiagnostic(SRWDiagnostic.GeneralSystemError, e.getMessage());
            return pqr;
        }
        byte[] q;
        try {
            q=CQLNode.makeQuery(root, dbProperties);
        }
        catch(UnknownQualifierException e) {
            log.error(e);
            PearsQueryResult pqr=new PearsQueryResult(this);
            pqr.addDiagnostic(SRWDiagnostic.UnsupportedIndex, e.getMessage());
            return pqr;
        }
        catch(PQFTranslationException e) {
            log.error(e);
            PearsQueryResult pqr=new PearsQueryResult(this);
            pqr.addDiagnostic(SRWDiagnostic.GeneralSystemError, e.getMessage());
            return pqr;
        }
        DataDir queryDir=new DataDir(new BerString(q));
        if(log.isDebugEnabled())
            log.debug("z39.50 query:\n"+queryDir);

        // Evaluate the query.
        Result result;
        try {
            if(rankByHoldings)
                result = pdb.getResult(queryDir, "Restrictor", restrictorSummary);
            else
                result = pdb.getResult(queryDir, oldResultSets, restrictorSummary);
        }
        catch(Diagnostic1 e) {
            PearsQueryResult pqr=new PearsQueryResult(this);
            switch(e.condition()) {
                case Diagnostic1.malformedSearchTerm:
                    pqr.addDiagnostic(SRWDiagnostic.QuerySyntaxError, e.addinfo());
                    break;
                case Diagnostic1.unsupportedAttributeCombination:
                    String msg=e.addinfo();
                    String use=msg.substring(msg.indexOf("use=")+4, msg.indexOf(','));
                    String index=(String)useToIndexMap.get(use);
                    int structureOffset=msg.indexOf("structure=");
                    String structure=msg.substring(structureOffset+10, structureOffset+11);
                    String relation;
                    if(structure.equals("1"))
                        relation="exact";
                    else if(structure.equals("2"))
                        relation="=";
                    else
                        relation="unknown (structure attribute="+structure+")";
                    pqr.addDiagnostic(SRWDiagnostic.UnsupportedCombinationOfRelationAndIndex, index+" "+relation);
                    break;
                case Diagnostic1.tooManyTruncatedWords:
                    pqr.addDiagnostic(SRWDiagnostic.TooManyTermsMatchedByMaskedQueryTerm, e.addinfo());
                    break;
                case Diagnostic1.truncatedWordsTooShort:
                    pqr.addDiagnostic(SRWDiagnostic.MaskedWordsTooShort, e.addinfo());
                    break;
                default:
                    log.error(e);
                    pqr.addDiagnostic(SRWDiagnostic.GeneralSystemError, e.getMessage());
            }
            return pqr;
        }
        catch(IllegalStateException e) {
            PearsQueryResult pqr=new PearsQueryResult(this);
            pqr.addDiagnostic(SRWDiagnostic.GeneralSystemError, e.getMessage());
            return pqr;
        }
        log.debug("at exit");
        PearsQueryResult pqr=new PearsQueryResult(this, result, query, extraRequestDataElements);
        log.info("search "+query+": ("+(System.currentTimeMillis()-startTime)+"ms)");
        return pqr;
    }

//    public SearchRetrieveResponseType doRequest(
//      SearchRetrieveRequestType request) throws ServletException {
//        SearchRetrieveResponseType response=new SearchRetrieveResponseType();
//
//
//                if(sortKeys!=null && startRec!=null && startRec.intValue()>1)
//                    return diagnostic(
//                        SRWDiagnostics.SortUnsupportedWhenStartRecordNotOneAndQueryNotAResultSetId,
//                        null, response);
//            int postings=result.getPostings();
//            
//            boolean       ascending=true;
//            PearsSortTool sortTool=null;
//            String        schemaName=request.getRecordSchema(), sortKey;
//            if(schemaName==null)
//                schemaName="default";
//            log.info("recordSchema="+schemaName);
//            Object handler=transformers.get(schemaName);
//            if(handler==null) {
//                log.error("no handler for schema "+schemaName);
//                if(log.isInfoEnabled()) {
//                    for(Enumeration enum2=transformers.keys();
//                      enum2.hasMoreElements();)
//                        log.info("handler name="+(String)enum2.nextElement());
//                }
//                return diagnostic(SRWDiagnostics.UnknownSchemaForRetrieval,
//                    schemaName, response);
//            }
//            SortEntry entries[]=null;
//            if(postings>0 && sortKeys!=null && sortKeys.length()>0) {
//                entries=(SortEntry[])sortedResultSets.get(resultSetID+"/"+sortKeys);
//                sortTool=(PearsSortTool)sortTools.get(sortKeys);
//                if(entries==null) { // need to sort the resultSet
//                    log.info("sorting resultSet");
//                    StringTokenizer keysTokenizer=new StringTokenizer(sortKeys);
//                    //while(keysTokenizer.hasMoreTokens()) {
//                    // just one key for now
//                        sortKey=keysTokenizer.nextToken();
//                        sortTool=new PearsSortTool(sortKey, transformers);
//                    //}
//                    String sortSchema=(String)nameSpaces.get(sortTool.prefix);
//                    if(sortSchema==null)
//                        sortSchema="";
//                    sortTool.setSchema(sortSchema);
//                    sortTool.makeSortElementExtractor();
////                    Object sortHandler=null;
////                    if(!sortTool.prefix.equalsIgnoreCase("xer")) {
////                        transformers.get(sortTool.prefix);
////                        if(sortHandler==null) {
////                            log.error("no handler for schema "+sortTool.prefix);
////                            if(log.isInfoEnabled()) {
////                                for(Enumeration enum2=transformers.keys();
////                                  enum2.hasMoreElements();)
////                                    log.info("handler name="+(String)enum2.nextElement());
////                            }
////                            return diagnostic(SRWDiagnostics.UnsupportedSchemaForSort,
////                                sortTool.prefix, response);
////                        }
////                    }
//                    BerString        doc;
//                    DataDir          recDir;
//                    DocumentIterator list=(DocumentIterator)result.getDocumentIdList();
//                    int              listEntry;
//                    String           stringRecord;
//                    entries=new SortEntry[postings];
//                    for(int i=0; i<postings; i++) {
//                        listEntry=list.nextInt();
//                        log.debug("listEntry="+listEntry);
//                        doc=(BerString)pdb.getDocument(listEntry);
//                        recDir=new DataDir(doc);
//                        if(sortTool.dataType.equals("text"))
//                            entries[i]=new SortEntry(sortTool.extract(recDir), listEntry);
//                        else {
//                            try {
//                                entries[i]=new SortEntry(Integer.parseInt(sortTool.extract(recDir)), listEntry);
//                            }
//                            catch(java.lang.NumberFormatException e) {
//                                entries[i]=new SortEntry(0, listEntry);
//                            }
//                        }
//                        if(entries[i].getKey()==null) { // missing value code
//                            if(sortTool.missingValue.equals("abort"))
//                                return diagnostic(SRWDiagnostics.SortEndedDueToMissingValue,
//                                    null, response);
//                            else if(sortTool.missingValue.equals("highValue"))
//                                entries[i]=new SortEntry("\ufffffe\ufffffe\ufffffe\ufffffe\ufffffe", listEntry);
//                            else if(sortTool.missingValue.equals("lowValue"))
//                                entries[i]=new SortEntry("\u000000", listEntry);
//                            else { // omit
//                                i--;
//                                postings--;
//                            }
//                        }
//                        if(log.isDebugEnabled())
//                            log.debug("entries["+i+"]="+entries[i]);
//                    }
//                    Arrays.sort(entries);
//                    sortedResultSets.put(resultSetID+"/"+sortKeys, entries);
//                    sortTools.put(sortKeys, sortTool);
//                }
//                else {
//                    log.info("reusing old sorted resultSet");
//                }
//            }
//            
//            int numRecs=defaultNumRecs;
//            NonNegativeInteger maxRecs=request.getMaximumRecords();
//            if(maxRecs!=null)
//                numRecs=(int)java.lang.Math.min(maxRecs.longValue(), maximumRecords);
//            
//            int startPoint=1;
//            if(startRec!=null)
//                startPoint=(int)startRec.longValue();
//            if(postings>0 && startPoint>postings)
//                diagnostic(SRWDiagnostics.FirstRecordPositionOutOfRange,
//                        null, response);
//                
//            if((startPoint-1+numRecs)>postings)
//                numRecs=postings-(startPoint-1);
//            if(postings>0 && numRecs==0)
//                response.setNextRecordPosition(new PositiveInteger("1"));
//            if(postings>0 && numRecs>0) { // render some records into SGML
//                RecordsType records=new RecordsType();
//                log.info("trying to get "+numRecs+
//                    " records starting with record "+startPoint+
//                    " from a set of "+postings+" records");
//                if(!recordPacking.equals("xml") &&
//                  !recordPacking.equals("string")) {
//                    return diagnostic(71, recordPacking, response);
//                }
//                if(entries==null)
//                DocumentIterator list=null;
//                    try {
//                        if (startPoint == 1)
//                            list = (DocumentIterator)result.getDocumentIdList();
//                        else
//                            list = (DocumentIterator)result.getDocumentIdList(startPoint);
//                    } catch (Diagnostic1 d) {
//                        log.error("Can't get list " + d);
//                        //throw new RemoteException("Can't retrieve records", d);
//                        throw new ServletException("Can't retrieve records");
//                    }
//                
//                records.setRecord(new RecordType[numRecs]);
//                DataDir                recDir;
//                Document               domDoc;
//                DocumentBuilder        db=null;
//                DocumentBuilderFactory dbf=null;
//                int                    i, listEntry=-1;
//                MessageElement         elems[];
//                RecordType             record;
//                String                 recStr, schemaID=(String)schemas.get(schemaName);
//                StringOrXmlFragment    frag;
//                if(recordPacking.equals("xml")) {
//                    dbf=DocumentBuilderFactory.newInstance();
//                    dbf.setNamespaceAware(true);
//                    db=dbf.newDocumentBuilder();
//                }
//                
//                /**
//                 * One at a time, retrieve and display the requested documents.
//                 */
//                for(i=0; i<numRecs; i++) {
//                    try {
//                        if(entries!=null) {
//                            log.info("startPoint="+startPoint+", i="+i+", entries.length="+entries.length);
//                            if(sortTool.ascending) {
//                                //log.info("getting ascending record "+(startRec.intValue()+i-1));
//                                listEntry=entries[startPoint+i-1].getNumber();
//                            }
//                            else {
//                                //log.info("getting descending record "+(entries.length-(startRec.intValue()+i)));
//                                listEntry=entries[entries.length-(startPoint+i)].getNumber();
//                            }
//                        }
//                        else {
//                            listEntry=list.nextInt();
//                            //log.info("walking unsorted list, listEntry="+listEntry);
//                        }
//
//                        record=new RecordType();
//                        record.setRecordPacking(recordPacking);
//                        frag=new StringOrXmlFragment();
//                        elems=new MessageElement[1];
//                        frag.set_any(elems);
//                        recDir=new DataDir((BerString)pdb.getDocument(listEntry));
//                        recStr=makeRecord(recDir, handler, schemaName);
//                        if(recordPacking.equals("xml")) {
//                            domDoc=db.parse(new InputSource(new StringReader(recStr)));
//                            Element el=domDoc.getDocumentElement();
//                            log.debug("got the DocumentElement");
//                            elems[0]=new MessageElement(el);
//                            log.debug("put the domDoc into elems[0]");
//                        }
//                        else { // string
//                            Text t=new Text(recStr);
//                            elems[0]=new MessageElement(t);
//                        }
//                        record.setRecordData(frag);
//                        log.debug("setRecordData");
//
//                        if(schemaID!=null)
//                            record.setRecordSchema(schemaID);
//                        else
//                            record.setRecordSchema(schemaName);
//                        record.setRecordPosition(new PositiveInteger(
//                            Integer.toString(startPoint+i)));
//                        
//                        records.setRecord(i, record);
//                    } catch (NoSuchElementException e) {
//                        log.error("Read beyond the end of list!!");
//                        log.error(e);
//                        break;
//                    } catch (Diagnostic1 d) {
//                        log.error("error reading list " + list);
//                        log.error(d);
//                    } catch (MalformedRecordException m) {
//                        log.error("error reading document "+listEntry+
//                        " from list "+list);
//                        log.error(m);
//                    } catch (javax.xml.transform.TransformerException t) {
//                        log.error("error reading document "+listEntry+
//                        " from list "+list);
//                        log.error(t);
//                    }
//
//                    response.setRecords(records);
//                }
//                if(startPoint+i<=postings)
//                    response.setNextRecordPosition(new PositiveInteger(
//                        Long.toString(startPoint+i)));
//            }
//            log.debug("exit doRequest");
//            return response;
//        }
//        catch(Diagnostic1 t) {
//            log.error(t);
//            if(t.condition()==Diagnostic1.tooManyTruncatedWords)
//                return diagnostic(
//                    SRWDiagnostics.TooManyTermsMatchedByMaskedQueryTerm,
//                    t.addinfo(), response);
//            if(t.condition()==Diagnostic1.unsupportedAttributeCombination) {
//                String addinfo=t.addinfo();
//                int i=addinfo.indexOf(',');
//                if(i>=0) {
//                    addinfo="index="+(String)useToIndexMap.get(addinfo.substring(4, i));
//                    if(addinfo.indexOf("structure=1")>=0)
//                        addinfo=addinfo+", relation=exact";
//                }
//                return diagnostic(
//                    SRWDiagnostics.UnsupportedCombinationOfRelationAndIndex,
//                    addinfo, response);
//            }
//            return diagnostic(SRWDiagnostics.GeneralSystemError,
//                "z39.50 diagnostic code: "+t.condition()+", addinfo="+
//                t.addinfo()+" z39.50 message="+t.msg(t.condition()), response);
//        }
//        catch(org.z3950.zing.cql.CQLParseException e) {
//            log.error(e);
//            return diagnostic(SRWDiagnostics.QuerySyntaxError, e.getMessage(),
//                response);
//        }
//        catch(org.z3950.zing.cql.UnknownQualifierException e) {
//            log.error(e);
//            return diagnostic(SRWDiagnostics.UnsupportedIndex, e.getMessage(),
//                response);
//        }
//        catch(Exception e) {
//            //log.error(e);
//            log.error(e, e);
//            throw new ServletException(e.getMessage());
//        }
//    }


    public String getIndexInfo() {
        return getIndexInfo(dbProperties);
    }

    public static String getIndexInfo(Properties dbProperties) {
        Enumeration     enumer=dbProperties.propertyNames();
        Hashtable       sets=new Hashtable();
        String          index, indexSet, prop;
        StringBuffer    sb=new StringBuffer("        <indexInfo>\n");
        StringTokenizer st;
        while(enumer.hasMoreElements()) {
            prop=(String)enumer.nextElement();
            if(prop.startsWith("qualifier.")) {
                st=new StringTokenizer(prop.substring(10));
                index=st.nextToken();
                st=new StringTokenizer(index, ".");
                if(st.countTokens()==1)
                    indexSet="local";
                else
                    indexSet=st.nextToken();
                index=st.nextToken();
                if(sets.get(indexSet)==null) {  // new set
                    sb.append("          <set identifier=\"")
                      .append(dbProperties.getProperty("contextSet."+indexSet))
                      .append("\" name=\"").append(indexSet).append("\"/>\n");
                    sets.put(indexSet, indexSet);
                }
                sb.append("          <index>\n")
                  .append("            <title>").append(indexSet).append('.').append(index).append("</title>\n")
                  .append("            <map>\n")
                  .append("              <name set=\"").append(indexSet).append("\">").append(index).append("</name>\n")
                  .append("              </map>\n")
                  .append("            </index>\n");
            }
        }
        sb.append("          </indexInfo>\n");
        return sb.toString();
    }


    public void init(final String dbname, String srwHome, String dbHome,
      String dbPropertiesFileName, Properties dbProperties) {
        log.debug("entering SRWPearsDatabase.init, dbname="+dbname);
        log.debug("srwHome="+srwHome+", dbHome="+dbHome+
            ", dbPropertiesFileName="+dbPropertiesFileName+
            ", dbProperties="+dbProperties);
        initDB(dbname, srwHome, dbHome, dbPropertiesFileName, dbProperties);
        
        // get the pearsgwen properties for this database and
        // put them into a properties object of their own.
        Enumeration enumer=dbProperties.propertyNames();
        Properties gwenProperties=new Properties();
        String name, use, value;
        StringTokenizer st;
        while(enumer.hasMoreElements()) {
            name=(String)enumer.nextElement();
            if(name.startsWith("pearsgwen.")) {
                value=dbProperties.getProperty(name);
                if(name.equals("pearsgwen.inifileName")) {
                    if(!value.startsWith("/") && !value.startsWith(".") &&
                      dbHome!=null)
                        value=dbHome+value;
                }
                gwenProperties.put(name, value);
            }
            else if(name.startsWith("qualifier.")) {
                value=dbProperties.getProperty(name);
                if(value.startsWith("1=")) {
                    st=new StringTokenizer(value, "= \t");
                    st.nextToken();
                    use=st.nextToken();
                    useToIndexMap.put(use, name.substring(10));
                    if(log.isDebugEnabled())
                        log.debug("use="+use+", index="+name.substring(10));
                }
            }
        }
        
        String dbType=dbProperties.getProperty("Gwen.implementation");
        if (dbType==null) {
            log.error("Gwen.implementation not specified");
            log.error(".props filename is " + dbPropertiesFileName);
            return;
        }
        log.debug("dbType="+dbType);
        Constructor con;
        Runtime rt=Runtime.getRuntime();
        try {
            Class c = Class.forName(dbType);
            con = c.getConstructor(
            new Class[] {java.util.Properties.class, int.class});
            
            /**
             * Create the database instance. READONLY update mode was
             * chosen because this demo doesn't attempt to update the
             * database.
             */
            log.debug("Creating Database: "+dbname);
            log.info("before creating pdb: totalMemory="+rt.totalMemory()+", freeMemory="+rt.freeMemory());
            pdb = (Database)con.newInstance(
                new Object[] {gwenProperties, new Integer(Database.READONLY)});
            log.info("after creating pdb: totalMemory="+rt.totalMemory()+", freeMemory="+rt.freeMemory());
        }
        catch(java.lang.reflect.InvocationTargetException e) {
            log.info("error creating pdb: totalMemory="+rt.totalMemory()+", freeMemory="+rt.freeMemory());
            log.error("Exception creating database of type "+dbType+
                " for database "+dbname);
            Throwable t=e.getTargetException();
            log.error("Base Exception: "+e.getTargetException());
            log.error(t, t);
            return;
        }
        catch(Exception e) {
            log.error("Exception creating database of type "+dbType+
                " for database "+dbname);
            log.error(e, e);
            return;
        }
        
        log.debug("leaving SRWPearsDatabase.init");
        return;
    }


    public boolean supportsSort() {
        return true;
    }
}

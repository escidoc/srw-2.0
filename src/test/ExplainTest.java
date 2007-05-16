package test;

import gov.loc.www.zing.srw.ExplainRequestType;
import gov.loc.www.zing.srw.ExplainResponseType;
import gov.loc.www.zing.srw.interfaces.ExplainPort;
import gov.loc.www.zing.srw.interfaces.SRWPort;
import gov.loc.www.zing.srw.service.SRWSampleServiceLocator;

import java.net.URL;

public class ExplainTest {

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            SRWSampleServiceLocator service=new SRWSampleServiceLocator();
            URL url=new URL(
                  "http://localhost:8084/SRW/search/test");
            SRWPort srwService = service.getSRW(url);
            ExplainPort explainService = service.getExplainSOAP(url);
            ExplainRequestType request = new ExplainRequestType();
            request.setVersion("1.1");
            ExplainResponseType response = explainService.explainOperation(request);
            System.out.println(response.getVersion());
        } catch (Exception e) {
            System.out.println(e);
        }
    }

}

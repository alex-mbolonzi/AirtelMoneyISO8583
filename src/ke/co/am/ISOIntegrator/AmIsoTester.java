/*
    Author: Alex Mbolonzi

 */
package ke.co.am.ISOIntegrator;

import java.io.IOException;
import java.util.StringTokenizer;

import org.jpos.iso.ISOException;

import ke.co.am.ISOIntegrator.AmCardToAM;
import ke.co.am.ISOIntegrator.AmEcho;
import ke.co.am.ISOIntegrator.AmKyc;
//import ke.co.ars.dao.AMB2CDAO;
//import ke.co.ars.dao.MobileSubscriberDAO;
import ke.co.ars.dao.NodeDAO;
import ke.co.ars.entity.Hits;
import ke.co.ars.entity.TrxRequest;
import ke.co.ars.entity.TrxResponse;
import ke.co.ars.entity.MobileSubscriber;
import ke.co.ars.entity.Node;
import ke.co.ars.entity.Transfer;


public class AmIsoTester {

    public void echoTest(String hostName) {

        TrxResponse responseMsg = new TrxResponse();

        AmEcho echo = new AmEcho();

        String serverName = hostName;

        try {

            responseMsg = echo.EchoRequest(serverName);

            System.out.println("Echo status code : " + responseMsg.getStatusCode());

            System.out.println("Echo status description : " + responseMsg.getStatusDescription());

        } catch (ISOException | IOException e) {
            // TODO Auto-generated catch block
            responseMsg.setStatusCode(30);
            
            responseMsg.setStatusDescription("ERROR: Unable to parse ISO message");
            
            e.printStackTrace();
        }

    }

    public void kycTest() {

        String serverName = "SRVATKE01";

        Node nodeDetails = new Node();

        NodeDAO nodeDAO = new NodeDAO();

        nodeDetails = nodeDAO.getServerDetails(serverName);

        TrxRequest isoRequest = new TrxRequest();

        TrxResponse responseMsg = new TrxResponse();

        AmKyc kyc = new AmKyc();

        String msisdn = "254788542196";

        String AMOUNT = "0";

        int nodeStatus = nodeDetails.getStatusCode();

        if (nodeStatus == 0) {

            String serverIP = nodeDetails.getNodeIP();

            int serverPort = nodeDetails.getNodePort();

            int timeout = nodeDetails.getTimeout();

            isoRequest.setMsisdn(msisdn);
            isoRequest.setAmount(AMOUNT);
            isoRequest.setTimeout(timeout);
            isoRequest.setISOServerIP(serverIP);
            isoRequest.setISOServerPort(serverPort);

            try {
                responseMsg = kyc.KycRequest(isoRequest);

                System.out.println("Kyc status code : " + responseMsg.getStatusCode());

                System.out.println("Kyc status description : " + responseMsg.getStatusDescription());

                System.out.println("Kyc additional data : " + responseMsg.getTransactionData());

                if (responseMsg.getStatusCode() == 0) {

                    StringTokenizer stringTokenizer = new StringTokenizer(responseMsg.getTransactionData(), ",");

                    String firstName = stringTokenizer.nextElement().toString().trim();

                    String secondName = stringTokenizer.nextElement().toString().trim();

                    String lastName = stringTokenizer.nextElement().toString().trim();

                    firstName = firstName.substring(firstName.indexOf(":") + 1, firstName.length());

                    secondName = secondName.substring(secondName.indexOf(":") + 1, secondName.length());

                    lastName = lastName.substring(lastName.indexOf(":") + 1, lastName.length());

                    String subName = firstName + " " + secondName + " " + lastName;

                    MobileSubscriber subscriber = new MobileSubscriber();

                    subscriber.setMobileNumber(msisdn);
                    subscriber.setMNO("ATKE");
                    subscriber.setCountryCode("254");
                    subscriber.setName(subName);
                    subscriber.setStatus(0);

//                    MobileSubscriberDAO subscriberDAO = new MobileSubscriberDAO();

//                    subscriberDAO.addMobileSubscriber(subscriber);

                }
                
            } catch (ISOException | IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                
                responseMsg.setStatusCode(96);
                
                responseMsg.setStatusDescription("ERROR: Unable to parse ISO message");
                
            }

        } else {

            responseMsg.setStatusCode(95);

            responseMsg.setStatusDescription("Server currently unavailable.");
        }
    }

    public void card2Mobile() {

        String serverName = "SRVATKE01";

        Node nodeDetails = new Node();

        NodeDAO nodeDAO = new NodeDAO();

        nodeDetails = nodeDAO.getServerDetails(serverName);

        TrxRequest isoRequest = new TrxRequest();

        TrxResponse responseMsg = new TrxResponse();
        
        String destmsisdn = "254788542196";
        
        String sourcemsisdn = "254788542196";

        String AMOUNT = "100";

        String transactionID = "49360";
        
        int origID = 25401;
        
        String orig = "B2C App";

        String payload = destmsisdn + "|" + AMOUNT + "|" + transactionID; 
                
        Hits hit = new Hits();
        
        hit.setTrxType("AMC2B");
        
        hit.setPayload(payload);
        
//        AMB2CDAO b2cDAO = new AMB2CDAO();
        
//        int hitID = b2cDAO.logHits(hit);

        int nodeStatus = nodeDetails.getStatusCode();

        if (nodeStatus == 0) {

            AmCardToAM mobileDeposit = new AmCardToAM();
            
            String institutionCode = nodeDetails.getInstitutionCode();

            String merchantCode = nodeDetails.getAccountcode();

            String currencyCode = nodeDetails.getCurrencyCode();

            String serverIP = nodeDetails.getNodeIP();

            int serverPort = nodeDetails.getNodePort();

            int timeout = nodeDetails.getTimeout();

            isoRequest.setTransactionID(transactionID);
            isoRequest.setMsisdn(destmsisdn);
            isoRequest.setAmount(AMOUNT);
            isoRequest.setInstitutionCode(institutionCode);
            isoRequest.setMerchantCode(merchantCode);
            isoRequest.setCurrencyCode(currencyCode);
            isoRequest.setTimeout(timeout);
            isoRequest.setISOServerIP(serverIP);
            isoRequest.setISOServerPort(serverPort);

            try {
                
                responseMsg = mobileDeposit.CardToAMRequest(isoRequest);

                System.out.println("Card2Mobile status code : " + responseMsg.getStatusCode());

                System.out.println("Card2Mobile status description : " + responseMsg.getStatusDescription());

                System.out.println("Card2Mobile additional data : " + responseMsg.getTransactionData());
                
                Transfer b2cTrx = new Transfer();
                
//                b2cTrx.setHitsID(hitID);
                b2cTrx.setNodeID(nodeDetails.getNodeID());
                b2cTrx.setOrigID(origID);
                b2cTrx.setOrig(orig);
                b2cTrx.setBusinessNumber(nodeDetails.getAccountcode());
                b2cTrx.setTrxCode(transactionID);
                b2cTrx.setDestAccount(destmsisdn);
                b2cTrx.setSourceMSISDN(sourcemsisdn);
                b2cTrx.setAmount(AMOUNT);
                b2cTrx.setCurrencyCode(currencyCode);
                b2cTrx.setResultData(responseMsg.getTransactionData());
                b2cTrx.setStatusCode(0);
//                b2cTrx.setResultStatus(String.valueOf(responseMsg.getStatusCode()));
                b2cTrx.setTraceNumber(responseMsg.getTransactionID());
                b2cTrx.setDescription(responseMsg.getStatusDescription());
                
//                b2cDAO.addC2BTrx(b2cTrx);

            } catch (ISOException | IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        } else {

            responseMsg.setStatusCode(95);

            responseMsg.setStatusDescription("Server currently unavailable.");
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

        AmIsoTester isoTest = new AmIsoTester();

        isoTest.echoTest("SRVATKE01");

//        isoTest.kycTest();

//        isoTest.card2Mobile();

    }

}

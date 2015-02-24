/*
    Author: Alex Mbolonzi

 */
package ke.co.am.ISOIntegrator;

import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jpos.iso.ISOException;

import ke.co.am.ISOIntegrator.AmCardToAM;
import ke.co.am.ISOIntegrator.AmEcho;
import ke.co.am.ISOIntegrator.AmKyc;
//import ke.co.ars.dao.AMB2CDAO;
//import ke.co.ars.dao.MobileSubscriberDAO;
import ke.co.ars.dao.NodeDAO;
import ke.co.ars.dao.TransferB2CDAO;
//import ke.co.ars.entity.Hits;
import ke.co.ars.entity.TrxRequest;
import ke.co.ars.entity.TrxResponse;
import ke.co.ars.entity.MobileSubscriber;
import ke.co.ars.entity.Node;
//import ke.co.ars.entity.TransactionStatus;
import ke.co.ars.entity.Transfer;
//import ke.co.ars.oracle.dao.OracleTransferB2CDAO;

public class AmIsoJob {
    
    /* Get actual class name to be printed on */
    static Logger log = Logger.getLogger(AmIsoJob.class.getName());

    public AmIsoJob() {
        
      //PropertiesConfigurator is used to configure logger from properties file
        PropertyConfigurator.configure("/opt/log4j/amlog4j.properties");
    }
    
    public void echoTest(String hostName) {
        
        log.info("Initiating Echo request to " + hostName);

        TrxResponse responseMsg = new TrxResponse();

        AmEcho echo = new AmEcho();

        String serverName = hostName;

        try {

            responseMsg = echo.EchoRequest(serverName);

        } catch (ISOException | IOException e) {
            // TODO Auto-generated catch block
            responseMsg.setStatusCode(96);
            
            responseMsg.setStatusDescription("ERROR: IO Exception");
            
//            e.printStackTrace();
            log.error("Exception: ",e.fillInStackTrace());
        }

    }

    public TrxResponse kycData(String hostName, String mobileNumber) {

        log.info("Initiating KYC request for MSISDN " + mobileNumber);
        
        String serverName = hostName;

        Node nodeDetails = new Node();

        NodeDAO nodeDAO = new NodeDAO();

        log.info("Getting server details for " + hostName);
        
        nodeDetails = nodeDAO.getServerDetails(serverName);

        TrxRequest isoRequest = new TrxRequest();

        TrxResponse responseMsg = new TrxResponse();
        
//        String response = null;

        AmKyc kyc = new AmKyc();

        String msisdn = mobileNumber;

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
//
//                    log.info("Insert subscriber details to DB " + subName + " " + msisdn);
//                    
////                    check if subscriber is already stored in MMM DB
//                    List<MobileSubscriber> subscriberList = new ArrayList<MobileSubscriber>();
//                    
//                    subscriberList.addAll(subscriberDAO.findByMSISDN(msisdn));
//                    
//                    if(subscriberList.isEmpty()) {
////                        add subscriber to MMM DB
//                        subscriberDAO.addMobileSubscriber(subscriber);
//                    }      

                }
                
            } catch (ISOException | IOException e) {
                // TODO Auto-generated catch block
                responseMsg.setStatusCode(30);
                
                responseMsg.setStatusDescription("ERROR: Unable to parse ISO message");
                
//                e.printStackTrace();
                log.error("Exception: ",e.fillInStackTrace());
                
            }

        } else {

            responseMsg.setStatusCode(95);

            responseMsg.setStatusDescription("Server currently unavailable.");
        }
//        
//        response = responseMsg.getStatus() + "$" + responseMsg.getStatusDescription() 
//                + "$" + responseMsg.getTransactionData();
//                
//        log.info("Response: " + response);
        
        return responseMsg;
    }

    public TrxResponse card2Mobile(String hostName, String destmobile, 
            String sourcemobile, String creditAmount, String trxID, int sourceID, 
            String source,String beneficiary_name) {

        log.info("Initiating Card2Mobile request.....");
        
        Transfer transferC2MTransaction = new Transfer();
        
        transferC2MTransaction.setServerName(hostName);
        transferC2MTransaction.setTransactionType("AM_C2M");
        transferC2MTransaction.setDestAccount(destmobile);
        transferC2MTransaction.setAmount(creditAmount);
        transferC2MTransaction.setTrxCode(trxID);
        transferC2MTransaction.setSourceMSISDN(sourcemobile);
        transferC2MTransaction.setOrigID(sourceID);
        transferC2MTransaction.setOrig(source);

        TransferB2CDAO transferC2MDAO = new TransferB2CDAO();
        
        TrxRequest amC2MReq = new TrxRequest();
        
        log.info("Getting server details for " + hostName);
        
        amC2MReq = transferC2MDAO.logTransaction(transferC2MTransaction);

        TrxResponse c2mresponseMsg = new TrxResponse();
             
      switch(amC2MReq.getStatusCode()){
      case 94:
      	
      		c2mresponseMsg.setStatusCode(94);

      		c2mresponseMsg.setStatusDescription("Duplicate transaction.");
          
      	break;
      case 95:
      	
      		c2mresponseMsg.setStatusCode(95);

      		c2mresponseMsg.setStatusDescription("Server currently unavailable.");
          
      	break;
      default:
      	
    	  TrxRequest amIsoRequest = new TrxRequest();
    	  
    	  amIsoRequest.setTransactionID(amC2MReq.getTransactionID());
    	  amIsoRequest.setMsisdn(destmobile);
    	  amIsoRequest.setAmount(creditAmount);
    	  amIsoRequest.setInstitutionCode(amC2MReq.getInstitutionCode());
    	  amIsoRequest.setMerchantCode(amC2MReq.getMerchantCode());
    	  amIsoRequest.setCurrencyCode(amC2MReq.getCurrencyCode());
    	  amIsoRequest.setTimeout(amC2MReq.getTimeout());
    	  amIsoRequest.setISOServerIP(amC2MReq.getISOServerIP());
    	  amIsoRequest.setISOServerPort(amC2MReq.getISOServerPort());
      	
      	log.info("Sending Card2Mobile transaction details.... ");
      	
      	AmCardToAM amMobileDeposit = new AmCardToAM();
      
      	TrxResponse c2mResponseTransactions = new TrxResponse();
      	
      	try {
      		
      		c2mResponseTransactions = amMobileDeposit.CardToAMRequest(amIsoRequest);
            
        } catch (ISOException | IOException e) {
            // TODO Auto-generated catch block
//            e.printStackTrace();
            log.error("Exception: ",e.fillInStackTrace());
        }
      	      	
      	c2mResponseTransactions.setTransactionID(amC2MReq.getTransactionID());
//      	c2mResponseTransactions.setStatus("100");
//      	c2mResponseTransactions.setStatusDescription("Processing");
      	log.info("Update Card2Bank transaction details.... ");
      	  transferC2MDAO.updateTransactionResponse(c2mResponseTransactions);
      	
      	  c2mresponseMsg.setStatusCode(0);
          c2mresponseMsg.setStatusDescription(c2mResponseTransactions.getStatusDescription());
               	
      	break;
      }

//        response = responseMsg.getStatus() + "$" + responseMsg.getStatusDescription() 
//                + "$" + responseMsg.getTransactionData();
//        
//        log.info("TrxResponse: " + response);
        
        return c2mresponseMsg;
    }
}

package ke.co.am.ISOIntegrator;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import ke.co.ars.dao.NodeDAO;
import ke.co.ars.entity.TrxResponse;
import ke.co.ars.entity.Node;
import ke.co.am.ISOIntegrator.AmEcho;
import ke.co.am.ISOIntegrator.AmIsoParser;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jpos.iso.ISOException;
import org.jpos.iso.channel.ASCIIChannel;
import org.jpos.iso.packager.GenericPackager;
import org.jpos.iso.ISOMsg;


public class AmEcho {
	
    /* Get actual class name to be printed on */
    static Logger log = Logger.getLogger(AmEcho.class.getName());
    
	public TrxResponse EchoRequest (String serverName) throws ISOException, IOException {
		
		//PropertiesConfigurator is used to configure logger from properties file
        PropertyConfigurator.configure("/opt/log4j/amlog4j.properties");
        
	    log.info("Recieved Echo request.....");
    
	    NodeDAO echoDAO = new NodeDAO();
	    
	    Node nodeDetails = new Node();
	    
	    nodeDetails = echoDAO.getServerDetails(serverName);
	    
	    TrxResponse responseMsg = new TrxResponse();
	    
	    //if(nodeDetails.getStatus() == 0) {
	    
	    int timeout = nodeDetails.getTimeout();
        
        String serverIP = nodeDetails.getNodeIP();
        
        int serverPort = nodeDetails.getNodePort();
        
        log.info("Echo request to ..... " + serverIP + serverPort);
//        Logger logger = new Logger();
//        logger.addListener (new SimpleLogListener(System.out));
 
        ASCIIChannel channel = new ASCIIChannel(
                serverIP, serverPort, new GenericPackager("/opt/ISO/iso87ascii.xml")
        );
 
//        ((LogSource)channel).setLogger (logger, "test-channel");

		SimpleDateFormat transactionTime = new SimpleDateFormat("hhmmss");
		SimpleDateFormat transmissionDate = new SimpleDateFormat("MMddhhmmss");
		SimpleDateFormat transactionMonthDay = new SimpleDateFormat("MMdd");
		Date date = new Date();

        try {          
            channel.setTimeout(timeout);
			channel.connect ();
		} catch (IOException e) {

		    responseMsg.setStatusCode(97);
            
            responseMsg.setStatusDescription("ERROR: connection timeout to " + serverIP + " on port " + serverPort);
            
//			e.printStackTrace();
            log.error("Exception: ",e.fillInStackTrace());
		}
        
        ISOMsg m = new ISOMsg ();
        m.setMTI("0800");
        m.set(7,transmissionDate.format(date));
        m.set(11,"049360");
        m.set(12,transactionTime.format(date));
        m.set(13,transactionMonthDay.format(date));
        m.set(70,"301");

        try {
            
            log.info("Echo ISO request : " + m.toString());
            
			channel.send (m);

//			System.out.println(channel.toString());
			
			ISOMsg isoResponse = channel.receive();
	        
			AmIsoParser isoMessageParser = new AmIsoParser();
            
            responseMsg = isoMessageParser.ParseISOMessage(isoResponse);
            
	        channel.disconnect ();
 
		} catch (IOException e) {
		    
		    responseMsg.setStatusCode(96);
            
            responseMsg.setStatusDescription("ERROR: Unable to parse ISO response message");
            
//			e.printStackTrace();
            log.error("Exception: ",e.fillInStackTrace());
		}
	    
//        responseMsg.setTransactionID(serverName);
        nodeDetails.setStatusCode(responseMsg.getStatusCode());
        nodeDetails.setStatusDescription(responseMsg.getStatusDescription());
        
        echoDAO.updateEchoStatus(nodeDetails);
        
		return responseMsg;
	}
	
}

/*
    Author: Alex Mbolonzi

 */
package ke.co.am.ISOIntegrator;

import java.util.Date;

import ke.co.ars.entity.TrxResponse;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jpos.iso.ISOMsg;

public class AmIsoParser {
    
    /* Get actual class name to be printed on */
    static Logger log = Logger.getLogger(AmIsoParser.class.getName());

    public TrxResponse ParseISOMessage(ISOMsg isoMessage) {
        
      //PropertiesConfigurator is used to configure logger from properties file
        PropertyConfigurator.configure("/opt/log4j/amlog4j.properties");

        TrxResponse responseMsg = new TrxResponse();

        Date date = new Date();

        String MTI = null;

        String statusCode = null;

        String processingCode = null;

        String addDataPrivate = null;
        
        log.info("Parsing ISO response....." + isoMessage);
        
        /* get the values from the ISO message */

        MTI = isoMessage.getString(0);

        statusCode = isoMessage.getString(39);

        switch (MTI) {

            case "1810":

                switch (statusCode) {

                    case "00":
                        responseMsg.setStatusDescription("Echo test OK at " + date);

                        break;

                    default:
                        responseMsg.setStatusDescription("Echo test failed at " + date);
                }

                responseMsg.setStatusCode(Integer.valueOf(statusCode));

                break;

            case "1210":

                processingCode = isoMessage.getString(3);

                switch (statusCode) {

                    case "00":

                        switch (processingCode) {

                            case "070000":

                                addDataPrivate = isoMessage.getString(48);
                                
                                responseMsg.setTransactionData(addDataPrivate);
                                
                                responseMsg.setStatusDescription("AUTHORISED");
                                
                                responseMsg.setStatusCode(0);

                                break;

                            case "210000":
                                
                                addDataPrivate = isoMessage.getString(59);
                                
                                String stan = isoMessage.getString(11);
                                
                                responseMsg.setTransactionID(stan);
                                
                                responseMsg.setTransactionData(addDataPrivate);
                                
                                responseMsg.setStatusDescription("AUTHORISED");
                                
                                responseMsg.setStatusCode(0);
                                
                                break; 

                        }
                        
                        break;
                        
                        default :
                            
                            responseMsg.setStatusCode(Integer.valueOf(statusCode));
                            
                            responseMsg.setStatusDescription("RESTRICTED: " + isoMessage.getString(48));
                            
                        break;
                }

                break;
        }
        
        return responseMsg;
    }
}

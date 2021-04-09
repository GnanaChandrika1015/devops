/**
 * 
 */
package com.ibm.tpd.primarydb.ingestmsg.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ibm.tpd.primarydb.canonical.dto.Dispatch33INTERNALAPIC;
import com.ibm.tpd.primarydb.entity.response.SuccessResponse;
import com.ibm.tpd.primarydb.exception.InvalidJsonException;
import com.ibm.tpd.primarydb.exception.InvalidMessageTypeException;
import com.ibm.tpd.primarydb.exception.KafkaErrorTopicFailureException;
import com.ibm.tpd.primarydb.ingestmsg.service.IngestMessageService;
import com.ibm.tpd.primarydb.logger.PDBLogger;
import com.ibm.tpd.primarydb.util.TPDLogMessageConstructUtil;
import com.ibm.tpd.primarydb.util.TPDMessageTypes;
import com.ibm.tpd.primarydb.util.TPDServiceNames;

/**
 * @author SangitaPal
 *
 */
@RestController
@RequestMapping("/tpd/primarydb")
public class IngestMessageControllerImpl implements IngestMessageController{
	
	@Autowired
	private IngestMessageService ingestService;
	
	@Autowired
	PDBLogger pdbLogger;
	
	final private String serviceName = TPDServiceNames.Dispatch_Ingest_Service.serviceName();
	final private String msgType = TPDMessageTypes.Dispatch.getMsgType();
	
	/**
     * This is a Rest API of type POST for message ingestion to kafka for message type Dispatch of tobacco products(3-3).
     * 
     * First, it tries to push the message to operational topic. If failed, retries n number of times to push the message
     * to retry topic. This retry count is configurable using environment variable kafka.producer.retry.topic.max.retry.count
     * If pushing message to Retry topic is failed, then message will be pushed to Dead letter Queue Error topic. If service is able to 
     * to push message to any of these Kafka topics, SucessResponse with http status code 200 is returned, else ErrorResponse with 
     * http status code 500 is returned.
     *
     *@param 	msgType 
     *@param 	primaryRepositoryId
     *@param 	msg
     *@return	SuccessResponse
     *@throws	KafkaErrorTopicFailureException, InvalidMessageTypeException, Exception
     */
	@Override
	@PostMapping(path="/ingest/dispatch")
	public SuccessResponse ingestMessage(@RequestBody final Dispatch33INTERNALAPIC msgBody) throws InvalidJsonException, KafkaErrorTopicFailureException, InvalidMessageTypeException, Exception {
		long processingStartTimeInMs = System.currentTimeMillis();
		String methodName = "ingestMessage()";
		String primaryRepositoryId= new String();
		
		pdbLogger.debug( TPDLogMessageConstructUtil.infoStartMessage(serviceName, methodName, msgType, primaryRepositoryId));
		pdbLogger.debug( TPDLogMessageConstructUtil.debugMessage(serviceName, methodName, "for primaryRepositoryId " + primaryRepositoryId + " received json message: " + msgBody));
		
		SuccessResponse ingestResponse = ingestService.sendMessageToKafkaTopic(msgType, primaryRepositoryId, msgBody, processingStartTimeInMs);
		
		return ingestResponse;
	}
}

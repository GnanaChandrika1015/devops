/**
 * 
 */
package com.ibm.tpd.primarydb.ingestmsg.controller;

import com.ibm.tpd.primarydb.canonical.dto.Dispatch33INTERNALAPIC;
import com.ibm.tpd.primarydb.entity.response.SuccessResponse;
import com.ibm.tpd.primarydb.exception.InvalidJsonException;
import com.ibm.tpd.primarydb.exception.InvalidMessageTypeException;
import com.ibm.tpd.primarydb.exception.KafkaErrorTopicFailureException;

/**
 * @author SangitaPal
 *
 */
public interface IngestMessageController {
	public SuccessResponse ingestMessage(final Dispatch33INTERNALAPIC msgBody) throws InvalidJsonException, KafkaErrorTopicFailureException, InvalidMessageTypeException, Exception;
	
}

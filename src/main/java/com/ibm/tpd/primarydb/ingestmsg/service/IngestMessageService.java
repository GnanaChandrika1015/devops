package com.ibm.tpd.primarydb.ingestmsg.service;

import com.ibm.tpd.primarydb.canonical.dto.Dispatch33INTERNALAPIC;
import com.ibm.tpd.primarydb.entity.response.SuccessResponse;
import com.ibm.tpd.primarydb.exception.BusinessValidationFailedException;
import com.ibm.tpd.primarydb.exception.FailedToValidateBusinessRulesException;
import com.ibm.tpd.primarydb.exception.InvalidJsonException;
import com.ibm.tpd.primarydb.exception.InvalidMessageTypeException;
import com.ibm.tpd.primarydb.exception.KafkaErrorTopicFailureException;

/**
 * @author SangitaPal
 *
 */
public interface IngestMessageService {
	public SuccessResponse sendMessageToKafkaTopic(final String msgType, String primaryRepositoryId, final Dispatch33INTERNALAPIC msg, final long processingStartTimeInMs) 
			throws InvalidJsonException, KafkaErrorTopicFailureException, InvalidMessageTypeException, 
			BusinessValidationFailedException, FailedToValidateBusinessRulesException, Exception;
	
}

/**
 * 
 */
package com.ibm.tpd.primarydb.ingestmsg.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.datastax.driver.core.ConsistencyLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.tpd.config.CommonConfigProperties;
import com.ibm.tpd.primarydb.canonical.dto.Dispatch33INTERNALAPIC;
import com.ibm.tpd.primarydb.canonical.dto.Error;
import com.ibm.tpd.primarydb.canonical.dto.ValidationDTO;
import com.ibm.tpd.primarydb.dto.Message_Hash_Lookup;
import com.ibm.tpd.primarydb.dto.PrimaryRepositoryID_LookUp;
import com.ibm.tpd.primarydb.dto.StateMachine;
import com.ibm.tpd.primarydb.entity.response.SuccessResponse;
import com.ibm.tpd.primarydb.exception.BadRequestException;
import com.ibm.tpd.primarydb.exception.BusinessValidationFailedException;
import com.ibm.tpd.primarydb.exception.FailedToValidateBusinessRulesException;
import com.ibm.tpd.primarydb.exception.HashValidationException;
import com.ibm.tpd.primarydb.exception.InvalidJsonException;
import com.ibm.tpd.primarydb.exception.InvalidMessageTypeException;
import com.ibm.tpd.primarydb.exception.InvalidUIDFormatException;
import com.ibm.tpd.primarydb.exception.KafkaErrorTopicFailureException;
import com.ibm.tpd.primarydb.exception.KafkaProducerException;
import com.ibm.tpd.primarydb.exception.NonTpdUidExistsException;
import com.ibm.tpd.primarydb.exception.StateMachineOperationException;
import com.ibm.tpd.primarydb.exception.TPDException;
import com.ibm.tpd.primarydb.ingestmsg.util.IngestMessageHelper;
import com.ibm.tpd.primarydb.logger.PDBLogger;
import com.ibm.tpd.primarydb.statemachine.service.StateMachineService;
import com.ibm.tpd.primarydb.util.ApiStringUtil;
import com.ibm.tpd.primarydb.util.BusinessRuleValidationErrors;
import com.ibm.tpd.primarydb.util.BusinessValidationRespStatusCode;
import com.ibm.tpd.primarydb.util.LookupDataUtil;
import com.ibm.tpd.primarydb.util.MetricsEventTypes;
import com.ibm.tpd.primarydb.util.MetricsUtil;
import com.ibm.tpd.primarydb.util.TPDConstants;
import com.ibm.tpd.primarydb.util.TPDDispatchedUITypes;
import com.ibm.tpd.primarydb.util.TPDErrorMessages;
import com.ibm.tpd.primarydb.util.TPDInfoMessages;
import com.ibm.tpd.primarydb.util.TPDIssueTypes;
import com.ibm.tpd.primarydb.util.TPDLogMessageConstructUtil;
import com.ibm.tpd.primarydb.util.TPDMessageChannelTypes;
import com.ibm.tpd.primarydb.util.TPDMessageTypes;
import com.ibm.tpd.primarydb.util.TPDServiceNames;
import com.ibm.tpd.primarydb.util.TPDStageTypes;
import com.ibm.tpd.primarydb.util.TPDStatusCodeConstants;
import com.ibm.tpd.primarydb.util.TimestampUtil;
import com.ibm.tpd.services.CommonTPDService;
import com.ibm.tpd.services.PublishCanonicalMsgService;
import com.ibm.tpd.services.PublishMetricsEventMsgService;
import com.ibm.tpd.validation.ValidationHandler;

/**
 * @author SangitaPal
 * 
 * This is service class for producing message to Kafka topic. Topic name selection is dependent on msgType.
 * Topic names are configured in properties file. Custom KafkaErrorTopicFailureException is thrown in case of any exception
 * during message publication o Kafka. 
 *
 */
@Service
public class IngestMessageServiceImpl extends CommonTPDService implements IngestMessageService{
	
	final String serviceName = TPDServiceNames.Dispatch_Ingest_Service.serviceName();
	
	@Value("${kafka.producer.success.status.code}")
	private String statusCode;

	@Value("${kafka.producer.success.status.desc}")
	private String statusDesc;
	
	@Value("${business.validation.enabled:true}")
    boolean businessValidationEnabled;
	
	@Autowired
	CommonConfigProperties commonConfigProps;
	
	private final TPDOutboundMessageStreams messageStreams;
	
	protected final Map<String, MessageChannel> messageChannelMap = new HashMap<String, MessageChannel>();
	
	@Autowired
	PDBLogger pdbLogger;
	
	@Autowired
	IngestMessageHelper ingestMessageHelper;
	
	@Autowired
	private StateMachineService stateMachineService;
	
	@Autowired
	ValidationHandler validationHandler;
	
	@Autowired
	PublishMetricsEventMsgService publishMetricsEventMsgService;
	
	@Autowired
	PublishCanonicalMsgService publishCanonicalMsgService;
	
    public IngestMessageServiceImpl(TPDOutboundMessageStreams messageStreams) {
        this.messageStreams = messageStreams;
        this.messageChannelMap.putAll(setMessageChannelMap());
    }
    
    /**
     * This method used to push message to kafka topic.
     * 
     * First, it tries to push the message to operational topic. If failed, retries n number of times to push the message
     * to retry topic. This retry count is configurable using environment variable kafka.producer.retry.topic.max.retry.count
     * If pushing message to Retry topic is failed, then message will be pushed to Dead letter Queue Error topic.
     *
     *@param 	msgType 
     *@param 	primaryRepositoryId
     *@param 	msg
     *@return	SuccessResponse
     *@throws	InvalidJsonException, KafkaErrorTopicFailureException, InvalidMessageTypeException, 
     *			BusinessValidationFailedException, FailedToValidateBusinessRulesException, Exception
     */
    @Override
    public SuccessResponse sendMessageToKafkaTopic(final String msgType, String primaryRepositoryId, final Dispatch33INTERNALAPIC  msg, final long processingStartTimeInMs) 
    				throws InvalidJsonException, KafkaErrorTopicFailureException, InvalidMessageTypeException, 
    				BusinessValidationFailedException, FailedToValidateBusinessRulesException, KafkaProducerException, Exception {
    	
    	String methodName = "sendMessageToKafkaTopic()";
    	
    	long eventStartTime = 0;
    	long validationStartTime = 0;
    	long validationEndTime = 0;
    	String clientAppName = "";
    	String sourceMessageId= msg.getDispatch().getMessageHeader().getSourceMessageID();
    	long timeStamp = System.currentTimeMillis();    	
    	ValidationDTO validationDTO = null;
    	boolean isMessageSentToTopic = false;
    	Timestamp ts = new Timestamp(System.currentTimeMillis());
    	PrimaryRepositoryID_LookUp repIDObject = new PrimaryRepositoryID_LookUp();
    	Message_Hash_Lookup msgHashLookup = new Message_Hash_Lookup();
    	String messageHash = "";
    	String jsonMsg = "";
    	StateMachine stateMachine= new StateMachine();
    	try {
			clientAppName = msg.getDispatch().getMessageHeader().getClientAppName();
			String apiInitiatedTime = msg.getDispatch().getMessageHeader().getApiInitiatedTime();
			messageHash = msg.getDispatch().getMessageHeader().getMessageHash();

			primaryRepositoryId = msg.getDispatch().getMessageHeader().getPrimaryRepositoryID();
			jsonMsg = new ObjectMapper().writeValueAsString(msg);
			pdbLogger.debug(TPDLogMessageConstructUtil.debugMessage(serviceName, methodName,
					msg.getDispatch().getMessageType().toString(), primaryRepositoryId, jsonMsg));

			long apiInitiatedTimeInMs = TimestampUtil.convertUTCTimeStampToMilleSeconds(apiInitiatedTime);

			publishMetricsEventMsgService.sendMetricsEventMsg(TPDMessageTypes.Dispatch.getMsgType(),
					primaryRepositoryId, clientAppName, MetricsEventTypes.API_Initiated.eventType(),
					apiInitiatedTimeInMs, apiInitiatedTimeInMs, MetricsEventTypes.API_Initiated.serviceType(),
					sourceMessageId, MetricsEventTypes.API_Initiated.successCode(), "", "",
					MetricsEventTypes.API_Initiated.successDesc() + "||" + "Ingest invoked at " + Instant.now());

			validationDTO = ingestMessageHelper.populateValidationObj(msg);

			if (!clientAppName.isEmpty() || clientAppName.length() > 0) {
				// if router message perform primaryRepositoryIDvalidation
				validationStartTime = System.currentTimeMillis();
				validationEndTime = System.currentTimeMillis();
				validatePrimaryRepositoryId(primaryRepositoryId);
			} else if (commonConfigProps.isHashValidationEnabled() && msgType.equals(validationDTO.getMessageType())) {
				// if BAT message perform hash validation
				validationStartTime = System.currentTimeMillis();
				validationEndTime = System.currentTimeMillis();
				validateMessageHash(messageHash, primaryRepositoryId);
			}

			if (businessValidationEnabled && msgType.equals(validationDTO.getMessageType())) {
				validationStartTime = System.currentTimeMillis();
				validationEndTime = System.currentTimeMillis();
				validationDTO = validationHandler.performJsonValidations(validationDTO);//...............Invoke for Non-DB Json Validations
				if (null != validationDTO.getRejectionData().getErrors()
						&& !validationDTO.getRejectionData().getErrors().isEmpty()) {
					throw new BadRequestException("Business validation failed", validationDTO.getRejectionData(),
							validationDTO.getSourceMessageID(), validationDTO.getTestFlag(),
							validationDTO.getMessageType(), validationDTO.getPrimaryRepositoryID(),
							validationDTO.getStatusCode());
				}
			}

			validationDTO = populateValidationDTO_DBValues_Async(validationDTO, true);

			if (businessValidationEnabled && msgType.equals(validationDTO.getMessageType())) {
				validationDTO = validationHandler.performDBValidations(validationDTO);//................Invoke for DBValidations
				msg.getDispatch().setRejectionData(validationDTO.getRejectionData());
				jsonMsg = new ObjectMapper().writeValueAsString(msg);
				validationEndTime = System.currentTimeMillis();
			}
				
			if (!msgType.equals(validationDTO.getMessageType())
					|| TPDStatusCodeConstants.BusinessValidationStatus.equals(validationDTO.getStatusCode())
					|| TPDStatusCodeConstants.SuccessStatus.equals(validationDTO.getStatusCode())) {
				// Invoke state machine to capture the change in state of UIs
				stateMachine = changeState(msg, apiInitiatedTimeInMs, validationDTO);

				// Throw exception if state machine has failed
				if (!stateMachine.getRejectionData().getErrors().isEmpty()) {
					Error error = stateMachine.getRejectionData().getErrors().get(0);
					throw new StateMachineOperationException(error.getErrorCode(), error.getErrorDescription(),
							primaryRepositoryId);
				}

				// raise event if implicit disaggregation has occurred
				if (stateMachine.isImplicitDisaggr()) {
					eventStartTime = System.currentTimeMillis();
					publishMetricsEventMsgService.sendMetricsEventMsg(TPDMessageTypes.Dispatch.getMsgType(),
							primaryRepositoryId, clientAppName, MetricsEventTypes.Implicit_Disaggregation.eventType(),
							eventStartTime, System.currentTimeMillis(),
							MetricsEventTypes.Implicit_Disaggregation.serviceType(), "",
							MetricsEventTypes.Implicit_Disaggregation.successCode(), "", "",
							MetricsEventTypes.Implicit_Disaggregation.successDesc());
				}

				// raise event if data has been written into ScyllaDB with LOCAL_QUORUM
				// Consistency Level
				if (ConsistencyLevel.LOCAL_QUORUM.toString().equals(stateMachine.getWriteConsistencyLevel())) {
					eventStartTime = System.currentTimeMillis();
					publishMetricsEventMsgService.sendMetricsEventMsg(TPDMessageTypes.Dispatch.getMsgType(),
							primaryRepositoryId, clientAppName,
							MetricsEventTypes.Produce_NodeReplication_Topic.eventType(), eventStartTime,
							System.currentTimeMillis(), MetricsEventTypes.Produce_NodeReplication_Topic.serviceType(),
							"", MetricsEventTypes.Produce_NodeReplication_Topic.successCode(), "", "",
							MetricsEventTypes.Produce_NodeReplication_Topic.successDesc());
				}
			}
				
			/*
			 * First try to send message to Operational topic. If failed, throw
			 * KafkaProducerException
			 */
			eventStartTime = System.currentTimeMillis();
			if (validationDTO.getRejectionData().getErrors().isEmpty()
					&& publishCanonicalMsgService.sendMessageToOperationalTopic(msgType, primaryRepositoryId, clientAppName, jsonMsg, 
							ingestMessageHelper.getMessageChannel(messageChannelMap, TPDMessageChannelTypes.Ingest_BAT_Operational.msgChannelType()), 
							validationDTO.getStatusCode())) {
				isMessageSentToTopic = true;
				if (!clientAppName.isEmpty() || clientAppName.length() > 0) {
					repIDObject = LookupDataUtil.populatePrimaryRepIDDTO(repIDObject, primaryRepositoryId, ts);
					insertPrimaryRepositoryId(repIDObject, serviceName, msgType);
				} else if (commonConfigProps.isHashValidationEnabled()) {
					msgHashLookup = LookupDataUtil.populateMessageHashLookUpDTO(msgHashLookup, primaryRepositoryId,
							messageHash, ts);
					insertMessageHash(msgHashLookup, apiInitiatedTime, msgType);
				}
			}
    		
			if (msgType.equals(validationDTO.getMessageType())) {
				if (!validationDTO.getRejectionData().getErrors().isEmpty()) {
					if (TPDStatusCodeConstants.BusinessValidationStatus.equals(validationDTO.getStatusCode())) {
						// Record event metrics
						publishMetricsEventMsgService.sendMetricsEventMsg(TPDMessageTypes.Dispatch.getMsgType(),
								primaryRepositoryId, clientAppName,
								MetricsEventTypes.Business_Validation_299.eventType(), validationStartTime,
								validationEndTime, MetricsEventTypes.Business_Validation_299.serviceType(), "",
								MetricsEventTypes.Business_Validation_299.errorCode(),
								BusinessValidationRespStatusCode.BusinessValidationFailed.respStatusCode(), "",
								MetricsUtil.multMetricErrCode(validationDTO.getRejectionData().getErrors()));
					} else if (TPDStatusCodeConstants.BadRequestStatus.equals(validationDTO.getStatusCode())) {
						// Record event metrics
						publishMetricsEventMsgService.sendMetricsEventMsg(TPDMessageTypes.Dispatch.getMsgType(),
								primaryRepositoryId, clientAppName,
								MetricsEventTypes.Business_Validation_400.eventType(), validationStartTime,
								validationEndTime, MetricsEventTypes.Business_Validation_400.serviceType(), "",
								MetricsEventTypes.Business_Validation_400.errorCode(),
								BusinessValidationRespStatusCode.BadRequestValidation.respStatusCode(), "",
								MetricsUtil.multMetricErrCode(validationDTO.getRejectionData().getErrors()));
					}
					throw new BusinessValidationFailedException("Business validation failed",
							validationDTO.getRejectionData(), validationDTO.getSourceMessageID(),
							msg.getDispatch().getMessageHeader().getTestFlag().value(), validationDTO.getMessageType(),
							validationDTO.getPrimaryRepositoryID(), validationDTO.getStatusCode());
				}
			}
		} catch (InvalidMessageTypeException ex) {
			String errorMessage = TPDLogMessageConstructUtil
					.errorMessage(TPDErrorMessages.Invalid_message_Type_Specified.errorCode(),
							TPDErrorMessages.Invalid_message_Type_Specified.severity(), serviceName, methodName,
							msgType, ex.getPrimaryRepositoryID(), null,
							TPDErrorMessages.Invalid_message_Type_Specified.errorMessage(),
							ex.getMessage() + " Total time taken(ms) to process the request: "
									+ (System.currentTimeMillis() - processingStartTimeInMs),
							jsonMsg, ex.getStackTrace());
			pdbLogger.error(errorMessage);
			throw new InvalidJsonException(ex.getErrCode(), ex.getErrMsg(), primaryRepositoryId);

		} catch (BusinessValidationFailedException ex) {
			if (!isMessageSentToTopic) {
				publishCanonicalMsgService.sendMessageToOperationalTopic(msgType, primaryRepositoryId, clientAppName, jsonMsg, 
						ingestMessageHelper.getMessageChannel(messageChannelMap, TPDMessageChannelTypes.Ingest_BAT_Operational.msgChannelType()), 
						validationDTO.getStatusCode());
			}
			String errorMessage = TPDLogMessageConstructUtil.errorMessage(
					TPDErrorMessages.Business_Rule_Validation_Failed.errorCode(),
					TPDErrorMessages.Business_Rule_Validation_Failed.severity(), serviceName, methodName, msgType,
					primaryRepositoryId, null, TPDErrorMessages.Business_Rule_Validation_Failed.errorMessage(),
					ex.getRejectionData().toString() + " Total time taken(ms) to process the request: "
							+ (System.currentTimeMillis() - processingStartTimeInMs),
					null);
			pdbLogger.info(errorMessage);
			throw ex;

		} catch (FailedToValidateBusinessRulesException ex) {
			String errorMessage = TPDLogMessageConstructUtil
					.errorMessage(TPDErrorMessages.Failed_To_Validate_Business_Rules_Failed.errorCode(),
							TPDErrorMessages.Failed_To_Validate_Business_Rules_Failed.severity(), serviceName,
							methodName, msgType, primaryRepositoryId, null,
							TPDErrorMessages.Failed_To_Validate_Business_Rules_Failed.errorMessage(),
							ex.getMessage() + " Total time taken(ms) to process the request: "
									+ (System.currentTimeMillis() - processingStartTimeInMs),
							jsonMsg, ex.getStackTrace());
			raiseIssue(primaryRepositoryId, TPDMessageTypes.Dispatch.getMsgType(), timeStamp,
					TPDIssueTypes.Failed_To_Write_To_IngestMain.getIssueId(),
					TPDErrorMessages.Message_Send_To_Kafka_Topic_In_Ingest_Failed.errorCode(), 0,
					TPDErrorMessages.Message_Send_To_Kafka_Topic_In_Ingest_Failed.errorMessage(), "false", "Error",
					TPDStageTypes.Ingest.getStageId());
			pdbLogger.error(errorMessage);
			throw ex;

		} catch (NonTpdUidExistsException ex) {
			pdbLogger.info(TPDLogMessageConstructUtil.infoHighSeverityMessage(
					TPDInfoMessages.Non_TPD_UI_Exists_In_Message.infoCode(),
					TPDInfoMessages.Non_TPD_UI_Exists_In_Message.severity(), serviceName, methodName, msgType,
					primaryRepositoryId,
					TPDInfoMessages.Non_TPD_UI_Exists_In_Message.infoMessage() + ex.getRejectionData().toString()
							+ " Total time taken(ms) to process the request: "
							+ (System.currentTimeMillis() - processingStartTimeInMs)));
			throw ex;

		} catch (InvalidUIDFormatException ex) {
			pdbLogger.info(TPDLogMessageConstructUtil.infoHighSeverityMessage(
					TPDInfoMessages.Invalid_UID_Format.infoCode(), TPDInfoMessages.Invalid_UID_Format.severity(),
					serviceName, methodName, msgType, primaryRepositoryId,
					TPDInfoMessages.Invalid_UID_Format.infoMessage() + ex.getRejectionData().toString()
							+ " Total time taken(ms) to process the request: "
							+ (System.currentTimeMillis() - processingStartTimeInMs)));
			throw ex;

		} catch (StateMachineOperationException ex) {
			eventStartTime = System.currentTimeMillis();
			String severity = TPDErrorMessages.Generic_Exception_Occurred.severity();
			if (TPDErrorMessages.Failed_To_Update_State_In_Lookup_Table.errorCode().equals(ex.getErrCode())) {
				severity = TPDErrorMessages.Failed_To_Update_State_In_Lookup_Table.severity();
			} else if (TPDErrorMessages.Failed_To_Find_UI_In_Lookup_Table.errorCode().equals(ex.getErrCode())) {
				severity = TPDErrorMessages.Failed_To_Find_UI_In_Lookup_Table.severity();
			}
			String errorMessage = TPDLogMessageConstructUtil.errorMessage(ex.getErrCode(), severity, serviceName,
					methodName, msgType, primaryRepositoryId, null, ex.getErrMsg(),
					"Total time taken(ms) to process the request: "
							+ (System.currentTimeMillis() - processingStartTimeInMs),
					jsonMsg, null);
			pdbLogger.error(errorMessage);

			// ---------
			// Put the request payload JSON message in Kafka error topic and record the
			// failed event in metrics db
			// ---------
			publishCanonicalMsgService.sendMessageToErrorTopic(msgType, primaryRepositoryId, clientAppName, jsonMsg, 
					ingestMessageHelper.getMessageChannel(messageChannelMap, TPDMessageChannelTypes.Ingest_BAT_Error.msgChannelType()));
			publishMetricsEventMsgService.sendMetricsEventMsg(TPDMessageTypes.Dispatch.getMsgType(),
					primaryRepositoryId, clientAppName, MetricsEventTypes.Produce_Ingest_Main.eventType(),
					eventStartTime, System.currentTimeMillis(), MetricsEventTypes.Produce_Ingest_Main.serviceType(), "",
					MetricsEventTypes.Produce_Ingest_Main.errorCode(),
					String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()), "",
					StateMachineOperationException.class.getName() + "||" + ex.getErrMsg());

			throw ex;

		} catch (TPDException ex) {
			String errorMessage = TPDLogMessageConstructUtil.errorMessage(ex.getErrCode(), "", serviceName, methodName,
					msgType, primaryRepositoryId, null, ex.getErrMsg(),
					ex.getMessage() + " Total time taken(ms) to process the request: "
							+ (System.currentTimeMillis() - processingStartTimeInMs),
					msg.toString(), ex.getStackTrace());
			pdbLogger.error(errorMessage);
			raiseIssue(primaryRepositoryId, TPDMessageTypes.Dispatch.getMsgType(), timeStamp,
					TPDIssueTypes.Failed_To_Write_To_IngestMain.getIssueId(), ex.getErrCode(), 0, ex.getErrMsg(),
					"false", "Error", TPDStageTypes.Ingest.getStageId());

			String errorData = "";
			if (!clientAppName.isEmpty() || clientAppName.length() > 0) {
				errorData = primaryRepositoryId;
			} else if (commonConfigProps.isHashValidationEnabled()) {
				errorData = ex.getPrimaryRepositoryID();
			}

			List<Error> rejectionErrors = new ArrayList<Error>();
			Error error = new Error();
			error.setValidationControl(ex.getValidationControl());
			error.setErrorCode(ex.getErrCode());
			error.setErrorDescription(ex.getErrMsg());
			error.setErrorData(errorData);
			rejectionErrors.add(error);
			validationDTO.getRejectionData().getErrors().addAll(rejectionErrors);

			// Record CODE_NOT_UNIQUE/PAYLOAD_NOT_UNIQUE event in metrics db
			if (ex.getErrCode().equals(BusinessRuleValidationErrors.CODE_NOT_UNIQUE.errCode())
					|| ex.getErrCode().equals(BusinessRuleValidationErrors.PAYLOAD_NOT_UNIQUE.errCode())) {
				String errorDesc = MetricsEventTypes.Technical_Validation_400.errorDesc();
				if (!validationDTO.getRejectionData().getErrors().isEmpty())
					errorDesc = MetricsUtil.multMetricErrCode(validationDTO.getRejectionData().getErrors());
				publishMetricsEventMsgService.sendMetricsEventMsg(TPDMessageTypes.Dispatch.getMsgType(),
						primaryRepositoryId, clientAppName, MetricsEventTypes.Technical_Validation_400.eventType(),
						validationStartTime, validationEndTime,
						MetricsEventTypes.Technical_Validation_400.serviceType(), "",
						MetricsEventTypes.Technical_Validation_400.errorCode(),
						BusinessValidationRespStatusCode.BadRequestValidation.respStatusCode(), "", errorDesc);
			}

			if (ex.getErrCode().equals(BusinessRuleValidationErrors.CODE_NOT_UNIQUE.errCode())) {
				throw new BadRequestException("PrimaryRepositoryID exists", validationDTO.getRejectionData(),
						validationDTO.getSourceMessageID(), msg.getDispatch().getMessageHeader().getTestFlag().value(),
						validationDTO.getMessageType(), validationDTO.getPrimaryRepositoryID());
			} else if (ex.getErrCode().equals(BusinessRuleValidationErrors.PAYLOAD_NOT_UNIQUE.errCode())) {
				throw new HashValidationException("Hash exists", validationDTO.getRejectionData(),
						validationDTO.getSourceMessageID(), msg.getDispatch().getMessageHeader().getTestFlag().value(),
						validationDTO.getMessageType(), validationDTO.getPrimaryRepositoryID());
			} else if (TPDErrorMessages.Failed_To_Find_AUID_In_Lookup_Table.errorCode().equals(ex.getErrCode())
					|| TPDErrorMessages.Failed_To_Find_UID_Lookup_Table.errorCode().equals(ex.getErrCode())
					|| TPDErrorMessages.Failed_To_Find_EOID_In_EconomicOperator_Table.errorCode().equals(ex.getErrCode())
					|| TPDErrorMessages.Failed_To_Find_FID_In_Facility_Table.errorCode().equals(ex.getErrCode())) {

				// ---------
				// Put the request payload JSON message in Kafka error topic and record the
				// failed event in metrics db
				// ---------
				eventStartTime = System.currentTimeMillis();
				publishCanonicalMsgService.sendMessageToErrorTopic(msgType, primaryRepositoryId, clientAppName, jsonMsg, 
						ingestMessageHelper.getMessageChannel(messageChannelMap, TPDMessageChannelTypes.Ingest_BAT_Error.msgChannelType()));
				publishMetricsEventMsgService.sendMetricsEventMsg(TPDMessageTypes.Dispatch.getMsgType(),
						primaryRepositoryId, clientAppName, MetricsEventTypes.Produce_Ingest_Main.eventType(),
						eventStartTime, System.currentTimeMillis(), MetricsEventTypes.Produce_Ingest_Main.serviceType(),
						"", MetricsEventTypes.Produce_Ingest_Main.errorCode(),
						String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()), "",
						TPDException.class.getName() + "||" + ex.getErrMsg());
			}

			throw new Exception(ex.getErrMsg());
		} catch (BadRequestException ex) {
			String errorDesc = MetricsEventTypes.Technical_Validation_400.errorDesc();

			if (!validationDTO.getRejectionData().getErrors().isEmpty())
				errorDesc = MetricsUtil.multMetricErrCode(validationDTO.getRejectionData().getErrors());

			publishMetricsEventMsgService.sendMetricsEventMsg(TPDMessageTypes.Dispatch.getMsgType(),
					primaryRepositoryId, clientAppName, MetricsEventTypes.Technical_Validation_400.eventType(),
					validationStartTime, validationEndTime, MetricsEventTypes.Technical_Validation_400.serviceType(),
					"", MetricsEventTypes.Technical_Validation_400.errorCode(),
					BusinessValidationRespStatusCode.BadRequestValidation.respStatusCode(), "", errorDesc);
			throw ex;

		} catch (Exception ex) {
			eventStartTime = System.currentTimeMillis();
			String errorMessage = TPDLogMessageConstructUtil
					.errorMessage(TPDErrorMessages.Generic_Exception_Occurred.errorCode(),
							TPDErrorMessages.Generic_Exception_Occurred.severity(), serviceName, methodName, msgType,
							primaryRepositoryId, null, TPDErrorMessages.Generic_Exception_Occurred.errorMessage(),
							ex.getMessage() + " Total time taken(ms) to process the request: "
									+ (System.currentTimeMillis() - processingStartTimeInMs),
							jsonMsg, ex.getStackTrace());
			pdbLogger.error(errorMessage);
			raiseIssue(primaryRepositoryId, TPDMessageTypes.Dispatch.getMsgType(), timeStamp,
					TPDIssueTypes.Failed_To_Write_To_IngestMain.getIssueId(),
					TPDErrorMessages.Generic_Exception_Occurred.errorCode(), 0,
					TPDErrorMessages.Generic_Exception_Occurred.errorMessage(), "false", "Error",
					TPDStageTypes.Ingest.getStageId());

			// ---------
			// Put the request payload JSON message in Kafka error topic and record the
			// failed event in metrics db
			// ---------
			publishCanonicalMsgService.sendMessageToErrorTopic(msgType, primaryRepositoryId, clientAppName, jsonMsg, 
					ingestMessageHelper.getMessageChannel(messageChannelMap, TPDMessageChannelTypes.Ingest_BAT_Error.msgChannelType()));
			publishMetricsEventMsgService.sendMetricsEventMsg(TPDMessageTypes.Dispatch.getMsgType(),
					primaryRepositoryId, clientAppName, MetricsEventTypes.Produce_Ingest_Main.eventType(),
					eventStartTime, System.currentTimeMillis(), MetricsEventTypes.Produce_Ingest_Main.serviceType(), "",
					MetricsEventTypes.Produce_Ingest_Main.errorCode(),
					String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()), "",
					Exception.class.getName() + "||" + TPDErrorMessages.Generic_Exception_Occurred.errorMessage());

			throw ex;
		}
    	
    	pdbLogger.info( TPDLogMessageConstructUtil.infoSuccessMessage(serviceName, methodName, msgType, primaryRepositoryId,  
    			TPDInfoMessages.Message_Processed_Successfully_In_Ingest_Layer.infoMessage() 
    			+ " Total time taken(ms) to process the request: " + (System.currentTimeMillis() - processingStartTimeInMs)));

    	return new SuccessResponse(statusCode, statusDesc);
    }
    
	/**
	 * This method is used to capture the change in the state of upUIs and aUIs.
	 * 
	 * @param msg
	 * @param apiInitiatedTimeInMs
	 * @param validationDTO
	 * @return The object of StateMachine
	 */
	private StateMachine changeState(final Dispatch33INTERNALAPIC msg, long apiInitiatedTimeInMs, final ValidationDTO validationDTO) {
		String methodName = "changeState(Dispatch33INTERNALAPIC, long, ValidationDTO)";
		pdbLogger.debug(TPDLogMessageConstructUtil.infoStartMessage(serviceName, methodName));
		
		StateMachine stateMachine = new StateMachine();
		stateMachine.setSourceMessageID(validationDTO.getSourceMessageID());
		stateMachine.setPrimaryRepositoryID(validationDTO.getPrimaryRepositoryID());
		stateMachine.setMessageType(TPDMessageTypes.Dispatch.getMsgType());
		stateMachine.setClientAppName(msg.getDispatch().getMessageHeader().getClientAppName());
		stateMachine.setStatusCode(validationDTO.getStatusCode());
		stateMachine.setFID(msg.getDispatch().getFid());
		stateMachine.setDispatchedUIType(msg.getDispatch().getUiType().value());
		if (TPDDispatchedUITypes.UPUI.getCode().equals(msg.getDispatch().getUiType().value())
				|| TPDDispatchedUITypes.Both.getCode().equals(msg.getDispatch().getUiType().value())) {
			// derive the upUI(s) from upUI(L)
			List<String> shortUPUIs = new ArrayList<String>();
			Map<String, String> longUPUIMap = new HashMap<String, String>();
			validationDTO.getFullLengthIDs().stream().forEach(x -> {
				shortUPUIs.add(ApiStringUtil.truncateFullLengthId(x, TPDMessageTypes.Dispatch.getMsgType()));
				longUPUIMap.put(ApiStringUtil.truncateFullLengthId(x, TPDMessageTypes.Dispatch.getMsgType()), x);
			});
			// add the SeqValFailedUPUIs to the list of shortUPUIs
			validationDTO.getSeqValFailedUPUIs().stream().forEach(x -> {
				shortUPUIs.add(ApiStringUtil.truncateFullLengthId(x, TPDMessageTypes.Dispatch.getMsgType()));
				longUPUIMap.put(ApiStringUtil.truncateFullLengthId(x, TPDMessageTypes.Dispatch.getMsgType()), x);
			});
			stateMachine.setUpUIs(shortUPUIs);
			stateMachine.setLongUPUIs(longUPUIMap);
		} 
		if (TPDDispatchedUITypes.AUI.getCode().equals(msg.getDispatch().getUiType().value())
				|| TPDDispatchedUITypes.Both.getCode().equals(msg.getDispatch().getUiType().value())) {
			stateMachine.setAUIs(validationDTO.getAggregatedUID2());
			// add the SeqValFailedAUIs to the list of aUIs
			stateMachine.getAUIs().addAll(validationDTO.getSeqValFailedAUIs());
		}
		stateMachine.setApiInitiatedTime(new Timestamp(apiInitiatedTimeInMs));
		stateMachine.setSeqValFailedAUIs(validationDTO.getSeqValFailedAUIs());
		stateMachine.setSeqValFailedUPUIs(validationDTO.getSeqValFailedUPUIs());
		stateMachine.setAUIDTOs(validationDTO.getAuiMap());
		stateMachine.setUpUIDTOs(validationDTO.getUpuiMap());
		stateMachine.setMessageTimeLong(validationDTO.getMessageTimeLong());
		// If Router messages are excluded from transmit sequencing control then the
		// antecedent PRIDs are not populated in MSGSEQREL table
		if (!(StringUtils.isNotEmpty(msg.getDispatch().getMessageHeader().getClientAppName())
				&& !CollectionUtils.isEmpty(commonConfigProps.getTxMsgSeqValdnExcludedMsgSenders())
				&& commonConfigProps.getTxMsgSeqValdnExcludedMsgSenders().contains(TPDConstants.SenderSystems.Router))) {
			stateMachine.setAntecedentPRIDs(validationDTO.getAntecedentPRIDs());
		}
		stateMachine = stateMachineService.setDispatched(stateMachine);
		
		pdbLogger.debug(TPDLogMessageConstructUtil.infoEndMessage(serviceName, methodName));
		return stateMachine;
	}
    
    /**
     * This method used to populate map of messageChannels used to push message to Kafka topic
     * 
     *@return	Map<String, MessageChannel>
     */
    private Map<String, MessageChannel> setMessageChannelMap() {
    	
		Map<String, MessageChannel> messageChannelMap = new HashMap<String, MessageChannel>();
		
		//Putting Operational channels
    	messageChannelMap.put(TPDMessageChannelTypes.Ingest_BAT_Operational.msgChannelType(),  messageStreams.dispatchOutboundChannel());
    	messageChannelMap.put(TPDMessageChannelTypes.Ingest_BAT_Error.msgChannelType(),
				messageStreams.dispatchDLQOutboundChannel());
    	
    	return messageChannelMap;
    }
}

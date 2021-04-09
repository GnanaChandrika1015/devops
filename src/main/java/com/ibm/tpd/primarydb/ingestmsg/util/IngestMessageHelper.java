package com.ibm.tpd.primarydb.ingestmsg.util;

import java.sql.Timestamp;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.ibm.tpd.primarydb.canonical.dto.Dispatch33INTERNALAPIC;
import com.ibm.tpd.primarydb.canonical.dto.ValidationDTO;
import com.ibm.tpd.primarydb.dto.Entity;
import com.ibm.tpd.primarydb.logger.PDBLogger;
import com.ibm.tpd.primarydb.util.TPDConstants;
import com.ibm.tpd.primarydb.util.TPDDispatchedUITypes;
import com.ibm.tpd.primarydb.util.TPDServiceNames;
import com.ibm.tpd.primarydb.util.TimestampUtil;

/**
 * @author SangitaPal
 *
 */
@Component
public class IngestMessageHelper {
	
	final String serviceName = TPDServiceNames.Dispatch_Ingest_Service.serviceName();
	
	@Autowired
	PDBLogger pdbLogger;



	/**
     * Method to get messageChannel for specific messageChannelType and messageType.
     * 
     *@param 	messageChannelMap 
     *@param 	messageChannelType
     *@param 	messageType
     *@return	MessageChannel
     */
	public MessageChannel getMessageChannel(final Map<String, MessageChannel> messageChannelMap, final String messageChannelType) {
		MessageChannel messageChannel = messageChannelMap.get(messageChannelType);
		
		return messageChannel;
	}
	
	/**
	 * <p>
	 * Populates the validation Data object from the input message. 
	 * </p>
	 * 
	 * @param Dispatch33INTERNALAPIC   The object of input message to be processed.
	 * @return ValidationDTO           Populated ValidationDTO
	 */
	public ValidationDTO populateValidationObj(Dispatch33INTERNALAPIC dispatchMsg) {
		ValidationDTO validationDTO = new ValidationDTO();
		int aggType = dispatchMsg.getDispatch().getUiType().value().intValue();

		if (aggType == TPDDispatchedUITypes.UPUI.getCode().intValue()
				|| aggType == TPDDispatchedUITypes.Both.getCode().intValue()) {
			validationDTO.getFullLengthIDs().addAll(dispatchMsg.getDispatch().getUpUIs());
		}

		if (aggType == TPDDispatchedUITypes.AUI.getCode().intValue()
				|| aggType == TPDDispatchedUITypes.Both.getCode().intValue()) {
			validationDTO.getAggregatedUID2().addAll(dispatchMsg.getDispatch().getaUIs());
		}
		validationDTO.setSourceMessageID(dispatchMsg.getDispatch().getMessageHeader().getSourceMessageID());
		validationDTO.setTestFlag(dispatchMsg.getDispatch().getMessageHeader().getTestFlag().value());
		validationDTO.setMessageType(dispatchMsg.getDispatch().getMessageType().toString());
		validationDTO.setPrimaryRepositoryID(dispatchMsg.getDispatch().getMessageHeader().getPrimaryRepositoryID());

		String apiInitiatedTime = dispatchMsg.getDispatch().getMessageHeader().getApiInitiatedTime();
		long apiInitiatedTimeInMs = TimestampUtil.convertUTCTimeStampToMilleSeconds(apiInitiatedTime);
		validationDTO.setApiInitiatedTime(new Timestamp(apiInitiatedTimeInMs));
		validationDTO.setFacilityId(dispatchMsg.getDispatch().getFid());

		String eventTime = dispatchMsg.getDispatch().getEventTime();
		validationDTO.setEventTimeStr(eventTime);
		if (eventTime != null && !eventTime.isEmpty()) {
			validationDTO.setEventTimeStr(eventTime);
			validationDTO.setEventTime(new Timestamp(TimestampUtil.convertEventTimeToMilleSeconds(eventTime)));
		}
		validationDTO.setMessageTimeLong(new Timestamp(
				TimestampUtil.convertUTCTimeStampToSeconds(dispatchMsg.getDispatch().getMessageTimeLong())));

		validationDTO.setAggType(dispatchMsg.getDispatch().getUiType().value());

		if (StringUtils.isNotEmpty(dispatchMsg.getDispatch().getMessageHeader().getClientAppName())) {
			validationDTO.setMessageSender(TPDConstants.SenderSystems.Router);
		}

		// populate EOIDs
		Entity entity = new Entity(dispatchMsg.getDispatch().getEoid());
		entity.setMsgFieldName("eoid");
		entity.setOwner(TPDConstants.EntityOwner.Manufacturer);
		validationDTO.getEOIDs().put(entity.getID(), entity);

		// populate FIDs
		entity = new Entity(dispatchMsg.getDispatch().getFid());
		entity.setMsgFieldName("fid");
		entity.setOwner(TPDConstants.EntityOwner.Manufacturer);
		validationDTO.getFIDs().put(entity.getID(), entity);
		
		// 2 – EU destination other than VM – fixed quantity
		if (StringUtils.isNotEmpty(dispatchMsg.getDispatch().getDestinationID2())) {
			// Destination facility identifier code
			entity = new Entity(dispatchMsg.getDispatch().getDestinationID2());
			entity.setMsgFieldName("destinationID2");
			entity.setOwner(TPDConstants.EntityOwner.External);
			// add the destinationID2 to the Map of FIDs only if it doesn't exist
			if (!validationDTO.getFIDs().containsKey(entity.getID())) {
				validationDTO.getFIDs().put(entity.getID(), entity);
			} else {
				validationDTO.setDestinationID2(entity);
			}
		}
		
		// 3 – EU VM(s)
		if (!CollectionUtils.isEmpty(dispatchMsg.getDispatch().getDestinationID3())) {
			// Destination facility identifier code(s) – possible multiple vending machines
			for (String fid : dispatchMsg.getDispatch().getDestinationID3()) {
				entity = new Entity(fid);
				entity.setMsgFieldName("destinationID3");
				entity.setOwner(TPDConstants.EntityOwner.External);
				// add the destinationID3 to the Map of FIDs only if it doesn't exist
				if (!validationDTO.getFIDs().containsKey(entity.getID())) {
					validationDTO.getFIDs().put(entity.getID(), entity);
				} else {
					validationDTO.getDestinationID3().put(entity.getID(), entity);
				}
			}
		}
		
		// 4 – EU destination other than VM – delivery with VV
		if (!CollectionUtils.isEmpty(dispatchMsg.getDispatch().getDestinationID4())) {
			// Destination id facility codes
			for (String fid : dispatchMsg.getDispatch().getDestinationID4()) {
				entity = new Entity(fid);
				entity.setMsgFieldName("destinationID4");
				entity.setOwner(TPDConstants.EntityOwner.External);
				// add the destinationID3 to the Map of FIDs only if it doesn't exist
				if (!validationDTO.getFIDs().containsKey(entity.getID())) {
					validationDTO.getFIDs().put(entity.getID(), entity);
				} else {
					validationDTO.getDestinationID4().put(entity.getID(), entity);
				}
			}
		}

		return validationDTO;
	}
	
}// end of class
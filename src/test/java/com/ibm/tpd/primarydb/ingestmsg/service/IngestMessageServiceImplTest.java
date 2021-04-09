package com.ibm.tpd.primarydb.ingestmsg.service;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.cql.CqlTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.tpd.config.CommonConfigProperties;
import com.ibm.tpd.config.ICVServiceConfigProperties;
import com.ibm.tpd.primarydb.canonical.dto.Dispatch33INTERNALAPIC;
import com.ibm.tpd.primarydb.canonical.dto.Error;
import com.ibm.tpd.primarydb.canonical.dto.RejectionData;
import com.ibm.tpd.primarydb.canonical.dto.ValidationDTO;
import com.ibm.tpd.primarydb.dto.PrimaryRepositoryID_LookUp;
import com.ibm.tpd.primarydb.dto.StateMachine;
import com.ibm.tpd.primarydb.entity.response.SuccessResponse;
import com.ibm.tpd.primarydb.exception.BadRequestException;
import com.ibm.tpd.primarydb.exception.HashValidationException;
import com.ibm.tpd.primarydb.exception.StateMachineOperationException;
import com.ibm.tpd.primarydb.exception.TPDException;
import com.ibm.tpd.primarydb.ingestmsg.util.IngestMessageHelper;
import com.ibm.tpd.primarydb.logger.PDBLogger;
import com.ibm.tpd.primarydb.statemachine.service.StateMachineService;
import com.ibm.tpd.primarydb.util.BusinessRuleValidationErrors;
import com.ibm.tpd.primarydb.util.TPDStatusCodeConstants;
import com.ibm.tpd.services.PersistValidId;
import com.ibm.tpd.services.PublishCanonicalMsgService;
import com.ibm.tpd.services.PublishMetricsEventMsgService;
import com.ibm.tpd.utils.EntityRegistryManager;
import com.ibm.tpd.utils.PopulateValDTOWithAUIData;
import com.ibm.tpd.utils.PopulateValDTOWithUPUIData;
import com.ibm.tpd.utils.RecallValidationUtil;
import com.ibm.tpd.utils.TPDLedgerMessageStreams;
import com.ibm.tpd.utils.TPDValidationUtility;
import com.ibm.tpd.utils.TpdUtility;
import com.ibm.tpd.validation.ValidationHandler;

/**
 * @author SangitaPal
 *
 */



 

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {IngestMessageServiceImpl.class, TestConfiguration.class})
@ContextConfiguration

@TestPropertySource(locations = "classpath:application-test.properties")
public class IngestMessageServiceImplTest {
	
	public static final String VALID_MSG_INPUT_FILE_NAME="dispatch-3-3.json";
	
	static String validJsonMessage="";
	static String msgType = "3-3";
	static String primaryRepositoryId = "1234567-1234-456-463746384";
	static SuccessResponse expectedResponse = null;
	static long processingStartTimeInMs = System.currentTimeMillis();
	private Dispatch33INTERNALAPIC validJsonMessageObj;
	
	@Value("${kafka.producer.success.status.code}")
	private String statusCode;

	@Value("${kafka.producer.success.status.desc}")
	private String statusDesc;
	
	
	@MockBean
	PDBLogger pdbLogger;
	
	@SpyBean
	IngestMessageServiceImpl ingestMessageService;
	
	@MockBean
	TPDOutboundMessageStreams messageStreams;
	
	@MockBean
	RecallValidationUtil recallValidationUtil;
	
	@MockBean
	TPDLedgerMessageStreams ledgerStreams;
	
	@MockBean
	TpdUtility tpdUtility;
	
	@MockBean
	TPDValidationUtility validationUtility;
	
	@MockBean
	PersistValidId persistValidId;
		
	@Autowired
	PopulateValDTOWithUPUIData populateValDTOWithUPUIData;
	@Autowired
	PopulateValDTOWithAUIData populateValDTOWithAUIData;
	
	@Autowired
	CqlTemplate cqlTemplate;
	@MockBean
	CassandraOperations cassandraTemplate;
	
	@MockBean
	@Qualifier("searchPrimaryRepIDStatement")
	PreparedStatement searchPrimaryRepIDStatement;
	
		
	@MockBean
	@Qualifier("populateUpuiDTOStatement")
	PreparedStatement populateUpuiDTOStatement;
	
	@MockBean
	@Qualifier("populateAuiDTOStatement")
	PreparedStatement populateAuiDTOStatement;
	
	@MockBean
	@Qualifier("searchMsgHashStatement")
	PreparedStatement searchMsgHashStatement;
	
	@MockBean
	@Qualifier("fetchuidLookUpStatement")
	PreparedStatement fetchuidLookUpStatement;	
	
	@MockBean
	@Qualifier("fetchAppliedUidStatement")
	PreparedStatement fetchAppliedUidStatement;
	
	@MockBean
	@Qualifier("fetchEOIDStatement")
	PreparedStatement fetchEOIDStatement;
	
	@MockBean
	@Qualifier("fetchFIDStatement")
	PreparedStatement fetchFIDStatement;
	
	@MockBean
	@Qualifier("fetchMsgTypeOfAntecedentEventByPRID")
	PreparedStatement fetchMsgTypeOfAntecedentEventByPRID;
	
	@MockBean
	@Qualifier("fetchTXStatusByPRID")
	PreparedStatement fetchTXStatusByPRID;
	
	@MockBean
	@Qualifier("fetchUIsDeliveredWithVV")
	PreparedStatement fetchUIsDeliveredWithVV;
	
	@MockBean
	@Qualifier("cqlTemplateForMsgTab")
	CqlTemplate cqlTemplateForMsgTab;
		
	@SpyBean
	IngestMessageHelper ingestMessageHelper;
	
	@MockBean
	PublishMetricsEventMsgService publishMetricsEventMsgService;
			
	@MockBean
	private StateMachineService stateMachineService;
				
	@MockBean
	ValidationHandler validationHandler;
	
	@MockBean
	PublishCanonicalMsgService publishCanonicalMsgService;
	
	@MockBean
	EntityRegistryManager entityRegistryManager;
	
	@SpyBean
	CommonConfigProperties commonConfigProps;
	
	@SpyBean
	ICVServiceConfigProperties icvSvcConfigProps;
	
	@BeforeClass
	public static void setup() {
				
		expectedResponse = new SuccessResponse();
		expectedResponse.setStatusCode("200");
		expectedResponse.setStatusDesc("Message pushed to Kafka topic successfully.");
		
	}
	
	
	@Before
	public void loadInputJson() {
		ClassLoader classLoader = new IngestMessageServiceImplTest().getClass().getClassLoader();
		File file = null;
		
		try {
			file = new File(classLoader.getResource(VALID_MSG_INPUT_FILE_NAME).getFile());
			if(file.exists()) {
				
				validJsonMessageObj = new ObjectMapper().readValue(file, Dispatch33INTERNALAPIC.class);
								
			}
			
		} catch (IOException e) {

			e.printStackTrace();
		}
		
	}
	
	/**
	 * Test for method validateBusinessRules when incorrect message type which is available in valid msg type list, then throws InvalidMessageTypeException
	 */
	@Test (expected=HashValidationException.class)
	public void sendMessageToKafkaTopic_whenDuplicateBATMessageReceived_thenThrowsHashValidationException() throws Exception {
		validJsonMessageObj.getDispatch().getMessageHeader().setClientAppName("");
		ValidationDTO returnDTO = new ValidationDTO();
		returnDTO.setMessageType("3-3");
		returnDTO.setRejectionData(new RejectionData());
		returnDTO.getRejectionData().setErrors(new ArrayList<Error>());
		Mockito.when(ingestMessageHelper.populateValidationObj(validJsonMessageObj)).thenReturn(returnDTO);
		
		Mockito.when(ingestMessageService.validateMessageHash(Mockito.anyString(),Mockito.anyString())).
				thenThrow(new TPDException(BusinessRuleValidationErrors.PAYLOAD_NOT_UNIQUE.errCode(),
						BusinessRuleValidationErrors.PAYLOAD_NOT_UNIQUE.validationControl() + " , " + BusinessRuleValidationErrors.PAYLOAD_NOT_UNIQUE.errDesc(),
						primaryRepositoryId));
		
		ingestMessageService.sendMessageToKafkaTopic("3-3", primaryRepositoryId, validJsonMessageObj, processingStartTimeInMs);
		
		
	}
	
	
	/**
	 * Test When Duplicate Router message received throws BadRequestException
	 */
	@Test (expected=BadRequestException.class)
	public void sendMessageToKafkaTopic_whenDuplicateRouterMessageReceived_thenThrowsBadRequestException() throws Exception {
		
		ValidationDTO returnDTO = new ValidationDTO();
		returnDTO.setMessageType("3-3");
		returnDTO.setRejectionData(new RejectionData());
		returnDTO.getRejectionData().setErrors(new ArrayList<Error>());
		Mockito.when(ingestMessageHelper.populateValidationObj(validJsonMessageObj)).thenReturn(returnDTO);
		
		Mockito.when(ingestMessageService.validatePrimaryRepositoryId(Mockito.anyString())).
				thenThrow(new TPDException(BusinessRuleValidationErrors.CODE_NOT_UNIQUE.errCode(),
						BusinessRuleValidationErrors.CODE_NOT_UNIQUE.validationControl() + " , " + BusinessRuleValidationErrors.CODE_NOT_UNIQUE.errDesc(),
						primaryRepositoryId));
		ingestMessageService.sendMessageToKafkaTopic("3-3", primaryRepositoryId, validJsonMessageObj, processingStartTimeInMs);
		
		
	}
	/**
	 * The Test method for Throwing BadRequestException when Technical Validation fails with the Json message
	 * @throws Exception
	 */
	@Test (expected=BadRequestException.class)
	public void sendMessageToKafkaTopic_whenTechnicalValidationFailed_ThrowsBadRequestException() throws Exception {
		ValidationDTO returnDTO = Mockito.spy(new ValidationDTO());
		returnDTO.setMessageType("3-3");
		returnDTO.setPrimaryRepositoryID(primaryRepositoryId);
		returnDTO.setRejectionData(new RejectionData());
		returnDTO.getRejectionData().setErrors(new ArrayList<Error>());
		
		returnDTO.getRejectionData().getErrors().add(new Error());
		
		Mockito.when(ingestMessageHelper.populateValidationObj(validJsonMessageObj)).then(Mockito.CALLS_REAL_METHODS);
		
		Mockito.when(validationUtility.isUniquePrimaryId(Mockito.eq(primaryRepositoryId), Mockito.any(Session.class),
				Mockito.any(PreparedStatement.class))).thenReturn(true);	
		Mockito.when(ingestMessageService.validatePrimaryRepositoryId(primaryRepositoryId)).thenReturn(true);
					
		Mockito.when(validationHandler.performJsonValidations(Mockito.any(ValidationDTO.class))).thenReturn(returnDTO);
		
		ingestMessageService.sendMessageToKafkaTopic("3-3", primaryRepositoryId, validJsonMessageObj, processingStartTimeInMs);
		
	}
	/**
	 * The Test method to return Success Response when all Business & Technical Validations pass
	 */
	@Test 
	public void sendMessageToKafkaTopicAndReturnSuccessResponse_whenValidationPasses() throws Exception {
		
		StateMachine stateMachine = new StateMachine();
		ValidationDTO returnDTO = Mockito.spy(new ValidationDTO());
		returnDTO.setMessageType(Mockito.eq("3-3"));
		returnDTO.setPrimaryRepositoryID(Mockito.eq(primaryRepositoryId));
		returnDTO.setShortLengthIDs(new ArrayList<String>());
		returnDTO.setAggregatedUID2(new ArrayList<String>());
		
		Mockito.when(ingestMessageHelper.populateValidationObj(validJsonMessageObj)).then(Mockito.CALLS_REAL_METHODS);
				
		Mockito.when(validationUtility.isUniquePrimaryId(Mockito.eq(primaryRepositoryId), Mockito.any(Session.class),
				Mockito.any(PreparedStatement.class))).
				thenReturn(true);	
		Mockito.when(ingestMessageService.validatePrimaryRepositoryId(primaryRepositoryId)).thenReturn(true);
		
			
		Mockito.when(validationHandler.performJsonValidations(Mockito.any(ValidationDTO.class))).thenReturn(returnDTO);
		
		
		returnDTO.setShortLengthIDs(new ArrayList<String>());
		returnDTO.setAggregatedUID2(new ArrayList<String>());
		
		Mockito.when(ingestMessageService.populateValidationDTO_DBValues(Mockito.mock(ValidationDTO.class, 
				Mockito.RETURNS_DEEP_STUBS))).
				thenReturn(returnDTO);
		Mockito.when(validationHandler.performDBValidations(Mockito.any(ValidationDTO.class))).thenReturn(returnDTO);
		
		Mockito.when(stateMachineService.setDispatched(Mockito.any(StateMachine.class))).thenReturn(stateMachine);
	
		Mockito.when(ingestMessageService.insertPrimaryRepositoryId(
				Mockito.any(PrimaryRepositoryID_LookUp.class),Mockito.eq(""), Mockito.eq(msgType))).
				thenReturn(true);
		
		assertEquals(expectedResponse, ingestMessageService.sendMessageToKafkaTopic("3-3", primaryRepositoryId, validJsonMessageObj, processingStartTimeInMs));
		
	}
	
	/**
	 * The test method for throwing BusinessValidationFailed Exception when one of the Business validations fails
	 * @throws Exception
	 */
	//@Test (expected=BusinessValidationFailedException.class)
	public void sendMessageToKafkaTopic_whenBusinessValidationFails_thenThrowsException() throws Exception {
				
		ValidationDTO returnDTO = Mockito.spy(new ValidationDTO());
		returnDTO.setMessageType(Mockito.eq("3-3"));
		returnDTO.setPrimaryRepositoryID(Mockito.eq(primaryRepositoryId));
		
		ValidationDTO validationDTO = new ValidationDTO();
		validationDTO.setMessageType("3-3");
		validationDTO.setPrimaryRepositoryID(primaryRepositoryId);
		returnDTO.setMessageType(Mockito.eq("3-3"));
		returnDTO.setPrimaryRepositoryID(Mockito.eq(primaryRepositoryId));
		
		Mockito.when(ingestMessageHelper.populateValidationObj(validJsonMessageObj)).then(Mockito.CALLS_REAL_METHODS);
				
		Mockito.when(validationUtility.isUniquePrimaryId(Mockito.eq(primaryRepositoryId), Mockito.any(Session.class),
				Mockito.any(PreparedStatement.class))).
				thenReturn(true);	
		Mockito.when(ingestMessageService.validatePrimaryRepositoryId(primaryRepositoryId)).thenReturn(true);
		
		Mockito.when(validationHandler.performJsonValidations(Mockito.any(ValidationDTO.class))).thenReturn(returnDTO);
				
		returnDTO.setShortLengthIDs(new ArrayList<String>());
		returnDTO.setAggregatedUID2(new ArrayList<String>());
		validationDTO.getRejectionData().getErrors().add(new Error());
		Mockito.when(ingestMessageService.populateValidationDTO_DBValues(Mockito.mock(ValidationDTO.class, 
				Mockito.RETURNS_DEEP_STUBS))).
				thenReturn(validationDTO);
		
		Mockito.when(validationHandler.performDBValidations(Mockito.any(ValidationDTO.class))).
		thenReturn(validationDTO);
		
		ingestMessageService.sendMessageToKafkaTopic("3-3", primaryRepositoryId, validJsonMessageObj, processingStartTimeInMs);
		
	}
	
	/**
	 * The Test method for throwing StateMachineOperation Exception when StateMachine fails
	 * @throws Exception
	 */
	@Test (expected=StateMachineOperationException.class)
	public void sendMessageToKafkaTopic_whenStateMachineOperationFails_thenThrowsException() throws Exception {
		StateMachine stateMachine = new StateMachine();
		stateMachine.getRejectionData().getErrors().add(new Error());
		ValidationDTO returnDTO = Mockito.spy(new ValidationDTO());
		returnDTO.setMessageType(Mockito.eq("3-3"));
		returnDTO.setPrimaryRepositoryID(Mockito.eq(primaryRepositoryId));
		Mockito.when(ingestMessageHelper.populateValidationObj(validJsonMessageObj)).then(Mockito.CALLS_REAL_METHODS);
		Mockito.when(validationUtility.isUniquePrimaryId(Mockito.eq(primaryRepositoryId), Mockito.any(Session.class),
				Mockito.any(PreparedStatement.class))).
				thenReturn(true);	
		Mockito.when(ingestMessageService.validatePrimaryRepositoryId(primaryRepositoryId)).thenReturn(true);
		Mockito.when(validationHandler.performJsonValidations(Mockito.any(ValidationDTO.class))).thenReturn(returnDTO);
		returnDTO.setShortLengthIDs(new ArrayList<String>());
		returnDTO.setAggregatedUID2(new ArrayList<String>());
		returnDTO.setStatusCode(TPDStatusCodeConstants.SuccessStatus);
		Mockito.when(ingestMessageService.populateValidationDTO_DBValues(Mockito.mock(ValidationDTO.class,
				Mockito.RETURNS_DEEP_STUBS))).
				thenReturn(returnDTO);
		Mockito.when(validationHandler.performDBValidations(Mockito.any(ValidationDTO.class))).
		thenReturn(returnDTO);
		Mockito.when(stateMachineService.setDispatched(Mockito.any(StateMachine.class))).thenReturn(stateMachine);
		ingestMessageService.sendMessageToKafkaTopic("3-3", primaryRepositoryId, validJsonMessageObj, processingStartTimeInMs);
	}
}
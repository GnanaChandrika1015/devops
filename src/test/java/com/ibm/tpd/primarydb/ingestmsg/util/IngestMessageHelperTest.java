/*package com.ibm.tpd.primarydb.ingestmsg.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.thrift.transport.TTransportException;
import org.cassandraunit.CQLDataLoader;
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet;
import org.cassandraunit.spring.CassandraDataSet;
import org.cassandraunit.spring.CassandraUnit;
import org.cassandraunit.spring.CassandraUnitTestExecutionListener;
import org.cassandraunit.spring.EmbeddedCassandra;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.cassandra.core.cql.CqlTemplate;
import org.springframework.kafka.test.rule.KafkaEmbedded;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.ibm.tpd.primarydb.canonical.dto.ValidationDTO;
//import com.ibm.tpd.primarydb.ingestmsg.service.DatabaseTestConfigService;
import com.ibm.tpd.primarydb.logger.PDBLogger;



 * @author SangitaPal
 * 
 

@RunWith(SpringRunner.class)
//@SpringBootTest(classes = {IngestMessageHelper.class, DatabaseTestConfigService.class})
@TestExecutionListeners(
		  listeners = CassandraUnitTestExecutionListener.class,
		  mergeMode = MergeMode.MERGE_WITH_DEFAULTS
		)
@CassandraUnit
@CassandraDataSet(value = "uidlookup.cql", keyspace = "bat_tpd_pri_msg")
@EmbeddedCassandra
@TestPropertySource(locations = "classpath:application-test.properties")
public class IngestMessageHelperTest {
	
	public static final String VALID_MSG_INPUT_FILE_NAME="dispatch-3-3.json";
	public static final String MSG_WITH_DUPLICATE_ID_INPUT_FILE_NAME="dispatch-3-3-with-duplicate-UID.json";
	public static final String MSG_WITH_DUPLICATE_AGGREGATED_ID_INPUT_FILE_NAME="dispatch-3-3-with-duplicate-AggregatedUID.json";
	public static final String MSG_WITH_EMPTY_IDLIST_INPUT_FILE_NAME="dispatch-3-3-with-empty-UID.json";
	public static final String MSG_WITHOUT_IDLIST_INPUT_FILE_NAME="dispatch-3-3-without-UID.json";
	
	final static List<String> appliedIds = new ArrayList<String>();
	final static LocalDate today = LocalDate.now();
	
	static String msgType = "3-3";
	
	static String validJsonMessage="";
	static String jsonMsgWithNoIdList = "";
	static String jsonMsgWithEmptyIdList = "";
	static String jsonMsgWithDuplicateIds = "";
	static String jsonMsgWithDuplicateAggregatedIds = "";
	static ValidationDTO expectedObject=null;
	
	@MockBean
	PDBLogger pdbLogger;
	
	@Autowired
	CqlTemplate cqlTemplate;
	
	@SpyBean
	IngestMessageHelper ingestMessageHelper;

	@ClassRule
	public static KafkaEmbedded embeddedKafka = new KafkaEmbedded(1, true, "testTopic");
	
	@BeforeClass
	public static void setup() {
		System.setProperty("spring.kafka.bootstrap-servers", embeddedKafka.getBrokersAsString());
		System.setProperty("spring.cloud.stream.kafka.binder.brokers", embeddedKafka.getBrokersAsString());
		System.setProperty("spring.cloud.stream.kafka.binder.zkNodes", embeddedKafka.getZookeeperConnectionString());
		
		List<String> fullLengthUIs = new ArrayList<String>();
		
		fullLengthUIs.add("UPUI(L)119041401");
		fullLengthUIs.add("UPUI(L)219041401");
		fullLengthUIs.add("UPUI(L)319041401");
		
		List<String> aggregatedUIs = new ArrayList<String>();
		
		aggregatedUIs.add("AUI1");
		aggregatedUIs.add("AUI2");
		aggregatedUIs.add("AUI3");
		
		expectedObject = new ValidationDTO();
		expectedObject.setMessageType(msgType);
		expectedObject.setFullLengthIDs(fullLengthUIs);
		expectedObject.setAggregatedUID1(aggregatedUIs);
		expectedObject.setPrimaryRepositoryID("1234567-1234-456-463746384");
		expectedObject.setSourceMessageID("SOURCE_MSG_ID_9999");
		expectedObject.setTestFlag(1);
	//	expectedObject.setApplyTime(Timestamp.valueOf("2018-09-30 05:00:00.0"));
		
		loadInputJson();
	}
	
	@BeforeClass
	public static void startCassandraEmbedded()
			throws InterruptedException, TTransportException, ConfigurationException, IOException {

		EmbeddedCassandraServerHelper.startEmbeddedCassandra();
		Cluster cluster = new Cluster.Builder().addContactPoints("127.0.0.1").withPort(9142).build();
		   Session session = cluster.connect();
		   CQLDataLoader dataLoader = new CQLDataLoader(session);
		   dataLoader.load(new ClassPathCQLDataSet("uidlookup.cql", true, "bat_tpd_pri_msg"));
		Thread.sleep(5000);
	}
	
	public static void loadInputJson() {
		ClassLoader classLoader = new IngestMessageHelperTest().getClass().getClassLoader();
		File file = null;
		try {
			file = new File(classLoader.getResource(VALID_MSG_INPUT_FILE_NAME).getFile());
			if(file.exists())
				validJsonMessage = new String(Files.readAllBytes(file.toPath()));
			
			file = new File(classLoader.getResource(MSG_WITH_DUPLICATE_ID_INPUT_FILE_NAME).getFile());
			if(file.exists())
				jsonMsgWithDuplicateIds = new String(Files.readAllBytes(file.toPath()));
			
			file = new File(classLoader.getResource(MSG_WITH_DUPLICATE_AGGREGATED_ID_INPUT_FILE_NAME).getFile());
			if(file.exists())
				jsonMsgWithDuplicateAggregatedIds = new String(Files.readAllBytes(file.toPath()));
			
			file = new File(classLoader.getResource(MSG_WITHOUT_IDLIST_INPUT_FILE_NAME).getFile());
			if(file.exists())
				jsonMsgWithNoIdList = new String(Files.readAllBytes(file.toPath()));
			
			file = new File(classLoader.getResource(MSG_WITH_EMPTY_IDLIST_INPUT_FILE_NAME).getFile());
			if(file.exists())
				jsonMsgWithEmptyIdList = new String(Files.readAllBytes(file.toPath()));
			
		} catch (IOException e) {

			e.printStackTrace();
		}
	}
	
	
	
	*//**
	 * Test for method validateDuplicateUidWithinMessage when duplicate id exists in the msg then populates RejectionErrors
	 *//*
	@Test
	public void validateDuplicateUidWithinMessage_whenDuplicateExistsInIdList_thenPopulateRejectionErrors() throws Exception {
		
		Set<String> duplicateIds = new HashSet<String>();
		duplicateIds.add("UPUI(L)219041401");
		duplicateIds.add("UPUI(L)319041401");
		duplicateIds.add("UPUI(L)419041401");
		
		//ValidationDTO deserializedMsgObject = ingestMessageHelper.getDeserializedMsgObject(msgType, jsonMsgWithDuplicateIds);
		//ValidationDTO validatedMsg = ingestMessageHelper.validateDuplicateUidWithinMessage(msgType, deserializedMsgObject);
		assertEquals(1, validatedMsg.getRejectionData().getErrors().size());
		
		for (RejectionError error : validatedMsg.getRejectionData().getRejectionErrors()) {
			assertEquals(BusinessRuleValidationErrors.VAL_UI_MULT_MSG.errCode(), error.getErrorCode());
			assertEquals(BusinessRuleValidationErrors.VAL_UI_MULT_MSG.errDesc(), error.getErrorDescription());
			assertEquals(false, duplicateIds.contains(error.getErrorData()));
		}
	}
	
	*//**
	 * Test for method validateDuplicateUidWithinMessage when duplicate aggregated id exists in the msg then populates RejectionErrors
	 *//*
	@Test
	public void validateDuplicateUidWithinMessage_whenDuplicateExistsInAggregatedIdList_thenPopulateRejectionErrors() throws Exception {
		
		Set<String> duplicateIds = new HashSet<String>();
		duplicateIds.add("AUI1");
		duplicateIds.add("AUI3");
		
		ValidationDTO deserializedMsgObject = ingestMessageHelper.getDeserializedMsgObject(msgType, jsonMsgWithDuplicateAggregatedIds);
		ValidationDTO validatedMsg = ingestMessageHelper.validateDuplicateUidWithinMessage(msgType, deserializedMsgObject);
		assertEquals(2, validatedMsg.getRejectionData().getRejectionErrors().size());
		
		for (RejectionError error : validatedMsg.getRejectionData().getRejectionErrors()) {
			assertEquals(BusinessRuleValidationErrors.VAL_UI_MULT_MSG.errCode(), error.getErrorCode());
			assertEquals(BusinessRuleValidationErrors.VAL_UI_MULT_MSG.errDesc(), error.getErrorDescription());
			assertEquals(true, duplicateIds.contains(error.getErrorData()));
		}
	}
	
	*//**
	 * Test for method validateAndPopulateErrorsForDuplicateUIDCheck when no duplicate id exists in the msg then does not populate RejectionErrors
	 *//*
	@Test
	public void validateDuplicateUidWithinMessage_whenNoDuplicateExistsInIdList_thenDoesNotPopulateRejectionErrors() throws Exception {
		ValidationDTO deserializedMsgObject = ingestMessageHelper.getDeserializedMsgObject(msgType, validJsonMessage);
		ValidationDTO validatedMsg = ingestMessageHelper.validateDuplicateUidWithinMessage(msgType, deserializedMsgObject);
		assertEquals(0, validatedMsg.getRejectionData().getRejectionErrors().size());
	}
	
	*//**
	 * Test for method validateAndPopulateErrorsForDuplicateUIDCheck when empty idlist exists in the msg then does not populate RejectionErrors
	 *//*
	@Test
	public void validateDuplicateUidWithinMessage_whenEmptyIdListSpecifiedInMsg_thenDoesNotPopulateRejectionErrors() throws Exception {
		ValidationDTO deserializedMsgObject = ingestMessageHelper.getDeserializedMsgObject(msgType, jsonMsgWithEmptyIdList);
		ValidationDTO validatedMsg = ingestMessageHelper.validateDuplicateUidWithinMessage(msgType, deserializedMsgObject);
		assertEquals(0, validatedMsg.getRejectionData().getRejectionErrors().size());
	}
	
	*//**
	 * Test for method validateUPUIExistenceAndOrderDeactivated when Id does not exist then populates RejectionErrors
	 *//*
	@Test
	public void validateUPUIExistenceAndOrderDeactivated_whenIdDoesNotExist_thenPopulatesRejectionErrors() throws Exception {
		ValidationDTO validationDTO = new ValidationDTO();
		List<String> idsToValidate = new ArrayList<String>();
		idsToValidate.add("68911cae-1668-4e76-903a-6523a338a3aa");
		idsToValidate.add("68911cae-1668-4e76-903a-6523a338a3bb");
		idsToValidate.add("uid-4");
		idsToValidate.add("uid-1");
		idsToValidate.add("uid-2");
		idsToValidate.add("uid-3");
		idsToValidate.add("uid-5");
		
		validationDTO.setShortLengthIDs(idsToValidate);
		
		validationDTO.getShortUidLongUidMapping().put("68911cae-1668-4e76-903a-6523a338a3aa","68911cae-1668-4e76-903a-6523a338a3aa19041401");
		validationDTO.getShortUidLongUidMapping().put("68911cae-1668-4e76-903a-6523a338a3bb","68911cae-1668-4e76-903a-6523a338a3bb19041401");
		validationDTO.getShortUidLongUidMapping().put("uid-4","uid-419041401");
		validationDTO.getShortUidLongUidMapping().put("uid-1","uid-119041401");
		validationDTO.getShortUidLongUidMapping().put("uid-2","uid-219041401");
		validationDTO.getShortUidLongUidMapping().put("uid-3","uid-319041401");
		validationDTO.getShortUidLongUidMapping().put("uid-5","uid-519041401");
		
		validationDTO.setApplyTime(Timestamp.valueOf(today.atStartOfDay()));
		
		ingestMessageHelper.validateUPUIExistenceAndOrderDeactivated(msgType, validationDTO, cqlTemplate);
		
		assertEquals(1, validationDTO.getRejectionData().getRejectionErrors().size());
		assertEquals(BusinessRuleValidationErrors.VAL_UI_EXIST_TIME.errCode(), validationDTO.getRejectionData().getRejectionErrors().get(0).getErrorCode());
		assertEquals(BusinessRuleValidationErrors.VAL_UI_EXIST_TIME.errDesc(), validationDTO.getRejectionData().getRejectionErrors().get(0).getErrorDescription());
		assertNotNull(validationDTO.getRejectionData().getRejectionErrors().get(0).getErrorData());
	}
	
	
	*//**
	 * Test for method validateUPUIExistenceAndOrderDeactivated when already deactivated UID is being applied then populates RejectionErrors
	 *//*
	@Test
	public void validateUPUIExistenceAndOrderDeactivated_whenAlreadyDeactivatedIdIsPresentInMsg_thenPopulatesRejectionErrors() throws Exception {
		ValidationDTO validationDTO = new ValidationDTO();
		List<String> idsToValidate = new ArrayList<String>();
		idsToValidate.add("68911cae-1668-4e76-903a-6523a338a3fc");
		idsToValidate.add("68911cae-1668-4e76-903a-6523a338a3bb");
		idsToValidate.add("68911cae-1668-4e76-903a-6523a338gggg");
		idsToValidate.add("68911cae-1668-4e76-903a-6523a338a3ee");
		idsToValidate.add("68911cae-1668-4e76-903a-6523a338a3ll");
		
		validationDTO.setShortLengthIDs(idsToValidate);
		
		validationDTO.getShortUidLongUidMapping().put("68911cae-1668-4e76-903a-6523a338a3fc","68911cae-1668-4e76-903a-6523a338a3fc19041401");
		validationDTO.getShortUidLongUidMapping().put("68911cae-1668-4e76-903a-6523a338a3bb","68911cae-1668-4e76-903a-6523a338a3bb19041401");
		validationDTO.getShortUidLongUidMapping().put("68911cae-1668-4e76-903a-6523a338gggg","68911cae-1668-4e76-903a-6523a338gggg19041401");
		validationDTO.getShortUidLongUidMapping().put("68911cae-1668-4e76-903a-6523a338a3ee","68911cae-1668-4e76-903a-6523a338a3ee19041401");
		validationDTO.getShortUidLongUidMapping().put("68911cae-1668-4e76-903a-6523a338a3ll","68911cae-1668-4e76-903a-6523a338a3ll19041401");
		
		ingestMessageHelper.validateUPUIExistenceAndOrderDeactivated(msgType, validationDTO, cqlTemplate);
		
		assertEquals(1, validationDTO.getRejectionData().getRejectionErrors().size());
		assertEquals(BusinessRuleValidationErrors.VAL_UI_ORD_DEACTIVATED.errCode(), validationDTO.getRejectionData().getRejectionErrors().get(0).getErrorCode());
		assertEquals(BusinessRuleValidationErrors.VAL_UI_ORD_DEACTIVATED.errDesc(), validationDTO.getRejectionData().getRejectionErrors().get(0).getErrorDescription());
		assertEquals("68911cae-1668-4e76-903a-6523a338gggg19041401", validationDTO.getRejectionData().getRejectionErrors().get(0).getErrorData());
	}
	
	*//**
	 * Test for method validateUPUIExistenceAndOrderDeactivated when all of the IDs exist and not deactivated then does not populate RejectionErrors
	 *//*
	@Test
	public void validateUPUIExistenceAndOrderDeactivated_whenAllIdsExistAndNotDeactivated_thenDoesNotPopulateRejectionErrors() throws Exception {
		ValidationDTO validationDTO = new ValidationDTO();
		List<String> idsToValidate = new ArrayList<String>();
		idsToValidate.add("68911cae-1668-4e76-903a-6523a338a3fc");
		idsToValidate.add("68911cae-1668-4e76-903a-6523a338a3bb");
		idsToValidate.add("68911cae-1668-4e76-903a-6523a338a3dd");
		idsToValidate.add("68911cae-1668-4e76-903a-6523a338a3ee");
		idsToValidate.add("68911cae-1668-4e76-903a-6523a338a3ll");
		
		validationDTO.setShortLengthIDs(idsToValidate);
		validationDTO.setApplyTime(Timestamp.valueOf(today.atStartOfDay()));
		
		ingestMessageHelper.validateUPUIExistenceAndOrderDeactivated(msgType, validationDTO, cqlTemplate);
		
		assertEquals(0, validationDTO.getRejectionData().getRejectionErrors().size());
	}
	
	*//**
	 * Test for method validateAUIExistenceAndDeactivated when already deactivated AUI is referenced then populates RejectionErrors
	 *//*
	@Test
	public void validateAUIExistenceAndDeactivated_whenNonExistentdAuiIsPresentInMsg_thenPopulatesRejectionErrors() throws Exception {
		ValidationDTO validationDTO = new ValidationDTO();
		List<String> auisToValidate = new ArrayList<String>();
		auisToValidate.add("AUI68911cae-1668-4e76-903a-6523a338pppp");
		auisToValidate.add("AUI68911cae-1668-4e76-903a-6523a3384444");
		auisToValidate.add("AUI68911cae-1668-4e76-903a-6523a3385555");
		auisToValidate.add("AUI68911cae-1668-4e76-903a-6523a338wwww");
		auisToValidate.add("AUI68911cae-1668-4e76-903a-6523a338zzzz");
		
		validationDTO.setAggregatedUIDs(auisToValidate);
		
		ValidationDTO validatedMsg = ingestMessageHelper.validateAUIExistenceAndDeactivated(msgType, validationDTO, cqlTemplate);
		
		assertEquals(1, validationDTO.getRejectionData().getRejectionErrors().size());
		for (RejectionError error : validatedMsg.getRejectionData().getRejectionErrors()) {
			assertEquals(BusinessRuleValidationErrors.VAL_UI_EXIST_TIME.errCode(), error.getErrorCode());
			assertEquals(BusinessRuleValidationErrors.VAL_UI_EXIST_TIME.errDesc(), error.getErrorDescription());
		}
	}
	
	*//**
	 * Test for method validateAUIExistenceAndDeactivated when already deactivated AUI is referenced then populates RejectionErrors
	 *//*
	@Test
	public void validateAUIExistenceAndDeactivated_whenAlreadyDeactivatedAuiIsPresentInMsg_thenPopulatesRejectionErrors() throws Exception {
		ValidationDTO validationDTO = new ValidationDTO();
		List<String> auisToValidate = new ArrayList<String>();
		auisToValidate.add("AUI68911cae-1668-4e76-903a-6523a3385555");
		auisToValidate.add("AUI68911cae-1668-4e76-903a-6523a3384444");
		
		validationDTO.setAggregatedUIDs(auisToValidate);
		
		ingestMessageHelper.validateAUIExistenceAndDeactivated(msgType, validationDTO, cqlTemplate);
		
		assertEquals(1, validationDTO.getRejectionData().getRejectionErrors().size());
		assertEquals(BusinessRuleValidationErrors.VAL_UI_ORD_DEACTIVATED.errCode(), validationDTO.getRejectionData().getRejectionErrors().get(0).getErrorCode());
		assertEquals(BusinessRuleValidationErrors.VAL_UI_ORD_DEACTIVATED.errDesc(), validationDTO.getRejectionData().getRejectionErrors().get(0).getErrorDescription());
		assertEquals("AUI68911cae-1668-4e76-903a-6523a3385555", validationDTO.getRejectionData().getRejectionErrors().get(0).getErrorData());
	}
	
	*//**
	 * Test for method validateAUIExistenceAndDeactivated when all of the IDs exist and not deactivated then does not populate RejectionErrors
	 *//*
	@Test
	public void validateAUIExistenceAndDeactivated_whenAllIdsExistAndNotDeactivated_thenDoesNotPopulateRejectionErrors() throws Exception {
		ValidationDTO validationDTO = new ValidationDTO();
		List<String> auisToValidate = new ArrayList<String>();
		auisToValidate.add("AUI68911cae-1668-4e76-903a-6523a3381111");
		auisToValidate.add("AUI68911cae-1668-4e76-903a-6523a3382222");
		auisToValidate.add("AUI68911cae-1668-4e76-903a-6523a3383333");
		auisToValidate.add("AUI68911cae-1668-4e76-903a-6523a3384444");
		
		validationDTO.setAggregatedUIDs(auisToValidate);
		validationDTO.setApplyTime(Timestamp.valueOf(today.atStartOfDay()));
		
		ingestMessageHelper.validateAUIExistenceAndDeactivated(msgType, validationDTO, cqlTemplate);
		
		assertEquals(0, validationDTO.getRejectionData().getRejectionErrors().size());
	}
	
	*//**
	 * Test for method addValidationErrorsToMsgBody when validation error exists then add the error details to message body
	 *//*
	@Test
	public void addValidationErrorsToMsgBody_whenValidationErrorsExist_thenAddValidationErrorsToMsgBody() throws Exception {
		ValidationDTO errorMsgObject = new ValidationDTO();
		
		for (int errCnt = 0; errCnt < 5; errCnt++) {
			RejectionError duplicateUiError = new RejectionError();
			duplicateUiError.setErrorCode(BusinessRuleValidationErrors.VAL_UI_MULT_MSG.errCode());
			duplicateUiError.setErrorDescription(BusinessRuleValidationErrors.VAL_UI_MULT_MSG.errDesc());
			duplicateUiError.setErrorData("1234567890");
			
			errorMsgObject.getRejectionData().getRejectionErrors().add(duplicateUiError);
			
			RejectionError uiExpiredError = new RejectionError();
			uiExpiredError.setErrorCode(BusinessRuleValidationErrors.VAL_UI_EXPIRY.errCode());
			uiExpiredError.setErrorDescription(BusinessRuleValidationErrors.VAL_UI_EXPIRY.errDesc());
			uiExpiredError.setErrorData("1234567890");
			
			errorMsgObject.getRejectionData().getRejectionErrors().add(uiExpiredError);
		}
		
		String msgWithErrorDtls = ingestMessageHelper.addValidationErrorsToMsgBody(TPDMessageTypes.Dispatch.getMsgShortDesc(), 
				validJsonMessage, errorMsgObject);
		
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode root = objectMapper.readTree(msgWithErrorDtls);
		JsonNode node = root.path(TPDMessageTypes.Dispatch.getMsgShortDesc());
		JsonNode rejectionDataNode = node.path("rejectionData"); 
		ArrayNode rejectionErrorsNode = (ArrayNode)rejectionDataNode.path("errors"); 
		@SuppressWarnings("unchecked")
		ArrayList<RejectionError> rejectionErrors = objectMapper.treeToValue(rejectionErrorsNode, ArrayList.class);
		
		assertEquals(errorMsgObject.getRejectionData().getRejectionErrors().size(), rejectionErrors.size());
	}
	
	*//**
	 * Test for method addValidationErrorsToMsgBody when no validation error exists then does not add the error details to message body
	 *//*
	@Test
	public void addValidationErrorsToMsgBody_whenNoValidationErrorsExist_thenDoNoAddValidationErrorsToMsgBody() throws Exception {
		ValidationDTO errorMsgObject = new ValidationDTO();
		
		String msgWithoutErrorDtls = ingestMessageHelper.addValidationErrorsToMsgBody(TPDMessageTypes.Dispatch.getMsgShortDesc(), 
				validJsonMessage, errorMsgObject);
		
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode root = objectMapper.readTree(msgWithoutErrorDtls);
		JsonNode node = root.path(TPDMessageTypes.Dispatch.getMsgShortDesc()); 
		JsonNode rejectionDataNode = node.path("rejectionData"); 
		ArrayNode rejectionErrorsNode = (ArrayNode)rejectionDataNode.path("errors"); 
		@SuppressWarnings("unchecked")
		ArrayList<RejectionError> rejectionErrors = objectMapper.treeToValue(rejectionErrorsNode, ArrayList.class);
		
		assertEquals(0, rejectionErrors.size()); 
	}
}
*/
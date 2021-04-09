package com.ibm.tpd.primarydb.errorhandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.ibm.tpd.primarydb.canonical.dto.Error;
import com.ibm.tpd.primarydb.dto.BusinessValidationError;
import com.ibm.tpd.primarydb.entity.response.NonTpdUidExistsResponse;
import com.ibm.tpd.primarydb.entity.response.ValidationFailedResponse;
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
import com.ibm.tpd.primarydb.exception.PrimaryRepositoryIDValidationException;
import com.ibm.tpd.primarydb.exception.StateMachineOperationException;
import com.ibm.tpd.primarydb.util.TPDStatusCodeConstants;

/**
 * @author SangitaPal
 * 
 * This is custom exception handler class for TPD primary DB. 
 *
 */

@ControllerAdvice
@RestController
public class TPDResponseExceptionHandler extends ResponseEntityExceptionHandler {

	/**
	 * Method for handling exceptions occurred while pushing message to Kafka error topic.
	 * @param	WebRequest
	 * @param	KafkaErrorTopicFailureException
	 * @return	ResponseEntity<ErrorResponse>
	 *
	 */
	@ExceptionHandler(KafkaErrorTopicFailureException.class)
	public final ResponseEntity<ValidationFailedResponse> handleKafkaErrorTopicFailureException(KafkaErrorTopicFailureException ex, WebRequest request) {
		return new ResponseEntity<>(getExceptionResponse(request, ex.getPrimaryRepositoryID()), HttpStatus.INTERNAL_SERVER_ERROR);
	}
	
	/**
	 * Method for handling invaid json format related Exceptions.
	 * 
	 * @param	WebRequest
	 * @param	InvalidJsonException
	 * @return	ResponseEntity<ErrorResponse>		 
	 *
	 */
	@ExceptionHandler(InvalidJsonException.class)
	public final ResponseEntity<ValidationFailedResponse> handleInvalidJsonException(InvalidJsonException ex, WebRequest request) {
		return new ResponseEntity<>(getExceptionResponse(request, ex.getPrimaryRepositoryID()), HttpStatus.INTERNAL_SERVER_ERROR);
	}
	
	/**
	 * Method for handling invalid message type Exceptions.
	 * @param	WebRequest
	 * @param	InvalidMessageTypeException
	 * @return	ResponseEntity<ValidationFailedResponse>
	 *
	 */
	@ExceptionHandler(InvalidMessageTypeException.class)
	public final ResponseEntity<ValidationFailedResponse> handleInvalidMessageTypeException(InvalidMessageTypeException ex, WebRequest request) {
		return new ResponseEntity<>(getInvalidMessageTypeResponse(ex), HttpStatus.BAD_REQUEST);
	}
	
	/**
	 * Method for handling invalid message type Exceptions.
	 * @param	WebRequest
	 * @param	FailedToValidateBusinessRulesException
	 * @return	ResponseEntity<ErrorResponse>
	 *
	 */
	@ExceptionHandler(FailedToValidateBusinessRulesException.class)
	public final ResponseEntity<ValidationFailedResponse> handleFailedToValidateBusinessRulesException(FailedToValidateBusinessRulesException ex, 
			WebRequest request) {
		return new ResponseEntity<>(getFailedToValidateBusinessRulesExceptionResponse(ex), HttpStatus.INTERNAL_SERVER_ERROR);
	}
	
	/**
	 * Method for handling exceptions occurred while pushing message to Kafka error topic.
	 * @param	WebRequest
	 * @param	KafkaErrorTopicFailureException
	 * @return	ResponseEntity<ValidationFailedResponse>
	 *
	 */
	@ExceptionHandler(BusinessValidationFailedException.class)
	public final ResponseEntity<ValidationFailedResponse> handleBusinessValidationFailedException(BusinessValidationFailedException ex, WebRequest request) {
		return new ResponseEntity<>(getBusinessValidationFailedResponse(ex), HttpStatus.BAD_REQUEST);
	}

	/**
	 * Method for handling non-tpd uis
	 * @param	WebRequest
	 * @param	NonTpdUidExistsException
	 * @return	ResponseEntity<NonTpdUidExistsResponse>
	 *
	 */
	@ExceptionHandler(NonTpdUidExistsException.class)
	public final ResponseEntity<NonTpdUidExistsResponse> handleNonTpdUidExistsException(NonTpdUidExistsException ex, WebRequest request) {
		return new ResponseEntity<>(getNonTpdUidExistsResponse(ex), HttpStatus.NOT_ACCEPTABLE);
	}
	
	/**
	 * Method for handling exceptions occurred while pushing message to Kafka topic.
	 * @param	WebRequest
	 * @param	KafkaProducerException
	 * @return	ResponseEntity<ErrorResponse>
	 *
	 */
	@ExceptionHandler(KafkaProducerException.class)
	public final ResponseEntity<ValidationFailedResponse> handleKafkaErrorTopicFailureException(KafkaProducerException ex, WebRequest request) {
		return new ResponseEntity<>(getExceptionResponse(request, ex.getPrimaryRepositoryID()), HttpStatus.INTERNAL_SERVER_ERROR);
	}

	/**
	 * Method for handling non-tpd uis
	 * @param	WebRequest
	 * @param	ValidationFailedResponse
	 * @return	ResponseEntity<ValidationFailedResponse>
	 *
	 */
	@ExceptionHandler(InvalidUIDFormatException.class)
	public final ResponseEntity<ValidationFailedResponse> handleInvalidUIDFormatException(InvalidUIDFormatException ex, WebRequest request) {
		return new ResponseEntity<>(getBusinessValidationFailedResponse(ex), HttpStatus.BAD_REQUEST);
	}
	
	/**
	 * Method for generic Exceptions.
	 * 
	 * @param	WebRequest
	 * @param	Exception
	 * @return	ResponseEntity<ErrorResponse>
	 *
	 */
	@ExceptionHandler(Exception.class)
	public final ResponseEntity<ValidationFailedResponse> handleOtherExceptions(Exception ex, WebRequest request) {
		return new ResponseEntity<>(getExceptionResponse(request, null), HttpStatus.INTERNAL_SERVER_ERROR);
	}
	
	/**
	 * Method for handling Bad Request Response 
	 * @param	WebRequest
	 * @param	BadRequestException
	 * @return	ResponseEntity<ValidationFailedResponse>
	 *
	 */
	@ExceptionHandler(BadRequestException.class)
	public final ResponseEntity<ValidationFailedResponse> handleBadRequestException(BadRequestException ex, WebRequest request) {
		return new ResponseEntity<>(getBadRequestExceptionResponse(ex), HttpStatus.BAD_REQUEST);
	}
	
	/**
	 * Method for handling exceptions occurred during Primary RepositoryIDValidation
	 * @param	WebRequest
	 * @param	PrimaryRepositoryIDValidationException
	 * @return	ResponseEntity<ValidationFailedResponse>
	 *
	 */
	@ExceptionHandler(PrimaryRepositoryIDValidationException.class)
	public final ResponseEntity<ValidationFailedResponse> handlePrimaryRepositoryIDValidationException(PrimaryRepositoryIDValidationException ex, WebRequest request) {
		return new ResponseEntity<>(getPrimaryRepositoryIDValidationFailureResponse(ex), HttpStatus.BAD_REQUEST);
	}
	
	/**
	 * Method for handling exceptions occurred HashValidation
	 * @param	WebRequest
	 * @param	HashValidationException
	 * @return	ResponseEntity<ValidationFailedResponse>
	 *
	 */
	@ExceptionHandler(HashValidationException.class)
	public final ResponseEntity<ValidationFailedResponse> handleHashValidationException(HashValidationException ex, WebRequest request) {
		return new ResponseEntity<>(getHashValidationFailureResponse(ex), HttpStatus.BAD_REQUEST);
	}
	
	/**
	 * Method for handling State Machine related Exceptions.
	 * 
	 * @param	WebRequest
	 * @param	StateMachineOperationException
	 * @return	ResponseEntity<ErrorResponse>		 
	 *
	 */
	@ExceptionHandler(StateMachineOperationException.class)
	public final ResponseEntity<ValidationFailedResponse> handleStateMachineException(StateMachineOperationException ex, WebRequest request) {
		return new ResponseEntity<>(getExceptionResponse(request, ex.getPrimaryRepositoryID()), HttpStatus.INTERNAL_SERVER_ERROR);
	}
	
	/**
	 * Method for creating error response
	 * 
	 * @param request
	 * @param primaryRepoId
	 * 
	 */
	private ValidationFailedResponse getExceptionResponse(WebRequest request, String primaryRepoId) {

		ValidationFailedResponse validationFailedResponse = new ValidationFailedResponse();
		validationFailedResponse.setStatusCode(String.valueOf(TPDStatusCodeConstants.SystemErrorStatus));
		validationFailedResponse.setPrimaryRepositoryID(primaryRepoId);
		validationFailedResponse.setError(1);

		BusinessValidationError error = new BusinessValidationError();
		error.setRejectionErrorCode("SYSTEM_ERROR");
		error.setRejectionErrorDescription("Internal system error.");
		validationFailedResponse.getErrors().add(error);

		return validationFailedResponse;
	}
	
	/**
	 * Method for creating error response when business rule validation failed
	 *
	 */
	private ValidationFailedResponse getBusinessValidationFailedResponse(BusinessValidationFailedException ex) {
		
		ValidationFailedResponse validationFailedResponse = new ValidationFailedResponse();
		validationFailedResponse.setStatusCode(String.valueOf(ex.getStatusCode()));
		validationFailedResponse.setStatusDesc("Business validation failed.");
		validationFailedResponse.setSourceMessageID(ex.getSourceMessageID());
		validationFailedResponse.setTestFlag(ex.getTestFlag());
		validationFailedResponse.setMessageType(ex.getMessageType());
		validationFailedResponse.setPrimaryRepositoryID(ex.getPrimaryRepositoryID());
		validationFailedResponse.setError(1);
		
		for (Error rejectionError : ex.getRejectionData().getErrors()) {
			BusinessValidationError error = new BusinessValidationError();
			error.setRejectionErrorCode(rejectionError.getErrorCode());
			error.setRejectionErrorDescription(rejectionError.getErrorDescription());
			error.setRejectionErrorData(rejectionError.getErrorData());
			validationFailedResponse.getErrors().add(error);
		}
		
		return validationFailedResponse;
	}
	
	/**
	 * Method for creating error response when business rule validation failed
	 *
	 */
	private ValidationFailedResponse getInvalidMessageTypeResponse(InvalidMessageTypeException ex) {
		
		ValidationFailedResponse validationFailedResponse = new ValidationFailedResponse();
		validationFailedResponse.setStatusCode(String.valueOf(TPDStatusCodeConstants.BadRequestStatus));
		validationFailedResponse.setStatusDesc("Business validation failed.");
		validationFailedResponse.setSourceMessageID(ex.getSourceMessageID());
		validationFailedResponse.setTestFlag(ex.getTestFlag());
		validationFailedResponse.setMessageType(ex.getMessageType());
		validationFailedResponse.setPrimaryRepositoryID(ex.getPrimaryRepositoryID());
		validationFailedResponse.setError(1);
		BusinessValidationError rejectionError = new BusinessValidationError("INVALID_MESSAGE_TYPE", "The field Message_Type is out of the defined list.", ex.getMessageType());
		validationFailedResponse.getErrors().add(rejectionError);
		
		return validationFailedResponse;
	}

	/**
	 * Method for creating error response when business rule validation failed
	 *
	 */
	private ValidationFailedResponse getBusinessValidationFailedResponse(InvalidUIDFormatException ex) {
		
		ValidationFailedResponse validationFailedResponse = new ValidationFailedResponse();
		validationFailedResponse.setStatusCode(String.valueOf(TPDStatusCodeConstants.BadRequestStatus));
		validationFailedResponse.setStatusDesc("Failed to validate business rules.");
		validationFailedResponse.setSourceMessageID(ex.getSourceMessageID());
		validationFailedResponse.setTestFlag(ex.getTestFlag());
		validationFailedResponse.setMessageType(ex.getMessageType());
		validationFailedResponse.setPrimaryRepositoryID(ex.getPrimaryRepositoryID());
		validationFailedResponse.setError(1);
		
		for (Error rejectionError : ex.getRejectionData().getErrors()) {
			BusinessValidationError error = new BusinessValidationError();
			error.setRejectionErrorCode(rejectionError.getErrorCode());
			error.setRejectionErrorDescription(rejectionError.getErrorDescription());
			error.setRejectionErrorData(rejectionError.getErrorData());
			validationFailedResponse.getErrors().add(error);
		}
		
		return validationFailedResponse;
	}

	/**
	 * Method for creating warning response when there is no business rule validation failure and non-tpd uid exists in message
	 *
	 */
	private NonTpdUidExistsResponse getNonTpdUidExistsResponse(NonTpdUidExistsException ex) {
		
		NonTpdUidExistsResponse nonTpdUidExistsResponse = new NonTpdUidExistsResponse();
		nonTpdUidExistsResponse.setStatusCode("279");
		nonTpdUidExistsResponse.setStatusDesc("Non-TPD UID exists in message");
		nonTpdUidExistsResponse.setSourceMessageID(ex.getSourceMessageID());
		nonTpdUidExistsResponse.setTestFlag(ex.getTestFlag());
		nonTpdUidExistsResponse.setMessageType(ex.getMessageType());
		nonTpdUidExistsResponse.setPrimaryRepositoryID(ex.getPrimaryRepositoryID());
		nonTpdUidExistsResponse.setError(1);
		for (Error rejectionError : ex.getRejectionData().getErrors()) {
			BusinessValidationError error = new BusinessValidationError();
			error.setRejectionErrorCode(rejectionError.getErrorCode());
			error.setRejectionErrorDescription(rejectionError.getErrorDescription());
			error.setRejectionErrorData(rejectionError.getErrorData());
			nonTpdUidExistsResponse.getErrors().add(error);
		}
		
		return nonTpdUidExistsResponse;
	}
	
	/**
	 * Method for creating error response when primary repositoryID validation failed
	 *
	 */
	private ValidationFailedResponse getPrimaryRepositoryIDValidationFailureResponse(PrimaryRepositoryIDValidationException ex) {
		
		ValidationFailedResponse validationFailedResponse = new ValidationFailedResponse();
		validationFailedResponse.setStatusCode(String.valueOf(TPDStatusCodeConstants.BadRequestStatus));
		validationFailedResponse.setStatusDesc("PrimaryRepositoryID validation failed.");
		validationFailedResponse.setSourceMessageID(ex.getSourceMessageID());
		validationFailedResponse.setTestFlag(ex.getTestFlag());
		validationFailedResponse.setMessageType(ex.getMessageType());
		validationFailedResponse.setPrimaryRepositoryID(ex.getPrimaryRepositoryID());
		validationFailedResponse.setError(1);
		
		for (Error rejectionError : ex.getRejectionData().getErrors()) {
			BusinessValidationError error = new BusinessValidationError();
			error.setRejectionErrorCode(rejectionError.getErrorCode());
			error.setRejectionErrorDescription(rejectionError.getErrorDescription());
			error.setRejectionErrorData(rejectionError.getErrorData());
			validationFailedResponse.getErrors().add(error);
		}
		
		return validationFailedResponse;
	}
	
	/**
	 * Method for creating error response when hash validation failed
	 *
	 */
	private ValidationFailedResponse getHashValidationFailureResponse(HashValidationException ex) {
		
		ValidationFailedResponse validationFailedResponse = new ValidationFailedResponse();
		validationFailedResponse.setStatusCode(String.valueOf(TPDStatusCodeConstants.BadRequestStatus));
		validationFailedResponse.setStatusDesc("Hash validation failed.");
		validationFailedResponse.setSourceMessageID(ex.getSourceMessageID());
		validationFailedResponse.setTestFlag(ex.getTestFlag());
		validationFailedResponse.setMessageType(ex.getMessageType());
		validationFailedResponse.setPrimaryRepositoryID(ex.getPrimaryRepositoryID());
		validationFailedResponse.setError(1);
		
		for (Error rejectionError : ex.getRejectionData().getErrors()) {
			BusinessValidationError error = new BusinessValidationError();
			error.setRejectionErrorCode(rejectionError.getErrorCode());
			error.setRejectionErrorDescription(rejectionError.getErrorDescription());
			error.setRejectionErrorData(rejectionError.getErrorData());
			validationFailedResponse.getErrors().add(error);
		}
		
		return validationFailedResponse;
	}
	
	/**
	 * Method for creating error response for BAD Request - 400 
	 *
	 */
	private ValidationFailedResponse getBadRequestExceptionResponse(BadRequestException ex) {
		
		ValidationFailedResponse validationFailedResponse = new ValidationFailedResponse();
		validationFailedResponse.setStatusCode(String.valueOf(TPDStatusCodeConstants.BadRequestStatus));
		validationFailedResponse.setStatusDesc(ex.getMessage());
		validationFailedResponse.setSourceMessageID(ex.getSourceMessageID());
		validationFailedResponse.setTestFlag(ex.getTestFlag());
		validationFailedResponse.setMessageType(ex.getMessageType());
		validationFailedResponse.setPrimaryRepositoryID(ex.getPrimaryRepositoryID());
		validationFailedResponse.setError(1);
		
		for (Error rejectionError : ex.getRejectionData().getErrors()) {
			BusinessValidationError error = new BusinessValidationError();
			error.setRejectionErrorCode(rejectionError.getErrorCode());
			error.setRejectionErrorDescription(rejectionError.getErrorDescription());
			error.setRejectionErrorData(rejectionError.getErrorData());
			validationFailedResponse.getErrors().add(error);
		}
		
		return validationFailedResponse;
	}
	
	/**
	 * Composes the error response for FailedToValidateBusinessRulesException
	 * 
	 * @param exc The object of FailedToValidateBusinessRulesException
	 * @return
	 */
	private ValidationFailedResponse getFailedToValidateBusinessRulesExceptionResponse(FailedToValidateBusinessRulesException exc) {

		ValidationFailedResponse validationFailedResponse = new ValidationFailedResponse();
		validationFailedResponse.setStatusCode(String.valueOf(TPDStatusCodeConstants.SystemErrorStatus));
		validationFailedResponse.setMessageType(exc.getMessageType());
		validationFailedResponse.setPrimaryRepositoryID(exc.getPrimaryRepositoryID());
		validationFailedResponse.setError(1);

		BusinessValidationError error = new BusinessValidationError();
		error.setRejectionErrorCode("SYSTEM_ERROR");
		error.setRejectionErrorDescription(exc.getErrMsg());
		validationFailedResponse.getErrors().add(error);

		return validationFailedResponse;
	}
}
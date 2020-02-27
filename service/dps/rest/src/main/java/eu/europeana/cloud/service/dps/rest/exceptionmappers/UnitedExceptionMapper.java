package eu.europeana.cloud.service.dps.rest.exceptionmappers;

import eu.europeana.cloud.common.response.ErrorInfo;
import eu.europeana.cloud.service.dps.exception.AccessDeniedOrObjectDoesNotExistException;
import eu.europeana.cloud.service.dps.exception.AccessDeniedOrTopologyDoesNotExistException;
import eu.europeana.cloud.service.dps.exception.DpsTaskValidationException;
import eu.europeana.cloud.service.dps.exception.TopologyAlreadyExistsException;
import eu.europeana.cloud.service.dps.rest.exceptions.TaskSubmissionException;
import eu.europeana.cloud.service.dps.status.DpsErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.Response;

/**
 * Maps exceptions thrown by services to {@link ResponseBody}.
 */

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class UnitedExceptionMapper {

    private final static Logger LOGGER = LoggerFactory.getLogger(UnitedExceptionMapper.class);

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseBody public ErrorInfo handleNotFound(Exception e) {
        return new ErrorInfo(DpsErrorCode.OTHER.toString(),"HTTP 404 Not Found");
    }


    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ExceptionHandler(TopologyAlreadyExistsException.class)
    @ResponseBody public ErrorInfo handleTopologyAlreadyExistsException(Exception e) {
        return buildResponse(DpsErrorCode.TOPOLOGY_ALREADY_EXIST,e);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseBody public ErrorInfo handleConstraintViolationException(Exception exception) {
        return buildResponse(DpsErrorCode.BAD_REQUEST, exception);
    }


    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(RuntimeException.class)
    @ResponseBody public ErrorInfo handleRuntimeException(Exception exception) {
    	if (exception instanceof AccessDeniedException) {
            return buildResponse(DpsErrorCode.ACCESS_DENIED_OR_OBJECT_DOES_NOT_EXIST_EXCEPTION, exception);
    	}

        LOGGER.error("Unexpected error occured.", exception);
        return buildResponse( DpsErrorCode.OTHER, exception);
    }

    @ExceptionHandler(ResponseStatusException.class)
    @ResponseBody public ErrorInfo handleResponseStatusException(HttpServletResponse response,ResponseStatusException exception) {
        response.setStatus(exception.getStatus().value());
        return buildResponse(DpsErrorCode.OTHER, exception.getReason());
    }

    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ExceptionHandler(AccessDeniedOrTopologyDoesNotExistException.class)
    @ResponseBody public ErrorInfo handleAccessDeniedOrTopologyDoesNotExistException(Exception e) {
        return buildResponse(DpsErrorCode.ACCESS_DENIED_OR_TOPOLOGY_DOES_NOT_EXIST_EXCEPTION, e);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(DpsTaskValidationException.class)
    @ResponseBody public ErrorInfo handleDpsTaskValidationException(Exception e) {
        return buildResponse( DpsErrorCode.TASK_NOT_VALID, e);
    }


    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(TaskSubmissionException.class)
    @ResponseBody public ErrorInfo handleTaskSubmissionException(Exception e) {
        return buildResponse(DpsErrorCode.OTHER, e);
    }

    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ExceptionHandler(AccessDeniedOrObjectDoesNotExistException.class)
    @ResponseBody public ErrorInfo handleAccessDeniedOrObjectDoesNotExistException(Exception e) {
        return buildResponse(DpsErrorCode.ACCESS_DENIED_OR_OBJECT_DOES_NOT_EXIST_EXCEPTION, e);
    }
    
    private static ErrorInfo buildResponse(DpsErrorCode errorCode, Exception e) {
        return buildResponse(errorCode, e.getMessage());
    }

    private static ErrorInfo buildResponse(DpsErrorCode errorCode, String message) {
        return new ErrorInfo(errorCode.name(), message);
    }
}

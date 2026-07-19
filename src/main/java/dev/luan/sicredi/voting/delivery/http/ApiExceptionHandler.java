package dev.luan.sicredi.voting.delivery.http;

import dev.luan.sicredi.voting.service.error.AgendaNotFoundException;
import dev.luan.sicredi.voting.service.error.AssociateAlreadyVotedException;
import dev.luan.sicredi.voting.service.error.AssociateNotEligibleException;
import dev.luan.sicredi.voting.service.error.AssociateNotFoundException;
import dev.luan.sicredi.voting.service.error.EligibilityServiceUnavailableException;
import dev.luan.sicredi.voting.service.error.PersistenceUnavailableException;
import dev.luan.sicredi.voting.service.error.VotingApplicationException;
import dev.luan.sicredi.voting.service.error.VotingResultNotAvailableException;
import dev.luan.sicredi.voting.service.error.VotingSessionAlreadyExistsException;
import dev.luan.sicredi.voting.service.error.VotingSessionClosedException;
import dev.luan.sicredi.voting.service.error.VotingSessionNotFoundException;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
class ApiExceptionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);
  private static final URI ERROR_TYPE_BASE = URI.create("https://api.sicredi-voting.dev/errors/");

  @ExceptionHandler(VotingApplicationException.class)
  ProblemDetail handleApplicationError(VotingApplicationException exception) {
    HttpStatus status = statusFor(exception);
    if (status.is5xxServerError()) {
      LOGGER.error(
          "Application dependency failure with code {} and type {}",
          exception.errorCode(),
          exception.getClass().getSimpleName());
    }
    return problem(status, exception.errorCode(), exception.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail handleValidationError(MethodArgumentNotValidException exception) {
    ProblemDetail problem =
        problem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "The request contains invalid fields");
    List<FieldViolation> violations =
        exception.getBindingResult().getFieldErrors().stream().map(FieldViolation::from).toList();
    problem.setProperty("violations", violations);
    return problem;
  }

  @ExceptionHandler({
    IllegalArgumentException.class,
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class
  })
  ProblemDetail handleMalformedRequest(Exception exception) {
    return problem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "The request is invalid");
  }

  @ExceptionHandler(Exception.class)
  ProblemDetail handleUnexpectedError(Exception exception) {
    LOGGER.error("Unexpected API failure of type {}", exception.getClass().getSimpleName());
    return problem(
        HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred");
  }

  private static HttpStatus statusFor(VotingApplicationException exception) {
    if (exception instanceof AgendaNotFoundException) {
      return HttpStatus.NOT_FOUND;
    }
    if (exception instanceof AssociateNotEligibleException
        || exception instanceof AssociateNotFoundException) {
      return HttpStatus.UNPROCESSABLE_CONTENT;
    }
    if (exception instanceof EligibilityServiceUnavailableException
        || exception instanceof PersistenceUnavailableException) {
      return HttpStatus.SERVICE_UNAVAILABLE;
    }
    if (exception instanceof AssociateAlreadyVotedException
        || exception instanceof VotingResultNotAvailableException
        || exception instanceof VotingSessionAlreadyExistsException
        || exception instanceof VotingSessionClosedException
        || exception instanceof VotingSessionNotFoundException) {
      return HttpStatus.CONFLICT;
    }
    return HttpStatus.INTERNAL_SERVER_ERROR;
  }

  private static ProblemDetail problem(HttpStatus status, String code, String detail) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(titleFor(status));
    problem.setType(ERROR_TYPE_BASE.resolve(code.toLowerCase().replace('_', '-')));
    problem.setProperty("code", code);
    problem.setProperty("timestamp", Instant.now());
    return problem;
  }

  private static String titleFor(HttpStatus status) {
    return switch (status) {
      case BAD_REQUEST -> "Invalid request";
      case NOT_FOUND -> "Resource not found";
      case CONFLICT -> "Resource state conflict";
      case UNPROCESSABLE_CONTENT -> "Business rule violation";
      case SERVICE_UNAVAILABLE -> "Required dependency unavailable";
      default -> "Internal server error";
    };
  }

  private record FieldViolation(String field, String message) {

    static FieldViolation from(FieldError fieldError) {
      return new FieldViolation(fieldError.getField(), fieldError.getDefaultMessage());
    }
  }
}

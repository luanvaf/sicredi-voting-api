package dev.luan.sicredi.voting.delivery.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import dev.luan.sicredi.voting.domainmodel.AgendaId;
import dev.luan.sicredi.voting.domainmodel.AssociateCpf;
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
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class ApiExceptionHandlerTest {

  private static final AgendaId AGENDA_ID = AgendaId.from("29dc7f35-4472-47f5-9144-7aaf64fef846");
  private static final AssociateCpf ASSOCIATE_CPF = new AssociateCpf("93541134780");

  private final ApiExceptionHandler handler = new ApiExceptionHandler();

  @ParameterizedTest
  @MethodSource("applicationErrors")
  void shouldMapApplicationErrors(
      VotingApplicationException exception, HttpStatus status, String title) {
    ProblemDetail problem = handler.handleApplicationError(exception);

    assertThat(problem.getStatus()).isEqualTo(status.value());
    assertThat(problem.getTitle()).isEqualTo(title);
    assertThat(problem.getDetail()).isEqualTo(exception.getMessage());
    assertThat(problem.getType().toString())
        .endsWith(exception.errorCode().toLowerCase().replace('_', '-'));
    assertThat(problem.getProperties())
        .containsEntry("code", exception.errorCode())
        .hasEntrySatisfying("timestamp", value -> assertThat(value).isInstanceOf(Instant.class));
  }

  @Test
  void shouldIncludeFieldViolationsInValidationErrors() {
    BeanPropertyBindingResult bindingResult =
        new BeanPropertyBindingResult(new Object(), "request");
    bindingResult.addError(new FieldError("request", "title", "must not be blank"));
    MethodArgumentNotValidException exception =
        new MethodArgumentNotValidException(mock(MethodParameter.class), bindingResult);

    ProblemDetail problem = handler.handleValidationError(exception);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(problem.getProperties()).containsEntry("code", "INVALID_REQUEST");
    assertThat((List<?>) problem.getProperties().get("violations"))
        .singleElement()
        .asString()
        .contains("field=title", "message=must not be blank");
  }

  @Test
  void shouldMapMalformedRequests() {
    ProblemDetail problem =
        handler.handleMalformedRequest(new IllegalArgumentException("sensitive parser detail"));

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(problem.getDetail()).isEqualTo("The request is invalid");
    assertThat(problem.getProperties()).containsEntry("code", "INVALID_REQUEST");
  }

  @Test
  void shouldMapMissingResources() {
    ProblemDetail problem =
        handler.handleMissingResource(
            new NoResourceFoundException(HttpMethod.GET, "missing", "No static resource missing"));

    assertProtocolProblem(
        problem,
        HttpStatus.NOT_FOUND,
        "Resource not found",
        "RESOURCE_NOT_FOUND",
        "The requested resource was not found");
  }

  @Test
  void shouldMapUnsupportedMethodsAndPreserveAllowHeader() {
    ResponseEntity<ProblemDetail> response =
        handler.handleMethodNotAllowed(
            new HttpRequestMethodNotSupportedException("GET", List.of("POST")));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    assertThat(response.getHeaders().getAllow()).isEqualTo(Set.of(HttpMethod.POST));
    assertProtocolProblem(
        response.getBody(),
        HttpStatus.METHOD_NOT_ALLOWED,
        "Method not allowed",
        "METHOD_NOT_ALLOWED",
        "The HTTP method is not supported for this resource");
  }

  @Test
  void shouldMapUnsupportedRequestMediaAndPreserveAcceptHeader() {
    ResponseEntity<ProblemDetail> response =
        handler.handleUnsupportedMediaType(
            new HttpMediaTypeNotSupportedException(
                MediaType.TEXT_PLAIN, List.of(MediaType.APPLICATION_JSON), HttpMethod.POST));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    assertThat(response.getHeaders().getAccept()).containsExactly(MediaType.APPLICATION_JSON);
    assertProtocolProblem(
        response.getBody(),
        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
        "Unsupported media type",
        "UNSUPPORTED_MEDIA_TYPE",
        "The request media type is not supported");
  }

  @Test
  void shouldMapUnavailableResponseMedia() {
    ResponseEntity<ProblemDetail> response =
        handler.handleNotAcceptable(
            new HttpMediaTypeNotAcceptableException(List.of(MediaType.APPLICATION_JSON)));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_ACCEPTABLE);
    assertProtocolProblem(
        response.getBody(),
        HttpStatus.NOT_ACCEPTABLE,
        "Representation not acceptable",
        "NOT_ACCEPTABLE",
        "The requested response media type is not available");
  }

  @Test
  void shouldHideUnexpectedFailureDetails() {
    ProblemDetail problem =
        handler.handleUnexpectedError(new IllegalStateException("sensitive internal detail"));

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    assertThat(problem.getDetail()).isEqualTo("An unexpected error occurred");
    assertThat(problem.getProperties()).containsEntry("code", "INTERNAL_ERROR");
  }

  private static void assertProtocolProblem(
      ProblemDetail problem, HttpStatus status, String title, String code, String detail) {
    assertThat(problem).isNotNull();
    assertThat(problem.getStatus()).isEqualTo(status.value());
    assertThat(problem.getTitle()).isEqualTo(title);
    assertThat(problem.getDetail()).isEqualTo(detail);
    assertThat(problem.getProperties()).containsEntry("code", code);
  }

  private static Stream<Arguments> applicationErrors() {
    RuntimeException cause = new RuntimeException("dependency detail");
    return Stream.of(
        Arguments.of(
            new AgendaNotFoundException(AGENDA_ID), HttpStatus.NOT_FOUND, "Resource not found"),
        Arguments.of(
            new AssociateNotEligibleException(ASSOCIATE_CPF),
            HttpStatus.UNPROCESSABLE_CONTENT,
            "Business rule violation"),
        Arguments.of(
            new AssociateNotFoundException(ASSOCIATE_CPF),
            HttpStatus.UNPROCESSABLE_CONTENT,
            "Business rule violation"),
        Arguments.of(
            new EligibilityServiceUnavailableException(cause),
            HttpStatus.SERVICE_UNAVAILABLE,
            "Required dependency unavailable"),
        Arguments.of(
            new PersistenceUnavailableException(cause),
            HttpStatus.SERVICE_UNAVAILABLE,
            "Required dependency unavailable"),
        Arguments.of(
            new AssociateAlreadyVotedException(AGENDA_ID, cause),
            HttpStatus.CONFLICT,
            "Resource state conflict"),
        Arguments.of(
            new VotingResultNotAvailableException(AGENDA_ID),
            HttpStatus.CONFLICT,
            "Resource state conflict"),
        Arguments.of(
            new VotingSessionAlreadyExistsException(AGENDA_ID),
            HttpStatus.CONFLICT,
            "Resource state conflict"),
        Arguments.of(
            new VotingSessionClosedException(AGENDA_ID),
            HttpStatus.CONFLICT,
            "Resource state conflict"),
        Arguments.of(
            new VotingSessionNotFoundException(AGENDA_ID),
            HttpStatus.CONFLICT,
            "Resource state conflict"),
        Arguments.of(
            new UnknownApplicationException(),
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal server error"));
  }

  private static final class UnknownApplicationException extends VotingApplicationException {

    private UnknownApplicationException() {
      super("UNKNOWN_APPLICATION_ERROR", "Unknown application error");
    }
  }
}

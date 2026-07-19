package dev.luan.sicredi.voting.integration.eligibility;

import dev.luan.sicredi.voting.domainmodel.AssociateCpf;
import dev.luan.sicredi.voting.domainmodel.AssociateEligibility;
import dev.luan.sicredi.voting.service.error.AssociateNotFoundException;
import dev.luan.sicredi.voting.service.error.EligibilityServiceUnavailableException;
import dev.luan.sicredi.voting.service.port.out.AssociateEligibilityGateway;
import java.util.Locale;
import java.util.Objects;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public final class RestClientAssociateEligibilityGateway implements AssociateEligibilityGateway {

  private final RestClient restClient;

  public RestClientAssociateEligibilityGateway(RestClient restClient) {
    this.restClient = Objects.requireNonNull(restClient, "RestClient must not be null");
  }

  @Override
  public AssociateEligibility check(AssociateCpf associateCpf) {
    Objects.requireNonNull(associateCpf, "Associate CPF must not be null");

    ProviderResult result;
    try {
      result =
          restClient
              .get()
              .uri("/users/{cpf}", associateCpf.value())
              .exchange(
                  (request, providerResponse) -> {
                    if (providerResponse.getStatusCode().value() == 404) {
                      return new ProviderResult(true, null);
                    }
                    if (!providerResponse.getStatusCode().is2xxSuccessful()) {
                      throw providerResponse.createException();
                    }
                    return new ProviderResult(
                        false, providerResponse.bodyTo(EligibilityResponse.class));
                  });
    } catch (RestClientException | HttpMessageConversionException exception) {
      throw new EligibilityServiceUnavailableException(exception);
    }

    if (result.associateNotFound()) {
      throw new AssociateNotFoundException(associateCpf);
    }
    return mapEligibility(result.response());
  }

  private static AssociateEligibility mapEligibility(EligibilityResponse response) {
    if (response == null || response.status() == null) {
      throw invalidProviderResponse();
    }

    return switch (response.status().strip().toUpperCase(Locale.ROOT)) {
      case "ABLE_TO_VOTE", "ABLE TO VOTE" -> AssociateEligibility.ELIGIBLE;
      case "UNABLE_TO_VOTE", "UNABLE TO VOTE" -> AssociateEligibility.INELIGIBLE;
      default -> throw invalidProviderResponse();
    };
  }

  private static EligibilityServiceUnavailableException invalidProviderResponse() {
    return new EligibilityServiceUnavailableException(
        new IllegalArgumentException("Eligibility provider returned an invalid response"));
  }

  private record EligibilityResponse(String status) {}

  private record ProviderResult(boolean associateNotFound, EligibilityResponse response) {}
}

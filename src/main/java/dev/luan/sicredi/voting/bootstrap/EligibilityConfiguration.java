package dev.luan.sicredi.voting.bootstrap;

import dev.luan.sicredi.voting.integration.eligibility.RestClientAssociateEligibilityGateway;
import dev.luan.sicredi.voting.service.port.out.AssociateEligibilityGateway;
import java.net.http.HttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
class EligibilityConfiguration {

  @Bean
  AssociateEligibilityGateway associateEligibilityGateway(EligibilityProperties properties) {
    HttpClient httpClient =
        HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build();
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(properties.readTimeout());
    RestClient restClient =
        RestClient.builder()
            .baseUrl(properties.baseUrl().toString())
            .requestFactory(requestFactory)
            .build();
    return new RestClientAssociateEligibilityGateway(restClient);
  }
}

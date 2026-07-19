package dev.luan.sicredi.voting.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "dev.luan.sicredi.voting")
@ConfigurationPropertiesScan(basePackages = "dev.luan.sicredi.voting.bootstrap")
@EnableScheduling
public class VotingApplication {

  public static void main(String[] args) {
    SpringApplication.run(VotingApplication.class, args);
  }
}

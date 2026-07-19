package dev.luan.sicredi.voting.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "dev.luan.sicredi.voting")
public class VotingApplication {

  public static void main(String[] args) {
    SpringApplication.run(VotingApplication.class, args);
  }
}

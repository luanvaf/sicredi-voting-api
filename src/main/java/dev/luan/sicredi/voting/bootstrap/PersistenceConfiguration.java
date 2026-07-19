package dev.luan.sicredi.voting.bootstrap;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration(proxyBeanMethods = false)
@EntityScan(basePackages = "dev.luan.sicredi.voting.integration.persistence")
@EnableJpaRepositories(basePackages = "dev.luan.sicredi.voting.integration.persistence")
class PersistenceConfiguration {}

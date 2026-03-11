package com.riskregister.riskregisterapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EntityScan({
    "com.riskregister.riskregisterapp.beans",
    "com.riskregister.riskregisterapp.entities"
})
@EnableJpaRepositories("com.riskregister.riskregisterapp.repositories")
public class RiskRegisterApplication {

	public static void main(String[] args) {
		SpringApplication.run(RiskRegisterApplication.class, args);
	}

}

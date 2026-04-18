package com.healthlens.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HealthLensApplication {

	public static void main(String[] args) {
		SpringApplication.run(HealthLensApplication.class, args);
	}
}

package com.porp;

import org.springframework.boot.SpringApplication;

public class TestPaymentOrchestrationPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.from(PaymentOrchestrationPlatformApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}

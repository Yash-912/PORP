package com.porp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;

/**
 * Entry point for the Payment Orchestration & Routing Platform.
 *
 * <p>This is a Spring Modulith application: a modular monolith where each top-level
 * package under {@code com.porp} is an independently verifiable module.
 */
@SpringBootApplication
@Modulithic(systemName = "Payment Orchestration & Routing Platform")
public class PorpApplication {

    public static void main(String[] args) {
        SpringApplication.run(PorpApplication.class, args);
    }
}
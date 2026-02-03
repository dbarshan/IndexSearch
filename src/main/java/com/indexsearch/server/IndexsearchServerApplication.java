package com.indexsearch.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import com.indexsearch.server.util.StartupInitializer;

/**
 * Server entry point and Spring Boot configuration bootstrap.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class IndexsearchServerApplication {

	/**
	 * Launch the Spring Boot application.
	 */
	public static void main(String[] args) {
		SpringApplication.run(IndexsearchServerApplication.class, args);
	}

	/**
	 * Run startup initialization when the server profile is active.
	 */
	@Bean
	@Profile("server")
	public CommandLineRunner startupRunner(StartupInitializer startupInitializer) {
		return args -> startupInitializer.init();
	}
}

package com.indexsearch.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.util.Map;

@SpringBootApplication(scanBasePackages = "com.indexsearch.client")
public class IndexSearchClientApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(IndexSearchClientApplication.class);
        app.setDefaultProperties(Map.of(
                "spring.main.web-application-type", "none",
                "spring.profiles.active", "client"
        ));
        app.run(args);
    }
}

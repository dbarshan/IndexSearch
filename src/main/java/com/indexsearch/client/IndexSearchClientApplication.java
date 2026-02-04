package com.indexsearch.client;

import com.indexsearch.client.gui.GuiLauncher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

@SpringBootApplication(scanBasePackages = "com.indexsearch.client")
public class IndexSearchClientApplication {
    public static void main(String[] args) {
        String mode = resolveClientMode();
        if ("gui".equalsIgnoreCase(mode)) {
            GuiLauncher.launchGui(args);
            return;
        }
        SpringApplication app = new SpringApplication(IndexSearchClientApplication.class);
        app.setDefaultProperties(Map.of(
                "spring.main.web-application-type", "none",
                "spring.profiles.active", "client"
        ));
        app.run(args);
    }

    private static String resolveClientMode() {
        String mode = System.getProperty("application.client.mode");
        if (mode == null || mode.isBlank()) {
            mode = System.getenv("INDEXSEARCH_CLIENT_MODE");
        }
        if (mode == null || mode.isBlank()) {
            mode = readModeFromProperties();
        }
        return mode == null || mode.isBlank() ? "cli" : mode.trim().toLowerCase();
    }

    private static String readModeFromProperties() {
        Properties properties = new Properties();
        try (InputStream input = IndexSearchClientApplication.class
                .getClassLoader()
                .getResourceAsStream("application-client.properties")) {
            if (input == null) {
                return null;
            }
            properties.load(input);
            return properties.getProperty("application.client.mode");
        } catch (IOException e) {
            return null;
        }
    }
}

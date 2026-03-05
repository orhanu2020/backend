package com.socialmedia;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Map;

@SpringBootApplication
@EnableScheduling
public class SocialMediaApplication {

    /** Exclude DataSource and JPA when DATABASE_URL is not set (run without PostgreSQL). */
    private static final String EXCLUDE_DB_AUTOCONFIG = String.join(",",
            "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
            "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration",
            "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
            "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration");

    public static void main(String[] args) {
        // Load .env file if it exists
        try {
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing()
                    .load();

            // Set environment variables as system properties so Spring can access them
            dotenv.entries().forEach(entry ->
                    System.setProperty(entry.getKey(), entry.getValue())
            );
        } catch (Exception e) {
            // If .env file doesn't exist or can't be loaded, continue without it
            // Environment variables can still be set via system environment
            System.out.println("Note: .env file not found or couldn't be loaded. Using system environment variables.");
        }

        SpringApplication app = new SpringApplication(SocialMediaApplication.class);
        if (!isDatabaseConfigured()) {
            app.setDefaultProperties(Map.of("spring.autoconfigure.exclude", EXCLUDE_DB_AUTOCONFIG));
            System.out.println("Database not configured (no DATABASE_URL): running without PostgreSQL.");
        }
        app.run(args);
    }

    private static boolean isDatabaseConfigured() {
        String url = System.getProperty("DATABASE_URL");
        if (url == null || url.isBlank()) {
            url = System.getenv("DATABASE_URL");
        }
        return url != null && !url.isBlank();
    }
}

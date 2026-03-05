package com.socialmedia;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SocialMediaApplication {

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
        
        SpringApplication.run(SocialMediaApplication.class, args);
    }
}

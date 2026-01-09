package com.lre.gitlabintegration.config.db;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
@Slf4j
public class DatabaseConfig {

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @PostConstruct
    public void init() {
        // Extract file path from jdbc:sqlite:db/lre-gitlab.db
        String dbPath = datasourceUrl.replace("jdbc:sqlite:", "");
        File dbFile = new File(dbPath);
        File parentDir = dbFile.getParentFile();

        if (parentDir != null && !parentDir.exists()) {
            if (parentDir.mkdirs()) {
                log.info("Created database directory: {}", parentDir.getAbsolutePath());
            } else {
                log.error("Failed to create database directory: {}", parentDir.getAbsolutePath());
                throw new IllegalStateException(
                        "Could not create database directory: " + parentDir.getAbsolutePath()
                );
            }
        }
    }
}

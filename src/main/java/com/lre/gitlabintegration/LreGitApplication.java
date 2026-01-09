package com.lre.gitlabintegration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LreGitApplication {
    public static void main(String[] args) {
        SpringApplication.run(LreGitApplication.class, args);
    }
}

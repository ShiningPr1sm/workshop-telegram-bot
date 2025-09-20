package com.shiningpr1sm.feedbackbot.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class SchemaCreator {

    @Value("${spring.jpa.properties.hibernate.default_schema:public}")
    private String defaultSchema;

    @Bean
    public CommandLineRunner createSchema(JdbcTemplate jdbcTemplate) {
        return args -> {
            if (!"public".equalsIgnoreCase(defaultSchema)) {
                String sql = "CREATE SCHEMA IF NOT EXISTS " + defaultSchema;
                jdbcTemplate.execute(sql);
                System.out.println("Schema '" + defaultSchema + "' created or already exists.");
            } else {
                System.out.println("Using default 'public' schema. No custom schema creation needed.");
            }
        };
    }
}
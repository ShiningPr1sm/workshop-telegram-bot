package com.shiningpr1sm.feedbackbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.starter.TelegramBotStarterConfiguration;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(TelegramBotStarterConfiguration.class)
public class FeedbackBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(FeedbackBotApplication.class, args);
    }
}
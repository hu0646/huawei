package com.campus.food.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "agent")
public class AgentProperties {

    private int maxToolCalls = 5;

    private int maxModelRounds = 6;

    private Duration roundTimeout = Duration.ofSeconds(60);

    private Duration toolTimeout = Duration.ofSeconds(5);

    private int maxHistoryMessages = 20;

    private Llm llm = new Llm();

    @Data
    public static class Llm {

        private String baseUrl = "https://api.deepseek.com";

        private String apiKey = "";

        private String model = "deepseek-chat";

        private double temperature = 0.7;

        private Duration connectTimeout = Duration.ofSeconds(10);

        private Duration requestTimeout = Duration.ofSeconds(30);

        private boolean mockEnabled = false;
    }
}
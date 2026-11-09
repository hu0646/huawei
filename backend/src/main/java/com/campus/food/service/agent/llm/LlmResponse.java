package com.campus.food.service.agent.llm;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class LlmResponse {

    private String content;

    private List<LlmToolCall> toolCalls = new ArrayList<>();

    public boolean requestsToolCall() {
        return toolCalls != null && !toolCalls.isEmpty();
    }
}
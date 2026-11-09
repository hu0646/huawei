package com.campus.food.service.agent.llm;

import java.util.List;
import java.util.Map;

public interface LlmClient {

    LlmResponse chat(List<LlmMessage> messages, List<Map<String, Object>> tools);

    boolean isAvailable();
}
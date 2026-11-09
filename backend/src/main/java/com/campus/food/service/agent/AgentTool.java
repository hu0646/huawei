package com.campus.food.service.agent;

import java.util.Map;

public interface AgentTool {

    String name();

    String description();

    Map<String, Object> parameters();

    ToolResult execute(Long userId, Map<String, Object> arguments);
}
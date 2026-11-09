package com.campus.food.service.agent;

import com.campus.food.common.BusinessException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AgentToolRegistry {

    private static final int MAX_RESULT_LENGTH = 4000;

    private final List<AgentTool> toolList;

    private final ObjectMapper objectMapper;

    private Map<String, AgentTool> tools;

    public List<Map<String, Object>> definitions() {
        return toolMap().values().stream()
                .sorted(Comparator.comparing(AgentTool::name))
                .map(tool -> {
                    Map<String, Object> function = new LinkedHashMap<>();
                    function.put("name", tool.name());
                    function.put("description", tool.description());
                    function.put("parameters", tool.parameters());
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("type", "function");
                    item.put("function", function);
                    return item;
                })
                .toList();
    }

    public AgentTool get(String name) {
        return toolMap().get(name);
    }

    public Map<String, Object> parseArguments(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new BusinessException("工具参数不是合法 JSON");
        }
    }

    public String toJson(Object data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            if (json.length() > MAX_RESULT_LENGTH) {
                return json.substring(0, MAX_RESULT_LENGTH) + "...(truncated)";
            }
            return json;
        } catch (Exception e) {
            throw new BusinessException("工具结果序列化失败");
        }
    }

    private Map<String, AgentTool> toolMap() {
        if (tools == null) {
            tools = toolList.stream()
                    .collect(Collectors.toMap(AgentTool::name, tool -> tool, (first, second) -> first,
                            LinkedHashMap::new));
        }
        return tools;
    }
}
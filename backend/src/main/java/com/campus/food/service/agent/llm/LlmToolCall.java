package com.campus.food.service.agent.llm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LlmToolCall {

    private String id;

    private String type = "function";

    private Function function;

    public static LlmToolCall of(String id, String name, String arguments) {
        return new LlmToolCall(id, "function", new Function(name, arguments));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Function {

        private String name;

        private String arguments;
    }
}
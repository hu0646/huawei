package com.campus.food.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentStep {

    private String name;

    private String arguments;

    private boolean success;

    private long durationMs;

    private String summary;
}
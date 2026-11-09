package com.campus.food.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentMessageVO {

    private Long id;

    private String role;

    private String content;

    private LocalDateTime createdAt;
}
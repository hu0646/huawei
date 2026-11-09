package com.campus.food.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDetailVO {

    private Long id;

    private String title;

    private List<AgentMessageVO> messages;
}
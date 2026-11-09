package com.campus.food.dto.agent;

import com.campus.food.dto.FoodVO;
import com.campus.food.dto.RecommendVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    private Long conversationId;

    private String reply;

    private List<AgentStep> steps;

    private List<RecommendVO> recommendations;

    private List<FoodVO> foods;

    private boolean createdConversation;

    private int toolCallCount;
}
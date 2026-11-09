package com.campus.food.service.agent;

import com.campus.food.dto.FoodVO;
import com.campus.food.dto.RecommendVO;
import com.campus.food.dto.agent.AgentStep;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ToolOutcome {

    private AgentStep step;

    private String json;

    private List<FoodVO> foods;

    private List<RecommendVO> recommendations;
}
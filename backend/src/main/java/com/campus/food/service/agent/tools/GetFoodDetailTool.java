package com.campus.food.service.agent.tools;

import com.campus.food.dto.FoodVO;
import com.campus.food.service.FoodService;
import com.campus.food.service.agent.AgentParams;
import com.campus.food.service.agent.AgentTool;
import com.campus.food.service.agent.ToolResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GetFoodDetailTool implements AgentTool {

    private final FoodService foodService;

    @Override
    public String name() {
        return "get_food_detail";
    }

    @Override
    public String description() {
        return "根据美食 ID 查询单道美食的详细信息，包括价格、评分、食堂、标签和简介。";
    }

    @Override
    public Map<String, Object> parameters() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("foodId", Map.of(
                "type", "integer",
                "minimum", 1,
                "description", "美食 ID"));

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", List.of("foodId"));
        schema.put("additionalProperties", false);
        return schema;
    }

    @Override
    public ToolResult execute(Long userId, Map<String, Object> arguments) {
        Long foodId = AgentParams.requiredLong(arguments, "foodId");
        FoodVO food = foodService.detail(foodId, userId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("food", food);
        return ToolResult.of(data, List.of(food), "已获取「" + food.getName() + "」的详情");
    }
}
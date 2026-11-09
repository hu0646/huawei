package com.campus.food.service.agent.tools;

import com.campus.food.dto.FoodVO;
import com.campus.food.service.FavoriteService;
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
public class AddFavoriteTool implements AgentTool {

    private final FavoriteService favoriteService;
    private final FoodService foodService;

    @Override
    public String name() {
        return "add_favorite";
    }

    @Override
    public String description() {
        return "将指定美食加入当前登录用户的收藏。收藏操作只作用于当前用户。";
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
        favoriteService.add(userId, foodId);
        FoodVO food = foodService.detail(foodId, userId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("success", true);
        data.put("foodId", foodId);
        data.put("food", food);
        return ToolResult.of(data, List.of(food), "已收藏「" + food.getName() + "」");
    }
}
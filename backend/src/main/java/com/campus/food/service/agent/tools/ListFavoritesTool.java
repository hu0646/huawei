package com.campus.food.service.agent.tools;

import com.campus.food.dto.FoodVO;
import com.campus.food.service.FavoriteService;
import com.campus.food.service.agent.AgentTool;
import com.campus.food.service.agent.ToolResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ListFavoritesTool implements AgentTool {

    private final FavoriteService favoriteService;

    @Override
    public String name() {
        return "list_favorites";
    }

    @Override
    public String description() {
        return "查看当前登录用户的收藏清单，返回已收藏的美食列表。";
    }

    @Override
    public Map<String, Object> parameters() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of());
        schema.put("additionalProperties", false);
        return schema;
    }

    @Override
    public ToolResult execute(Long userId, Map<String, Object> arguments) {
        List<FoodVO> foods = favoriteService.list(userId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("foods", foods);
        data.put("count", foods.size());
        return ToolResult.of(data, foods, "共 " + foods.size() + " 道收藏");
    }
}
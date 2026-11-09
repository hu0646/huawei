package com.campus.food.service.agent;

import com.campus.food.dto.FoodVO;
import com.campus.food.dto.RecommendVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolResult {

    private Object data;

    private List<FoodVO> foods = new ArrayList<>();

    private List<RecommendVO> recommendations = new ArrayList<>();

    private String summary;

    public static ToolResult of(Object data, String summary) {
        return new ToolResult(data, new ArrayList<>(), new ArrayList<>(), summary);
    }

    public static ToolResult of(Object data, List<FoodVO> foods, String summary) {
        return new ToolResult(data, foods, new ArrayList<>(), summary);
    }

    public static ToolResult recommendations(Object data,
                                            List<FoodVO> foods,
                                            List<RecommendVO> recommendations,
                                            String summary) {
        return new ToolResult(data, foods, recommendations, summary);
    }
}
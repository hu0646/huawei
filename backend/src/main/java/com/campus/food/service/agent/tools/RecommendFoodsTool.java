package com.campus.food.service.agent.tools;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.food.common.BusinessException;
import com.campus.food.dto.FoodVO;
import com.campus.food.dto.RecommendVO;
import com.campus.food.entity.Tag;
import com.campus.food.mapper.TagMapper;
import com.campus.food.service.RecommendService;
import com.campus.food.service.agent.AgentParams;
import com.campus.food.service.agent.AgentTool;
import com.campus.food.service.agent.ToolResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RecommendFoodsTool implements AgentTool {

    private final RecommendService recommendService;
    private final TagMapper tagMapper;

    @Override
    public String name() {
        return "recommend_foods";
    }

    @Override
    public String description() {
        return "按预算、口味标签和分类为当前用户推荐美食，返回推荐分与推荐理由。"
                + "预算和口味参数可选；不传时使用用户已保存的偏好。";
    }

    @Override
    public Map<String, Object> parameters() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("budgetMin", Map.of(
                "type", "number",
                "minimum", 0,
                "description", "预算下限（元），可选"));
        properties.put("budgetMax", Map.of(
                "type", "number",
                "minimum", 0,
                "description", "预算上限（元），可选"));
        properties.put("tagIds", Map.of(
                "type", "array",
                "items", Map.of("type", "integer", "minimum", 1),
                "description", "口味标签 ID 列表，可选；传空数组表示不使用口味偏好"));
        properties.put("category", Map.of(
                "type", "string",
                "maxLength", 40,
                "description", "美食分类，可选"));
        properties.put("limit", Map.of(
                "type", "integer",
                "minimum", 1,
                "maximum", 20,
                "description", "返回数量，默认 5"));

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("additionalProperties", false);
        return schema;
    }

    @Override
    public ToolResult execute(Long userId, Map<String, Object> arguments) {
        BigDecimal budgetMin = AgentParams.optionalDecimal(arguments, "budgetMin");
        BigDecimal budgetMax = AgentParams.optionalDecimal(arguments, "budgetMax");
        if (budgetMin != null && budgetMax != null && budgetMin.compareTo(budgetMax) > 0) {
            throw new BusinessException("预算下限不能大于上限");
        }

        List<Long> tagIds = AgentParams.optionalLongList(arguments, "tagIds");
        Set<Long> tagIdSet = null;
        if (tagIds != null) {
            tagIdSet = new LinkedHashSet<>(tagIds);
            if (!tagIdSet.isEmpty()) {
                Long validCount = tagMapper.selectCount(Wrappers.<Tag>lambdaQuery()
                        .in(Tag::getId, tagIdSet)
                        .eq(Tag::getStatus, 1));
                if (validCount == null || validCount.intValue() != tagIdSet.size()) {
                    throw new BusinessException("包含无效的口味标签");
                }
            }
        }

        String category = AgentParams.optionalString(arguments, "category");
        if (category != null && category.length() > 40) {
            throw new BusinessException("分类名称过长");
        }

        int limit = AgentParams.optionalInt(arguments, "limit", 5);
        limit = Math.max(1, Math.min(limit, 20));

        List<RecommendVO> recommendations = recommendService.recommend(
                userId, limit, budgetMin, budgetMax, tagIdSet, category);
        List<FoodVO> foods = recommendations.stream().map(RecommendVO::getFood).toList();

        Map<String, Object> criteria = new LinkedHashMap<>();
        criteria.put("budgetMin", budgetMin);
        criteria.put("budgetMax", budgetMax);
        criteria.put("tagIds", tagIdSet);
        criteria.put("category", category);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("recommendations", recommendations);
        data.put("count", recommendations.size());
        data.put("criteria", criteria);

        return ToolResult.recommendations(data, foods, recommendations,
                "推荐了 " + recommendations.size() + " 道美食");
    }
}
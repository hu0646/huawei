package com.campus.food.service.agent.tools;

import com.campus.food.dto.UserVO;
import com.campus.food.service.UserService;
import com.campus.food.service.agent.AgentTool;
import com.campus.food.service.agent.ToolResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GetMyPreferencesTool implements AgentTool {

    private final UserService userService;

    @Override
    public String name() {
        return "get_my_preferences";
    }

    @Override
    public String description() {
        return "获取当前登录用户的口味偏好标签和每餐预算区间。回答口味、预算相关问题前优先调用。";
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
        UserVO user = userService.getProfile(userId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("preferences", user.getPreferences().stream().map(tag -> tag.getName()).toList());
        data.put("tagIds", user.getPreferences().stream().map(tag -> tag.getId()).toList());
        data.put("budgetMin", user.getBudgetMin());
        data.put("budgetMax", user.getBudgetMax());
        data.put("onboardingDone", user.getOnboardingDone());
        int count = user.getPreferences().size();
        return ToolResult.of(data, count == 0
                ? "用户尚未选择口味偏好"
                : "已读取 " + count + " 个口味偏好");
    }
}
package com.campus.food.service.agent.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "agent.llm.mock-enabled", havingValue = "true")
public class MockLlmClient implements LlmClient {

    private static final Map<String, Long> KEYWORD_TAGS = Map.ofEntries(
            Map.entry("麻辣", 1L), Map.entry("香辣", 2L), Map.entry("清淡", 3L),
            Map.entry("酸甜", 4L), Map.entry("咸鲜", 5L), Map.entry("甜品", 6L),
            Map.entry("面食", 7L), Map.entry("米饭", 8L), Map.entry("烧烤", 9L),
            Map.entry("汤类", 10L), Map.entry("素食", 11L), Map.entry("快餐", 12L),
            Map.entry("早餐", 13L), Map.entry("小吃", 14L), Map.entry("川味", 15L),
            Map.entry("粤味", 16L), Map.entry("西北", 17L), Map.entry("日韩", 18L),
            Map.entry("西式", 19L), Map.entry("卤味", 20L)
    );

    private static final Pattern MONEY_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*元");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+)");

    private final ObjectMapper objectMapper;

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public LlmResponse chat(List<LlmMessage> messages, List<Map<String, Object>> tools) {
        String lastUser = messages.stream()
                .filter(message -> "user".equals(message.getRole()))
                .map(LlmMessage::getContent)
                .reduce((first, second) -> second)
                .orElse("");
        long toolCount = messages.stream()
                .filter(message -> "tool".equals(message.getRole()))
                .count();
        boolean calledAddFavorite = messages.stream()
                .filter(message -> message.getToolCalls() != null)
                .flatMap(message -> message.getToolCalls().stream())
                .anyMatch(call -> "add_favorite".equals(call.getFunction().getName()));

        if (toolCount == 0) {
            if (lastUser.contains("压力测试")) {
                List<LlmToolCall> stressCalls = new ArrayList<>();
                for (int i = 0; i < 6; i++) {
                    stressCalls.add(LlmToolCall.of("call_mock_stress_" + i, "list_favorites", "{}"));
                }
                LlmResponse stress = new LlmResponse();
                stress.setToolCalls(stressCalls);
                return stress;
            }
            if (containsAny(lastUser, "我的口味", "偏好", "我的预算", "预算是什么")) {
                return tool("get_my_preferences", Map.of());
            }
            if (containsAny(lastUser, "收藏列表", "我的收藏", "查看收藏", "看看收藏")) {
                return tool("list_favorites", Map.of());
            }
            if (lastUser.contains("详情")) {
                Integer foodId = firstNumber(lastUser);
                if (foodId != null) {
                    return tool("get_food_detail", Map.of("foodId", foodId));
                }
            }
            if (lastUser.contains("收藏") && !calledAddFavorite) {
                return tool("recommend_foods", Map.of("limit", 1));
            }
            if (containsAny(lastUser, "推荐", "吃什么", "来点", "有什么", "美食",
                    "麻辣", "香辣", "清淡", "酸甜", "面食", "米饭", "川味", "日韩", "预算")) {
                return tool("recommend_foods", recommendArguments(lastUser));
            }
            return text("我可以帮你查口味预算、推荐美食、看美食详情，也能收藏和查看收藏。"
                    + "试试问我：推荐 15 元以内的麻辣美食。");
        }

        if (lastUser.contains("收藏") && !calledAddFavorite) {
            Long foodId = firstFoodId(lastToolContent(messages));
            if (foodId != null) {
                return tool("add_favorite", Map.of("foodId", foodId));
            }
        }
        return text(finishText(lastUser));
    }

    private Map<String, Object> recommendArguments(String text) {
        Map<String, Object> arguments = new LinkedHashMap<>();
        List<Long> tagIds = new ArrayList<>();
        KEYWORD_TAGS.forEach((keyword, tagId) -> {
            if (text.contains(keyword)) {
                tagIds.add(tagId);
            }
        });
        tagIds.sort(Comparator.naturalOrder());
        if (!tagIds.isEmpty()) {
            arguments.put("tagIds", tagIds);
        }

        Matcher matcher = MONEY_PATTERN.matcher(text);
        if (matcher.find()) {
            double value = Double.parseDouble(matcher.group(1));
            if (text.contains("以上") || text.contains("不低于")) {
                arguments.put("budgetMin", value);
            } else {
                arguments.put("budgetMax", value);
            }
        }
        arguments.put("limit", 5);
        return arguments;
    }

    private String finishText(String lastUser) {
        if (lastUser.contains("详情")) {
            return "这是这道美食的详细信息，结果见下方卡片。还需要我帮你收藏吗？";
        }
        if (lastUser.contains("收藏") && !containsAny(lastUser, "收藏列表", "我的收藏", "查看收藏")) {
            return "已经帮你加入收藏了。想再挑一道的话直接告诉我。";
        }
        if (containsAny(lastUser, "收藏列表", "我的收藏", "查看收藏", "看看收藏")) {
            return "这是你当前的收藏清单，结果见下方。";
        }
        if (containsAny(lastUser, "我的口味", "偏好", "我的预算", "预算是什么")) {
            return "已读取你的口味与预算，后续推荐会参考这些偏好。";
        }
        return "已经按你的条件挑好了，结果见下方卡片。需要看详情或收藏，直接告诉我。";
    }

    private LlmResponse tool(String name, Map<String, Object> arguments) {
        String args;
        try {
            args = objectMapper.writeValueAsString(arguments);
        } catch (Exception e) {
            args = "{}";
        }
        LlmResponse response = new LlmResponse();
        response.setToolCalls(List.of(LlmToolCall.of("call_mock_" + System.nanoTime(), name, args)));
        return response;
    }

    private LlmResponse text(String content) {
        LlmResponse response = new LlmResponse();
        response.setContent(content);
        return response;
    }

    private String lastToolContent(List<LlmMessage> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            LlmMessage message = messages.get(i);
            if ("tool".equals(message.getRole())) {
                return message.getContent();
            }
        }
        return null;
    }

    private Long firstFoodId(String toolContent) {
        if (toolContent == null) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(toolContent);
            JsonNode foods = root.get("foods");
            if (foods != null && foods.isArray() && !foods.isEmpty()) {
                JsonNode id = foods.get(0).get("id");
                if (id != null && id.canConvertToLong()) {
                    return id.asLong();
                }
            }
            JsonNode recommendations = root.get("recommendations");
            if (recommendations != null && recommendations.isArray() && !recommendations.isEmpty()) {
                JsonNode id = recommendations.get(0).path("food").get("id");
                if (id != null && id.canConvertToLong()) {
                    return id.asLong();
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private Integer firstNumber(String text) {
        Matcher matcher = NUMBER_PATTERN.matcher(text);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : null;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
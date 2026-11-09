package com.campus.food.service.agent;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.food.common.BusinessException;
import com.campus.food.config.AgentProperties;
import com.campus.food.dto.FoodVO;
import com.campus.food.dto.RecommendVO;
import com.campus.food.dto.agent.AgentMessageVO;
import com.campus.food.dto.agent.AgentStep;
import com.campus.food.dto.agent.ChatRequest;
import com.campus.food.dto.agent.ChatResponse;
import com.campus.food.dto.agent.ConversationDetailVO;
import com.campus.food.dto.agent.ConversationVO;
import com.campus.food.entity.AgentConversation;
import com.campus.food.entity.AgentMessage;
import com.campus.food.mapper.AgentConversationMapper;
import com.campus.food.mapper.AgentMessageMapper;
import com.campus.food.service.UserService;
import com.campus.food.service.agent.llm.LlmClient;
import com.campus.food.service.agent.llm.LlmMessage;
import com.campus.food.service.agent.llm.LlmResponse;
import com.campus.food.service.agent.llm.LlmToolCall;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private static final String SYSTEM_PROMPT = """
            你是「校园食光」校园美食推荐助手。你可以查询用户口味与预算、推荐美食、查询美食详情、
            收藏美食和查看收藏。

            规则：
            1. 涉及用户口味、预算时优先调用 get_my_preferences。
            2. 推荐美食必须调用 recommend_foods，不要编造美食名称、ID 或价格。
            3. 收藏与查看收藏分别调用 add_favorite、list_favorites，操作只作用于当前登录用户。
            4. 工具参数必须合法；工具返回错误时向用户说明，并给出可执行的下一步建议。
            5. 回答使用简洁中文，推荐时说明推荐理由与价格，不要使用「匹配概率」这类表述。
            6. 信息足够时直接回答，避免无意义的重复工具调用。
            7. 每轮最多执行 5 次工具调用，超出时你需要根据已有结果给出结论。
            """;

    private final AgentProperties properties;
    private final LlmClient llmClient;
    private final AgentToolRegistry toolRegistry;
    private final AgentConversationMapper conversationMapper;
    private final AgentMessageMapper messageMapper;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    private final ExecutorService toolExecutor = Executors.newFixedThreadPool(4);

    @PreDestroy
    public void shutdown() {
        toolExecutor.shutdownNow();
    }

    public ChatResponse chat(Long userId, ChatRequest request) {
        userService.requireUser(userId);
        if (!llmClient.isAvailable()) {
            throw new BusinessException(503, "尚未配置 DEEPSEEK_API_KEY，暂时无法使用对话助手");
        }
        String userMessage = request.getMessage().trim();
        if (userMessage.isEmpty()) {
            throw new BusinessException("消息不能为空");
        }

        long deadline = System.currentTimeMillis() + properties.getRoundTimeout().toMillis();
        boolean created = request.getConversationId() == null;
        AgentConversation conversation = resolveConversation(userId, request.getConversationId(), userMessage);

        List<LlmMessage> messages = buildContext(conversation.getId());
        messages.add(LlmMessage.user(userMessage));
        saveMessage(conversation.getId(), "user", userMessage, null, null);

        List<AgentStep> steps = new ArrayList<>();
        List<FoodVO> foods = new ArrayList<>();
        List<RecommendVO> recommendations = new ArrayList<>();
        String reply = null;
        int toolCallCount = 0;

        for (int round = 0; round < properties.getMaxModelRounds(); round++) {
            if (System.currentTimeMillis() >= deadline) {
                throw new BusinessException(504, "本轮对话已超时，请稍后再试");
            }
            LlmResponse response = llmClient.chat(messages, toolRegistry.definitions());
            if (!response.requestsToolCall()) {
                reply = StringUtils.hasText(response.getContent())
                        ? response.getContent()
                        : "我已经完成查询。";
                break;
            }

            List<LlmToolCall> calls = response.getToolCalls();
            messages.add(LlmMessage.assistant(response.getContent(), calls));
            saveMessage(conversation.getId(), "assistant", response.getContent(),
                    toJson(calls), null);

            for (LlmToolCall call : calls) {
                if (toolCallCount >= properties.getMaxToolCalls()) {
                    reply = "本轮工具调用次数已达上限（" + properties.getMaxToolCalls()
                            + " 次），已停止继续调用。可以缩小问题范围后再试。";
                    break;
                }
                toolCallCount++;
                ToolOutcome outcome = executeTool(userId, call, deadline);
                steps.add(outcome.getStep());
                foods.addAll(outcome.getFoods());
                recommendations.addAll(outcome.getRecommendations());
                messages.add(LlmMessage.tool(call.getId(), outcome.getJson()));
                saveMessage(conversation.getId(), "tool", outcome.getJson(), null, call.getId());
            }
            if (reply != null) {
                break;
            }
        }

        if (reply == null) {
            reply = "抱歉，本轮没有得到最终回答，请换个说法再试一次。";
        }

        saveMessage(conversation.getId(), "assistant", reply, null, null);
        touchConversation(conversation.getId());

        List<RecommendVO> uniqueRecommendations = recommendations.stream()
                .collect(Collectors.toMap(item -> item.getFood().getId(), item -> item,
                        (first, second) -> first, LinkedHashMap::new))
                .values().stream().toList();
        Set<Long> recommendedIds = uniqueRecommendations.stream()
                .map(item -> item.getFood().getId())
                .collect(Collectors.toSet());
        List<FoodVO> otherFoods = foods.stream()
                .filter(food -> !recommendedIds.contains(food.getId()))
                .collect(Collectors.toMap(FoodVO::getId, food -> food,
                        (first, second) -> first, LinkedHashMap::new))
                .values().stream().toList();

        return ChatResponse.builder()
                .conversationId(conversation.getId())
                .reply(reply)
                .steps(steps)
                .recommendations(uniqueRecommendations)
                .foods(otherFoods)
                .createdConversation(created)
                .toolCallCount(toolCallCount)
                .build();
    }

    public List<ConversationVO> listConversations(Long userId) {
        userService.requireUser(userId);
        return conversationMapper.selectList(Wrappers.<AgentConversation>lambdaQuery()
                        .eq(AgentConversation::getUserId, userId)
                        .orderByDesc(AgentConversation::getUpdatedAt)
                        .orderByDesc(AgentConversation::getId)
                        .last("LIMIT 20"))
                .stream()
                .map(conversation -> new ConversationVO(conversation.getId(),
                        conversation.getTitle(), conversation.getUpdatedAt()))
                .toList();
    }

    public ConversationDetailVO getConversation(Long userId, Long conversationId) {
        AgentConversation conversation = requireConversation(userId, conversationId);
        List<AgentMessage> messages = messageMapper.selectList(Wrappers.<AgentMessage>lambdaQuery()
                .eq(AgentMessage::getConversationId, conversationId)
                .in(AgentMessage::getRole, "user", "assistant")
                .isNotNull(AgentMessage::getContent)
                .orderByAsc(AgentMessage::getId));
        List<AgentMessageVO> messageVOs = messages.stream()
                .filter(message -> StringUtils.hasText(message.getContent()))
                .map(message -> new AgentMessageVO(message.getId(), message.getRole(),
                        message.getContent(), message.getCreatedAt()))
                .toList();
        return new ConversationDetailVO(conversation.getId(), conversation.getTitle(), messageVOs);
    }

    private ToolOutcome executeTool(Long userId, LlmToolCall call, long deadline) {
        long start = System.currentTimeMillis();
        String name = call.getFunction() == null ? "" : call.getFunction().getName();
        String arguments = call.getFunction() == null ? "{}" : call.getFunction().getArguments();
        AgentTool tool = toolRegistry.get(name);
        if (tool == null) {
            return failure(name, arguments, start, "工具不存在或未授权");
        }
        long remaining = deadline - System.currentTimeMillis();
        if (remaining <= 0) {
            return failure(name, arguments, start, "本轮对话已超时");
        }

        Future<ToolResult> future = toolExecutor.submit(() -> {
            Map<String, Object> parsed = toolRegistry.parseArguments(arguments);
            return tool.execute(userId, parsed);
        });
        try {
            ToolResult result = future.get(Math.min(properties.getToolTimeout().toMillis(), remaining),
                    TimeUnit.MILLISECONDS);
            AgentStep step = AgentStep.builder()
                    .name(name)
                    .arguments(arguments)
                    .success(true)
                    .durationMs(System.currentTimeMillis() - start)
                    .summary(result.getSummary())
                    .build();
            return new ToolOutcome(step, toolRegistry.toJson(result.getData()),
                    result.getFoods(), result.getRecommendations());
        } catch (TimeoutException e) {
            future.cancel(true);
            return failure(name, arguments, start,
                    "工具执行超时（" + properties.getToolTimeout().toSeconds() + " 秒）");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            String message = cause instanceof BusinessException businessException
                    ? businessException.getMessage()
                    : "工具执行失败";
            log.warn("Agent 工具执行失败: tool={}, message={}", name, message);
            return failure(name, arguments, start, message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return failure(name, arguments, start, "工具执行被中断");
        }
    }

    private ToolOutcome failure(String name, String arguments, long start, String message) {
        AgentStep step = AgentStep.builder()
                .name(name)
                .arguments(arguments)
                .success(false)
                .durationMs(System.currentTimeMillis() - start)
                .summary(message)
                .build();
        String json;
        try {
            json = objectMapper.writeValueAsString(Map.of("error", message));
        } catch (Exception e) {
            json = "{\"error\":\"工具执行失败\"}";
        }
        return new ToolOutcome(step, json, List.of(), List.of());
    }

    private AgentConversation resolveConversation(Long userId, Long conversationId, String message) {
        if (conversationId == null) {
            AgentConversation conversation = new AgentConversation();
            conversation.setUserId(userId);
            conversation.setTitle(message.length() > 24 ? message.substring(0, 24) : message);
            conversationMapper.insert(conversation);
            return conversation;
        }
        return requireConversation(userId, conversationId);
    }

    private AgentConversation requireConversation(Long userId, Long conversationId) {
        AgentConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null || !userId.equals(conversation.getUserId())) {
            throw new BusinessException(403, "无权访问该会话");
        }
        return conversation;
    }

    private List<LlmMessage> buildContext(Long conversationId) {
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(LlmMessage.system(SYSTEM_PROMPT));

        List<AgentMessage> history = messageMapper.selectList(Wrappers.<AgentMessage>lambdaQuery()
                .eq(AgentMessage::getConversationId, conversationId)
                .in(AgentMessage::getRole, "user", "assistant")
                .isNotNull(AgentMessage::getContent)
                .orderByDesc(AgentMessage::getId)
                .last("LIMIT " + properties.getMaxHistoryMessages()));
        List<AgentMessage> ordered = new ArrayList<>(history);
        ordered.sort((first, second) -> Long.compare(first.getId(), second.getId()));
        for (AgentMessage message : ordered) {
            if (StringUtils.hasText(message.getContent())) {
                messages.add(new LlmMessage(message.getRole(), message.getContent(), null, null));
            }
        }
        return messages;
    }

    private void saveMessage(Long conversationId, String role, String content,
                             String toolCalls, String toolCallId) {
        AgentMessage message = new AgentMessage();
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content);
        message.setToolCalls(toolCalls);
        message.setToolCallId(toolCallId);
        messageMapper.insert(message);
    }

    private void touchConversation(Long conversationId) {
        conversationMapper.update(null, Wrappers.<AgentConversation>lambdaUpdate()
                .set(AgentConversation::getUpdatedAt, LocalDateTime.now())
                .eq(AgentConversation::getId, conversationId));
    }

    private String toJson(List<LlmToolCall> calls) {
        try {
            return objectMapper.writeValueAsString(calls);
        } catch (Exception e) {
            return "[]";
        }
    }
}
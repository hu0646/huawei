package com.campus.food.service.agent.llm;

import com.campus.food.common.BusinessException;
import com.campus.food.config.AgentProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "agent.llm.mock-enabled", havingValue = "false", matchIfMissing = true)
public class DeepSeekLlmClient implements LlmClient {

    private final AgentProperties properties;
    private final ObjectMapper objectMapper;

    private volatile RestClient restClient;

    @Override
    public boolean isAvailable() {
        return StringUtils.hasText(properties.getLlm().getApiKey());
    }

    @Override
    public LlmResponse chat(List<LlmMessage> messages, List<Map<String, Object>> tools) {
        if (!isAvailable()) {
            throw new BusinessException(503, "尚未配置 DEEPSEEK_API_KEY，暂时无法调用模型");
        }
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", properties.getLlm().getModel());
        request.put("messages", messages.stream().map(this::toPayload).toList());
        request.put("stream", false);
        request.put("temperature", properties.getLlm().getTemperature());
        if (tools != null && !tools.isEmpty()) {
            request.put("tools", tools);
            request.put("tool_choice", "auto");
        }

        try {
            Map<String, Object> response = client().post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + properties.getLlm().getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {
                    });
            return parseResponse(response);
        } catch (RestClientResponseException e) {
            log.warn("DeepSeek 调用失败: status={}, body={}",
                    e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new BusinessException(502, "模型服务调用失败，请稍后再试");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("DeepSeek 调用异常", e);
            throw new BusinessException(502, "模型服务暂时不可用，请稍后再试");
        }
    }

    private RestClient client() {
        if (restClient == null) {
            synchronized (this) {
                if (restClient == null) {
                    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                    factory.setConnectTimeout((int) properties.getLlm().getConnectTimeout().toMillis());
                    factory.setReadTimeout((int) properties.getLlm().getRequestTimeout().toMillis());
                    restClient = RestClient.builder()
                            .baseUrl(properties.getLlm().getBaseUrl())
                            .requestFactory(factory)
                            .build();
                }
            }
        }
        return restClient;
    }

    @SuppressWarnings("unchecked")
    private LlmResponse parseResponse(Map<String, Object> response) {
        Object choicesValue = response == null ? null : response.get("choices");
        if (!(choicesValue instanceof List<?> choices) || choices.isEmpty()) {
            throw new BusinessException(502, "模型返回内容为空");
        }
        Object first = choices.get(0);
        if (!(first instanceof Map<?, ?> choice)) {
            throw new BusinessException(502, "模型返回格式异常");
        }
        Object messageValue = choice.get("message");
        if (!(messageValue instanceof Map<?, ?> message)) {
            throw new BusinessException(502, "模型返回消息为空");
        }
        LlmResponse result = new LlmResponse();
        Object content = message.get("content");
        result.setContent(content == null ? null : content.toString());
        Object toolCalls = message.get("tool_calls");
        if (toolCalls instanceof List<?> list && !list.isEmpty()) {
            result.setToolCalls(objectMapper.convertValue(list,
                    new TypeReference<List<LlmToolCall>>() {
                    }));
        }
        return result;
    }

    private Map<String, Object> toPayload(LlmMessage message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("role", message.getRole());
        if (message.getToolCalls() != null && !message.getToolCalls().isEmpty()) {
            payload.put("content", null);
            payload.put("tool_calls", message.getToolCalls().stream().map(call -> {
                Map<String, Object> function = new LinkedHashMap<>();
                function.put("name", call.getFunction().getName());
                function.put("arguments", call.getFunction().getArguments());
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", call.getId());
                item.put("type", call.getType() == null ? "function" : call.getType());
                item.put("function", function);
                return item;
            }).toList());
        } else {
            payload.put("content", message.getContent() == null ? "" : message.getContent());
        }
        if (message.getToolCallId() != null) {
            payload.put("tool_call_id", message.getToolCallId());
        }
        return payload;
    }
}
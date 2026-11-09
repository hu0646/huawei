package com.campus.food.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.campus.food.common.Result;
import com.campus.food.dto.agent.ChatRequest;
import com.campus.food.dto.agent.ChatResponse;
import com.campus.food.dto.agent.ConversationDetailVO;
import com.campus.food.dto.agent.ConversationVO;
import com.campus.food.service.agent.AgentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    @PostMapping("/chat")
    public Result<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return Result.ok(agentService.chat(StpUtil.getLoginIdAsLong(), request));
    }

    @GetMapping("/conversations")
    public Result<List<ConversationVO>> conversations() {
        return Result.ok(agentService.listConversations(StpUtil.getLoginIdAsLong()));
    }

    @GetMapping("/conversations/{id}")
    public Result<ConversationDetailVO> conversation(@PathVariable Long id) {
        return Result.ok(agentService.getConversation(StpUtil.getLoginIdAsLong(), id));
    }
}
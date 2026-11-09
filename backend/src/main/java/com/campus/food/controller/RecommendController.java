package com.campus.food.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.campus.food.common.Result;
import com.campus.food.dto.RecommendVO;
import com.campus.food.service.RecommendService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendService recommendService;

    @GetMapping
    public Result<List<RecommendVO>> list(@RequestParam(defaultValue = "10") int limit) {
        return Result.ok(recommendService.recommend(StpUtil.getLoginIdAsLong(), limit));
    }
}
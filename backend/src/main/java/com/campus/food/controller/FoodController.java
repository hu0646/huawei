package com.campus.food.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.campus.food.common.Result;
import com.campus.food.dto.FoodVO;
import com.campus.food.service.FoodService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FoodController {

    private final FoodService foodService;

    @GetMapping("/foods")
    public Result<List<FoodVO>> list(@RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) String category) {
        return Result.ok(foodService.list(keyword, category, optionalUserId()));
    }

    @GetMapping("/foods/{id}")
    public Result<FoodVO> detail(@PathVariable Long id) {
        return Result.ok(foodService.detail(id, optionalUserId()));
    }

    @GetMapping("/food-categories")
    public Result<List<String>> categories() {
        return Result.ok(foodService.categories());
    }

    private Long optionalUserId() {
        return StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
    }
}
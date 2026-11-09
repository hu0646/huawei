package com.campus.food.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.campus.food.common.Result;
import com.campus.food.dto.FoodVO;
import com.campus.food.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping
    public Result<List<FoodVO>> list() {
        return Result.ok(favoriteService.list(StpUtil.getLoginIdAsLong()));
    }

    @PostMapping("/{foodId}")
    public Result<Void> add(@PathVariable Long foodId) {
        favoriteService.add(StpUtil.getLoginIdAsLong(), foodId);
        return Result.ok();
    }

    @DeleteMapping("/{foodId}")
    public Result<Void> remove(@PathVariable Long foodId) {
        favoriteService.remove(StpUtil.getLoginIdAsLong(), foodId);
        return Result.ok();
    }
}
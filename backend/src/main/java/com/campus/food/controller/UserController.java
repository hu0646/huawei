package com.campus.food.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.campus.food.common.Result;
import com.campus.food.dto.PreferenceRequest;
import com.campus.food.dto.UserVO;
import com.campus.food.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public Result<UserVO> profile() {
        return Result.ok(userService.getProfile(StpUtil.getLoginIdAsLong()));
    }

    @PostMapping("/preference")
    public Result<UserVO> savePreference(@Valid @RequestBody PreferenceRequest request) {
        return Result.ok(userService.savePreference(StpUtil.getLoginIdAsLong(), request));
    }
}
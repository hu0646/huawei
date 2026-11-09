package com.campus.food.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.food.common.BusinessException;
import com.campus.food.dto.LoginRequest;
import com.campus.food.dto.LoginVO;
import com.campus.food.dto.RegisterRequest;
import com.campus.food.entity.SysUser;
import com.campus.food.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public LoginVO register(RegisterRequest request) {
        String username = request.getUsername().trim();
        Long exists = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, username));
        if (exists != null && exists > 0) {
            throw new BusinessException("用户名已被注册");
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(StringUtils.hasText(request.getNickname())
                ? request.getNickname().trim()
                : username);
        user.setOnboardingDone(0);
        userMapper.insert(user);

        StpUtil.login(user.getId());
        return new LoginVO(StpUtil.getTokenValue(), userService.toVO(user));
    }

    public LoginVO login(LoginRequest request) {
        SysUser user = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, request.getUsername().trim()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        StpUtil.login(user.getId());
        return new LoginVO(StpUtil.getTokenValue(), userService.toVO(user));
    }

    public void logout() {
        StpUtil.logout();
    }
}
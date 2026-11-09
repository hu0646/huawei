package com.campus.food.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.food.common.BusinessException;
import com.campus.food.dto.PreferenceRequest;
import com.campus.food.dto.TagVO;
import com.campus.food.dto.UserVO;
import com.campus.food.entity.SysUser;
import com.campus.food.entity.Tag;
import com.campus.food.entity.UserPreference;
import com.campus.food.mapper.SysUserMapper;
import com.campus.food.mapper.TagMapper;
import com.campus.food.mapper.UserPreferenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper userMapper;
    private final UserPreferenceMapper preferenceMapper;
    private final TagMapper tagMapper;

    public SysUser requireUser(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在，请重新登录");
        }
        return user;
    }

    public UserVO getProfile(Long userId) {
        return toVO(requireUser(userId));
    }

    public UserVO toVO(SysUser user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setBudgetMin(user.getBudgetMin());
        vo.setBudgetMax(user.getBudgetMax());
        vo.setOnboardingDone(user.getOnboardingDone());

        List<UserPreference> preferences = preferenceMapper.selectList(
                Wrappers.<UserPreference>lambdaQuery().eq(UserPreference::getUserId, user.getId()));
        if (!preferences.isEmpty()) {
            List<Long> tagIds = preferences.stream().map(UserPreference::getTagId).distinct().toList();
            Map<Long, Tag> tagMap = tagMapper.selectBatchIds(tagIds).stream()
                    .collect(Collectors.toMap(Tag::getId, tag -> tag));
            vo.setPreferences(preferences.stream()
                    .map(preference -> tagMap.get(preference.getTagId()))
                    .filter(Objects::nonNull)
                    .map(TagVO::from)
                    .toList());
        }
        return vo;
    }

    @Transactional
    public UserVO savePreference(Long userId, PreferenceRequest request) {
        requireUser(userId);

        BigDecimal budgetMin = request.getBudgetMin();
        BigDecimal budgetMax = request.getBudgetMax();
        if (budgetMin != null && budgetMax != null && budgetMin.compareTo(budgetMax) > 0) {
            throw new BusinessException("预算下限不能大于预算上限");
        }

        List<Long> tagIds = request.getTagIds() == null
                ? List.of()
                : request.getTagIds().stream().filter(Objects::nonNull).distinct().toList();
        if (!tagIds.isEmpty()) {
            Long validCount = tagMapper.selectCount(Wrappers.<Tag>lambdaQuery()
                    .in(Tag::getId, tagIds)
                    .eq(Tag::getStatus, 1));
            if (validCount == null || validCount.intValue() != tagIds.size()) {
                throw new BusinessException("包含无效的口味标签");
            }
        }

        userMapper.update(null, Wrappers.<SysUser>lambdaUpdate()
                .set(SysUser::getBudgetMin, budgetMin)
                .set(SysUser::getBudgetMax, budgetMax)
                .set(SysUser::getOnboardingDone, 1)
                .set(SysUser::getUpdatedAt, LocalDateTime.now())
                .eq(SysUser::getId, userId));

        preferenceMapper.delete(Wrappers.<UserPreference>lambdaQuery()
                .eq(UserPreference::getUserId, userId));
        for (Long tagId : tagIds) {
            UserPreference preference = new UserPreference();
            preference.setUserId(userId);
            preference.setTagId(tagId);
            preferenceMapper.insert(preference);
        }
        return getProfile(userId);
    }
}
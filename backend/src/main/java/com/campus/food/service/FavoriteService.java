package com.campus.food.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.food.common.BusinessException;
import com.campus.food.dto.FoodVO;
import com.campus.food.entity.Food;
import com.campus.food.entity.UserFavorite;
import com.campus.food.mapper.FoodMapper;
import com.campus.food.mapper.UserFavoriteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final UserFavoriteMapper favoriteMapper;
    private final FoodMapper foodMapper;
    private final FoodAssembler foodAssembler;
    private final UserService userService;

    public List<FoodVO> list(Long userId) {
        userService.requireUser(userId);
        List<UserFavorite> favorites = favoriteMapper.selectList(Wrappers.<UserFavorite>lambdaQuery()
                .eq(UserFavorite::getUserId, userId)
                .orderByDesc(UserFavorite::getCreatedAt)
                .orderByDesc(UserFavorite::getId));
        if (favorites.isEmpty()) {
            return List.of();
        }
        List<Long> foodIds = favorites.stream().map(UserFavorite::getFoodId).distinct().toList();
        Map<Long, Food> foodMap = foodMapper.selectBatchIds(foodIds).stream()
                .filter(food -> food.getStatus() != null && food.getStatus() == 1)
                .collect(Collectors.toMap(Food::getId, food -> food));
        List<Food> ordered = favorites.stream()
                .map(favorite -> foodMap.get(favorite.getFoodId()))
                .filter(Objects::nonNull)
                .toList();
        return foodAssembler.toVOList(ordered, userId);
    }

    public void add(Long userId, Long foodId) {
        userService.requireUser(userId);
        Food food = foodMapper.selectById(foodId);
        if (food == null || food.getStatus() == null || food.getStatus() != 1) {
            throw new BusinessException("美食不存在或已下架");
        }
        Long exists = favoriteMapper.selectCount(Wrappers.<UserFavorite>lambdaQuery()
                .eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getFoodId, foodId));
        if (exists != null && exists > 0) {
            return;
        }
        UserFavorite favorite = new UserFavorite();
        favorite.setUserId(userId);
        favorite.setFoodId(foodId);
        favoriteMapper.insert(favorite);
    }

    public void remove(Long userId, Long foodId) {
        favoriteMapper.delete(Wrappers.<UserFavorite>lambdaQuery()
                .eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getFoodId, foodId));
    }
}
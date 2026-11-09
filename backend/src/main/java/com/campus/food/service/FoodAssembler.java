package com.campus.food.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.food.dto.FoodVO;
import com.campus.food.dto.TagVO;
import com.campus.food.entity.Food;
import com.campus.food.entity.FoodTag;
import com.campus.food.entity.Tag;
import com.campus.food.entity.UserFavorite;
import com.campus.food.mapper.FoodTagMapper;
import com.campus.food.mapper.TagMapper;
import com.campus.food.mapper.UserFavoriteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FoodAssembler {

    private final FoodTagMapper foodTagMapper;
    private final TagMapper tagMapper;
    private final UserFavoriteMapper userFavoriteMapper;

    public Map<Long, Tag> loadTagMap() {
        return tagMapper.selectList(Wrappers.<Tag>lambdaQuery().eq(Tag::getStatus, 1))
                .stream()
                .collect(Collectors.toMap(Tag::getId, tag -> tag));
    }

    public Map<Long, List<Tag>> loadFoodTags(Collection<Long> foodIds) {
        if (foodIds == null || foodIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<FoodTag> relations = foodTagMapper.selectList(
                Wrappers.<FoodTag>lambdaQuery().in(FoodTag::getFoodId, foodIds));
        if (relations.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Tag> tagMap = loadTagMap();
        Map<Long, List<Tag>> result = new HashMap<>();
        for (FoodTag relation : relations) {
            Tag tag = tagMap.get(relation.getTagId());
            if (tag != null) {
                result.computeIfAbsent(relation.getFoodId(), key -> new ArrayList<>()).add(tag);
            }
        }
        result.values().forEach(list ->
                list.sort(Comparator.comparing(Tag::getSortNo, Comparator.nullsLast(Comparator.naturalOrder()))));
        return result;
    }

    public Set<Long> loadFavoriteIds(Long userId, Collection<Long> foodIds) {
        if (userId == null || foodIds == null || foodIds.isEmpty()) {
            return Collections.emptySet();
        }
        return userFavoriteMapper.selectList(Wrappers.<UserFavorite>lambdaQuery()
                        .eq(UserFavorite::getUserId, userId)
                        .in(UserFavorite::getFoodId, foodIds))
                .stream()
                .map(UserFavorite::getFoodId)
                .collect(Collectors.toSet());
    }

    public List<FoodVO> toVOList(List<Food> foods, Long userId) {
        if (foods == null || foods.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> foodIds = foods.stream().map(Food::getId).toList();
        Map<Long, List<Tag>> foodTags = loadFoodTags(foodIds);
        Set<Long> favoriteIds = loadFavoriteIds(userId, foodIds);
        return foods.stream()
                .map(food -> toVO(food,
                        foodTags.getOrDefault(food.getId(), Collections.emptyList()),
                        favoriteIds.contains(food.getId())))
                .toList();
    }

    public FoodVO toVO(Food food, List<Tag> tags, boolean favorite) {
        FoodVO vo = new FoodVO();
        vo.setId(food.getId());
        vo.setName(food.getName());
        vo.setDescription(food.getDescription());
        vo.setCanteen(food.getCanteen());
        vo.setCategory(food.getCategory());
        vo.setPrice(food.getPrice());
        vo.setRating(food.getRating());
        vo.setPopularity(food.getPopularity());
        vo.setEmoji(food.getEmoji());
        vo.setTags(tags.stream().map(TagVO::from).toList());
        vo.setFavorite(favorite);
        return vo;
    }
}
package com.campus.food.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.food.common.BusinessException;
import com.campus.food.dto.FoodVO;
import com.campus.food.entity.Food;
import com.campus.food.mapper.FoodMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FoodService {

    private final FoodMapper foodMapper;
    private final FoodAssembler foodAssembler;

    public List<FoodVO> list(String keyword, String category, Long userId) {
        LambdaQueryWrapper<Food> query = Wrappers.<Food>lambdaQuery()
                .eq(Food::getStatus, 1);
        if (StringUtils.hasText(category)) {
            query.eq(Food::getCategory, category.trim());
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            query.and(wrapper -> wrapper
                    .like(Food::getName, kw)
                    .or().like(Food::getCanteen, kw)
                    .or().like(Food::getDescription, kw));
        }
        query.orderByDesc(Food::getRating)
                .orderByDesc(Food::getPopularity)
                .orderByAsc(Food::getId);
        return foodAssembler.toVOList(foodMapper.selectList(query), userId);
    }

    public FoodVO detail(Long foodId, Long userId) {
        Food food = foodMapper.selectById(foodId);
        if (food == null || food.getStatus() == null || food.getStatus() != 1) {
            throw new BusinessException("美食不存在或已下架");
        }
        return foodAssembler.toVOList(List.of(food), userId).get(0);
    }

    public List<String> categories() {
        return foodMapper.selectCategories();
    }
}
package com.campus.food.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.food.entity.Food;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface FoodMapper extends BaseMapper<Food> {

    @Select("SELECT DISTINCT category FROM food WHERE status = 1 AND category IS NOT NULL ORDER BY category")
    List<String> selectCategories();
}
package com.campus.food.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendVO {

    private FoodVO food;

    private int score;

    private List<String> reasons;
}
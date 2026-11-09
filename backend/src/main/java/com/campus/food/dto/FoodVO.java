package com.campus.food.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class FoodVO {

    private Long id;

    private String name;

    private String description;

    private String canteen;

    private String category;

    private BigDecimal price;

    private BigDecimal rating;

    private Integer popularity;

    private String emoji;

    private List<TagVO> tags = new ArrayList<>();

    private boolean favorite;
}
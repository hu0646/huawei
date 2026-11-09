package com.campus.food.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class UserVO {

    private Long id;

    private String username;

    private String nickname;

    private BigDecimal budgetMin;

    private BigDecimal budgetMax;

    private Integer onboardingDone;

    private List<TagVO> preferences = new ArrayList<>();
}
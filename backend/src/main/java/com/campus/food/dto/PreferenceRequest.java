package com.campus.food.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PreferenceRequest {

    private List<Long> tagIds;

    @DecimalMin(value = "0", message = "预算下限不能为负数")
    private BigDecimal budgetMin;

    @DecimalMin(value = "0", message = "预算上限不能为负数")
    private BigDecimal budgetMax;
}
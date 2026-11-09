package com.campus.food.service.agent;

import com.campus.food.common.BusinessException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class AgentParams {

    private AgentParams() {
    }

    public static Long requiredLong(Map<String, Object> arguments, String key) {
        Long value = optionalLong(arguments, key);
        if (value == null) {
            throw new BusinessException("缺少参数 " + key);
        }
        return value;
    }

    public static Long optionalLong(Map<String, Object> arguments, String key) {
        Object raw = arguments.get(key);
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(raw.toString().trim());
        } catch (NumberFormatException e) {
            throw new BusinessException("参数 " + key + " 必须是整数");
        }
    }

    public static Integer optionalInt(Map<String, Object> arguments, String key, Integer defaultValue) {
        Object raw = arguments.get(key);
        if (raw == null) {
            return defaultValue;
        }
        if (raw instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(raw.toString().trim());
        } catch (NumberFormatException e) {
            throw new BusinessException("参数 " + key + " 必须是整数");
        }
    }

    public static BigDecimal optionalDecimal(Map<String, Object> arguments, String key) {
        Object raw = arguments.get(key);
        if (raw == null || raw.toString().isBlank()) {
            return null;
        }
        try {
            BigDecimal value = new BigDecimal(raw.toString().trim());
            if (value.signum() < 0) {
                throw new BusinessException("参数 " + key + " 不能为负数");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new BusinessException("参数 " + key + " 必须是数字");
        }
    }

    public static String optionalString(Map<String, Object> arguments, String key) {
        Object raw = arguments.get(key);
        if (raw == null) {
            return null;
        }
        String value = raw.toString().trim();
        return value.isEmpty() ? null : value;
    }

    public static List<Long> optionalLongList(Map<String, Object> arguments, String key) {
        Object raw = arguments.get(key);
        if (raw == null) {
            return null;
        }
        if (!(raw instanceof List<?> list)) {
            throw new BusinessException("参数 " + key + " 必须是数组");
        }
        List<Long> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Number number) {
                result.add(number.longValue());
                continue;
            }
            try {
                result.add(Long.parseLong(item.toString().trim()));
            } catch (NumberFormatException e) {
                throw new BusinessException("参数 " + key + " 必须是整数数组");
            }
        }
        return result;
    }
}
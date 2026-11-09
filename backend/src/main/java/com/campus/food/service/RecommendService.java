package com.campus.food.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.food.dto.FoodVO;
import com.campus.food.dto.RecommendVO;
import com.campus.food.entity.Food;
import com.campus.food.entity.SysUser;
import com.campus.food.entity.Tag;
import com.campus.food.entity.UserPreference;
import com.campus.food.mapper.FoodMapper;
import com.campus.food.mapper.UserPreferenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendService {

    private static final int TASTE_MAX = 50;
    private static final int BUDGET_MAX = 30;
    private static final int RATING_MAX = 15;
    private static final int POPULARITY_MAX = 5;

    private static final int DEFAULT_TASTE_SCORE = 20;
    private static final int DEFAULT_BUDGET_SCORE = 20;
    private static final int BELOW_BUDGET_SCORE = 25;
    private static final int POPULARITY_REASON_THRESHOLD = 2000;

    private final FoodMapper foodMapper;
    private final UserPreferenceMapper preferenceMapper;
    private final FoodAssembler foodAssembler;
    private final UserService userService;

    public List<RecommendVO> recommend(Long userId, int limit) {
        return recommend(userId, limit, null, null, null, null);
    }

    public List<RecommendVO> recommend(Long userId, int limit,
                                       BigDecimal budgetMinOverride,
                                       BigDecimal budgetMaxOverride,
                                       Set<Long> tagIdsOverride,
                                       String category) {
        SysUser user = userService.requireUser(userId);
        int safeLimit = Math.max(1, Math.min(limit, 50));

        List<Food> foods = foodMapper.selectList(Wrappers.<Food>lambdaQuery()
                .eq(Food::getStatus, 1)
                .orderByAsc(Food::getId));
        if (StringUtils.hasText(category)) {
            String targetCategory = category.trim();
            foods = foods.stream()
                    .filter(food -> targetCategory.equals(food.getCategory()))
                    .toList();
        }
        if (foods.isEmpty()) {
            return List.of();
        }

        Set<Long> preferredTagIds;
        if (tagIdsOverride != null) {
            preferredTagIds = tagIdsOverride;
        } else {
            preferredTagIds = preferenceMapper.selectList(
                            Wrappers.<UserPreference>lambdaQuery()
                                    .eq(UserPreference::getUserId, userId))
                    .stream()
                    .map(UserPreference::getTagId)
                    .collect(Collectors.toSet());
        }

        boolean budgetProvided = budgetMinOverride != null || budgetMaxOverride != null;
        BigDecimal budgetMin = budgetProvided ? budgetMinOverride : user.getBudgetMin();
        BigDecimal budgetMax = budgetProvided ? budgetMaxOverride : user.getBudgetMax();

        List<Long> foodIds = foods.stream().map(Food::getId).toList();
        Map<Long, List<Tag>> foodTags = foodAssembler.loadFoodTags(foodIds);
        Set<Long> favoriteIds = foodAssembler.loadFavoriteIds(userId, foodIds);

        int maxPopularity = foods.stream()
                .mapToInt(food -> food.getPopularity() == null ? 0 : food.getPopularity())
                .max()
                .orElse(0);

        List<RecommendVO> results = new ArrayList<>();
        for (Food food : foods) {
            List<Tag> tags = foodTags.getOrDefault(food.getId(), List.of());
            List<String> reasons = new ArrayList<>();

            int tasteScore = scoreTaste(tags, preferredTagIds, reasons);
            int budgetScore = scoreBudget(food.getPrice(), budgetMin, budgetMax, reasons);
            int ratingScore = scoreRating(food.getRating(), reasons);
            int popularityScore = scorePopularity(food.getPopularity(), maxPopularity, reasons);
            int totalScore = tasteScore + budgetScore + ratingScore + popularityScore;

            if (reasons.isEmpty()) {
                reasons.add("综合评分与热度推荐");
            }

            FoodVO foodVO = foodAssembler.toVO(food, tags, favoriteIds.contains(food.getId()));
            results.add(new RecommendVO(foodVO, totalScore, reasons));
        }

        results.sort(Comparator.comparingInt(RecommendVO::getScore).reversed()
                .thenComparing(vo -> vo.getFood().getId()));
        return results.stream().limit(safeLimit).toList();
    }

    private int scoreTaste(List<Tag> tags, Set<Long> preferredTagIds, List<String> reasons) {
        if (preferredTagIds.isEmpty()) {
            return DEFAULT_TASTE_SCORE;
        }
        List<String> matchedNames = tags.stream()
                .filter(tag -> preferredTagIds.contains(tag.getId()))
                .map(Tag::getName)
                .toList();
        if (matchedNames.isEmpty()) {
            return 0;
        }
        int score = (int) Math.round(matchedNames.size() * 1.0 / preferredTagIds.size() * TASTE_MAX);
        reasons.add("符合你喜欢的「" + String.join("、", matchedNames) + "」口味");
        return Math.min(TASTE_MAX, score);
    }

    private int scoreBudget(BigDecimal price, BigDecimal budgetMin, BigDecimal budgetMax, List<String> reasons) {
        if (price == null) {
            price = BigDecimal.ZERO;
        }
        if (budgetMin == null && budgetMax == null) {
            return DEFAULT_BUDGET_SCORE;
        }
        if (budgetMin != null && budgetMax != null) {
            if (price.compareTo(budgetMin) >= 0 && price.compareTo(budgetMax) <= 0) {
                reasons.add("价格 " + formatMoney(price) + " 元在你的 "
                        + formatMoney(budgetMin) + "-" + formatMoney(budgetMax) + " 元预算内");
                return BUDGET_MAX;
            }
            if (price.compareTo(budgetMin) < 0) {
                reasons.add("价格 " + formatMoney(price) + " 元低于你的预算下限，更划算");
                return BELOW_BUDGET_SCORE;
            }
            return overBudgetTail(price, budgetMax, reasons);
        }
        if (budgetMax == null) {
            if (price.compareTo(budgetMin) >= 0) {
                reasons.add("价格 " + formatMoney(price) + " 元符合你的预算下限");
                return BUDGET_MAX;
            }
            reasons.add("价格 " + formatMoney(price) + " 元低于你的预算下限，更划算");
            return BELOW_BUDGET_SCORE;
        }
        if (price.compareTo(budgetMax) <= 0) {
            reasons.add("价格 " + formatMoney(price) + " 元在你的 " + formatMoney(budgetMax) + " 元预算内");
            return BUDGET_MAX;
        }
        return overBudgetTail(price, budgetMax, reasons);
    }

    private int overBudgetTail(BigDecimal price, BigDecimal budgetMax, List<String> reasons) {
        if (budgetMax.compareTo(BigDecimal.ZERO) <= 0) {
            reasons.add("价格 " + formatMoney(price) + " 元超出你的预算上限");
            return 0;
        }
        BigDecimal overRatio = price.subtract(budgetMax)
                .divide(budgetMax, 4, RoundingMode.HALF_UP);
        int penalty = (int) Math.round(overRatio.doubleValue() * BUDGET_MAX);
        int score = Math.max(0, BUDGET_MAX - penalty);
        if (overRatio.compareTo(BigDecimal.ONE) > 0) {
            reasons.add("价格 " + formatMoney(price) + " 元明显超出你的预算上限");
        } else {
            reasons.add("价格 " + formatMoney(price) + " 元略高于你的预算上限");
        }
        return score;
    }

    private int scoreRating(BigDecimal rating, List<String> reasons) {
        if (rating == null) {
            return 0;
        }
        int score = (int) Math.round(rating.doubleValue() / 5.0 * RATING_MAX);
        if (rating.compareTo(new BigDecimal("4.5")) >= 0) {
            reasons.add("评分 " + formatMoney(rating) + " 分，口碑很好");
        } else if (rating.compareTo(new BigDecimal("4.0")) >= 0) {
            reasons.add("评分 " + formatMoney(rating) + " 分，评价不错");
        }
        return score;
    }

    private int scorePopularity(Integer popularity, int maxPopularity, List<String> reasons) {
        if (popularity == null || popularity <= 0 || maxPopularity <= 0) {
            return 0;
        }
        int score = (int) Math.round(popularity * 1.0 / maxPopularity * POPULARITY_MAX);
        if (popularity >= POPULARITY_REASON_THRESHOLD) {
            reasons.add("月售 " + popularity + "+ 份，校园热门");
        }
        return Math.min(POPULARITY_MAX, score);
    }

    private String formatMoney(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
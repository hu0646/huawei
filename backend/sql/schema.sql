-- ============================================================
-- 校园美食推荐小助手 MySQL 8 初始化脚本
-- 执行方式: mysql -uroot -p < schema.sql
-- ============================================================
CREATE DATABASE IF NOT EXISTS `campus_food`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
USE `campus_food`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `agent_message`;
DROP TABLE IF EXISTS `agent_conversation`;
DROP TABLE IF EXISTS `user_favorite`;
DROP TABLE IF EXISTS `user_preference`;
DROP TABLE IF EXISTS `food_tag`;
DROP TABLE IF EXISTS `food`;
DROP TABLE IF EXISTS `tag`;
DROP TABLE IF EXISTS `sys_user`;

-- 用户表
CREATE TABLE `sys_user` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`        VARCHAR(50)  NOT NULL COMMENT '登录名',
    `password`        VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码',
    `nickname`        VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    `budget_min`      DECIMAL(10, 2) DEFAULT NULL COMMENT '预算下限（元）',
    `budget_max`      DECIMAL(10, 2) DEFAULT NULL COMMENT '预算上限（元）',
    `onboarding_done` TINYINT      NOT NULL DEFAULT 0 COMMENT '是否完成首次引导',
    `created_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_user_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表';

-- 美食表
CREATE TABLE `food` (
    `id`          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(80)    NOT NULL COMMENT '美食名称',
    `description` VARCHAR(300)   DEFAULT NULL COMMENT '简介',
    `canteen`     VARCHAR(80)    DEFAULT NULL COMMENT '食堂/档口',
    `category`    VARCHAR(40)    DEFAULT NULL COMMENT '分类',
    `price`       DECIMAL(10, 2) NOT NULL COMMENT '价格（元）',
    `rating`      DECIMAL(2, 1)  NOT NULL DEFAULT 0.0 COMMENT '评分（0-5）',
    `popularity`  INT            NOT NULL DEFAULT 0 COMMENT '热度（月售份数）',
    `emoji`       VARCHAR(16)    DEFAULT NULL COMMENT '展示图标',
    `status`      TINYINT        NOT NULL DEFAULT 1 COMMENT '1上架 0下架',
    `created_at`  DATETIME       DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME       DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_food_category` (`category`),
    KEY `idx_food_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '美食表';

-- 口味标签字典表（全站唯一标签来源）
CREATE TABLE `tag` (
    `id`      BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`    VARCHAR(30) NOT NULL COMMENT '标签名称',
    `code`    VARCHAR(40) NOT NULL COMMENT '标签编码',
    `sort_no` INT         NOT NULL DEFAULT 0 COMMENT '排序',
    `status`  TINYINT     NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tag_code` (`code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '口味标签字典';

-- 美食-标签关联表
CREATE TABLE `food_tag` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT,
    `food_id`    BIGINT   NOT NULL,
    `tag_id`     BIGINT   NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_food_tag` (`food_id`, `tag_id`),
    KEY `idx_food_tag_tag` (`tag_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '美食标签关联';

-- 用户口味偏好表
CREATE TABLE `user_preference` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT,
    `user_id`    BIGINT   NOT NULL,
    `tag_id`     BIGINT   NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_preference` (`user_id`, `tag_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户口味偏好';

-- 用户收藏表
CREATE TABLE `user_favorite` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT,
    `user_id`    BIGINT   NOT NULL,
    `food_id`    BIGINT   NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_favorite` (`user_id`, `food_id`),
    KEY `idx_user_favorite_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户收藏';


-- Agent 会话表
CREATE TABLE `agent_conversation` (
    `id`         BIGINT      NOT NULL AUTO_INCREMENT,
    `user_id`    BIGINT      NOT NULL,
    `title`      VARCHAR(80) DEFAULT NULL,
    `created_at` DATETIME    DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME    DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_agent_conversation_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Agent 会话';

-- Agent 消息表
CREATE TABLE `agent_message` (
    `id`              BIGINT      NOT NULL AUTO_INCREMENT,
    `conversation_id` BIGINT      NOT NULL,
    `role`            VARCHAR(20) NOT NULL,
    `content`         TEXT,
    `tool_calls`      TEXT,
    `tool_call_id`    VARCHAR(80) DEFAULT NULL,
    `created_at`      DATETIME    DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_agent_message_conversation` (`conversation_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Agent 消息';

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 以下为标签字典与美食种子数据
-- ============================================================

-- ============================================================
-- 标签字典（全站唯一来源，前端通过 GET /api/tags 获取）
-- ============================================================
INSERT INTO tag (id, name, code, sort_no, status) VALUES
(1, '麻辣', 'mala', 1, 1),
(2, '香辣', 'xiangla', 2, 1),
(3, '清淡', 'qingdan', 3, 1),
(4, '酸甜', 'suantian', 4, 1),
(5, '咸鲜', 'xianxian', 5, 1),
(6, '甜品', 'tianpin', 6, 1),
(7, '面食', 'mianshi', 7, 1),
(8, '米饭', 'mifan', 8, 1),
(9, '烧烤', 'shaokao', 9, 1),
(10, '汤类', 'tanglei', 10, 1),
(11, '素食', 'sushi', 11, 1),
(12, '快餐', 'kuaican', 12, 1),
(13, '早餐', 'zaocan', 13, 1),
(14, '小吃', 'xiaochi', 14, 1),
(15, '川味', 'chuanwei', 15, 1),
(16, '粤味', 'yuewei', 16, 1),
(17, '西北风味', 'xibei', 17, 1),
(18, '日韩', 'rihan', 18, 1),
(19, '西式', 'xishi', 19, 1),
(20, '卤味', 'luwei', 20, 1);

-- ============================================================
-- 美食种子数据
-- ============================================================
INSERT INTO food (id, name, description, canteen, category, price, rating, popularity, emoji, status) VALUES
(1, '麻辣香锅', '自选食材现炒，麻辣鲜香，学生党聚餐首选。', '一食堂·川味窗口', '地方风味', 22.00, 4.8, 2300, '🌶️', 1),
(2, '兰州牛肉面', '一清二白三红四绿，汤头醇厚，面条筋道。', '二食堂·西北面馆', '面食粉类', 15.00, 4.7, 2800, '🍜', 1),
(3, '黄焖鸡米饭', '鸡腿肉焖得软烂入味，汤汁拌饭一绝。', '一食堂·家常菜', '盖饭简餐', 18.00, 4.5, 1900, '🍗', 1),
(4, '柳州螺蛳粉', '酸笋香浓，汤底够味，配炸腐竹和花生。', '三食堂·柳州风味', '面食粉类', 16.00, 4.6, 2100, '🍲', 1),
(5, '重庆小面', '红油香辣，花椒提麻，二两小面刚刚好。', '二食堂·川渝面档', '面食粉类', 12.00, 4.4, 1600, '🍝', 1),
(6, '番茄鸡蛋盖饭', '酸甜开胃，鸡蛋嫩滑，家常味十足。', '一食堂·家常菜', '盖饭简餐', 14.00, 4.3, 1200, '🍅', 1),
(7, '清蒸鲈鱼套餐', '现蒸鲈鱼配时蔬米饭，少油清淡。', '教工餐厅', '盖饭简餐', 28.00, 4.9, 800, '🐟', 1),
(8, '皮蛋瘦肉粥', '慢熬米粥绵密顺滑，早餐暖胃之选。', '三食堂·粥铺', '早餐粥点', 8.00, 4.2, 900, '🥣', 1),
(9, '广式肠粉', '米浆现蒸，皮薄馅足，淋上豉油很鲜。', '二食堂·粤味档', '早餐粥点', 10.00, 4.5, 1500, '🥟', 1),
(10, '蜜汁叉烧饭', '叉烧肥瘦相间，蜜汁微甜，配青菜解腻。', '二食堂·粤味档', '盖饭简餐', 20.00, 4.6, 1300, '🍖', 1),
(11, '香酥炸鸡汉堡', '外皮酥脆，鸡肉多汁，赶课时的能量补给。', '西区快餐站', '快餐西餐', 24.00, 4.1, 2500, '🍔', 1),
(12, '芝士披萨', '现烤薄底，芝士拉丝，适合和朋友分食。', '西区快餐站', '快餐西餐', 32.00, 4.4, 1100, '🍕', 1),
(13, '石锅拌饭', '锅巴焦香，蔬菜丰富，拌上辣酱很满足。', '三食堂·韩餐档', '日韩料理', 23.00, 4.7, 1400, '🍚', 1),
(14, '韩式甜辣炸鸡', '甜辣酱裹满炸鸡，外酥里嫩，越吃越香。', '三食堂·韩餐档', '日韩料理', 26.00, 4.6, 1700, '🍗', 1),
(15, '三文鱼寿司拼盘', '新鲜三文鱼搭配醋饭，清爽不油腻。', '三食堂·日料档', '日韩料理', 30.00, 4.5, 600, '🍣', 1),
(16, '炭烤鸡翅', '炭火现烤，外焦里嫩，撒上孜然辣椒面。', '南门夜市', '夜宵烧烤', 12.00, 4.8, 3000, '🍢', 1),
(17, '东北烤冷面', '酸甜酱汁加鸡蛋火腿，铁板现烤。', '南门夜市', '夜宵烧烤', 9.00, 4.7, 2600, '🥘', 1),
(18, '老潼关肉夹馍', '白吉馍酥脆，腊汁肉软糯，肉香浓郁。', '南门夜市', '夜宵烧烤', 10.00, 4.6, 2200, '🥙', 1),
(19, '重庆酸辣粉', '红薯粉爽滑，酸辣汤底，开胃又过瘾。', '南门夜市', '面食粉类', 8.00, 4.5, 2000, '🍜', 1),
(20, '关东煮', '萝卜、鱼丸、海带结慢煮入味，汤头清甜。', '南门夜市', '夜宵烧烤', 11.00, 4.3, 1800, '🍢', 1),
(21, '鸡胸肉蔬菜沙拉', '高蛋白低脂，搭配油醋汁，健身友好。', '健身餐厅', '甜品轻食', 19.00, 4.0, 500, '🥗', 1),
(22, '茄子豆腐煲', '茄子软糯豆腐入味，素菜也能很下饭。', '一食堂·家常菜', '盖饭简餐', 15.00, 4.2, 700, '🍲', 1),
(23, '红糖糍粑', '外皮微脆内里软糯，红糖黄豆粉香甜。', '南门夜市', '甜品轻食', 7.00, 4.7, 900, '🍡', 1),
(24, '姜撞奶双皮奶', '奶香浓郁口感嫩滑，冷藏后风味更佳。', '二食堂·甜品站', '甜品轻食', 9.00, 4.8, 1000, '🍮', 1),
(25, '豆浆油条', '现磨豆浆配酥脆油条，经典早餐组合。', '三食堂·早餐铺', '早餐粥点', 5.00, 4.1, 1500, '🥛', 1),
(26, '鲜肉小笼包', '皮薄汁多，一笼八个，蘸醋更解腻。', '二食堂·早餐铺', '早餐粥点', 8.00, 4.6, 2400, '🥟', 1),
(27, '自选冒菜', '按份自选荤素，麻辣汤底，一人食火锅。', '一食堂·川味窗口', '地方风味', 20.00, 4.7, 2100, '🥘', 1),
(28, '台式卤肉饭', '卤肉肥瘦适中，卤汁拌饭，配溏心蛋。', '二食堂·卤味档', '盖饭简餐', 17.00, 4.5, 1600, '🍛', 1),
(29, '金汤酸菜鱼', '鱼片嫩滑，金汤酸辣开胃，分量扎实。', '三食堂·川湘菜', '地方风味', 29.00, 4.8, 1200, '🐟', 1),
(30, '咖喱鸡饭', '咖喱浓郁微辛，土豆鸡肉炖得入味。', '三食堂·东南亚档', '盖饭简餐', 21.00, 4.4, 1300, '🍛', 1);

-- ============================================================
-- 美食-标签关联
-- ============================================================
INSERT INTO food_tag (food_id, tag_id) VALUES
(1, 1), (1, 15), (1, 8),
(2, 17), (2, 7), (2, 5),
(3, 8), (3, 5), (3, 12),
(4, 14), (4, 2), (4, 7),
(5, 15), (5, 1), (5, 7),
(6, 8), (6, 4), (6, 12),
(7, 3), (7, 8), (7, 5),
(8, 3), (8, 13), (8, 10),
(9, 16), (9, 13), (9, 14),
(10, 16), (10, 8), (10, 5),
(11, 12), (11, 19), (11, 5),
(12, 19), (12, 5), (12, 12),
(13, 18), (13, 8), (13, 5),
(14, 18), (14, 2), (14, 9),
(15, 18), (15, 3),
(16, 9), (16, 2), (16, 14),
(17, 14), (17, 7), (17, 4),
(18, 17), (18, 14), (18, 5),
(19, 15), (19, 2), (19, 14),
(20, 14), (20, 3), (20, 10),
(21, 11), (21, 3), (21, 19),
(22, 11), (22, 5), (22, 8),
(23, 6), (23, 14),
(24, 6), (24, 16),
(25, 13), (25, 3),
(26, 13), (26, 7), (26, 5),
(27, 15), (27, 1), (27, 12),
(28, 20), (28, 8), (28, 5),
(29, 15), (29, 10), (29, 2),
(30, 5), (30, 8), (30, 12);
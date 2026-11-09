DROP TABLE IF EXISTS agent_message;
DROP TABLE IF EXISTS agent_conversation;
DROP TABLE IF EXISTS user_favorite;
DROP TABLE IF EXISTS user_preference;
DROP TABLE IF EXISTS food_tag;
DROP TABLE IF EXISTS food;
DROP TABLE IF EXISTS tag;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(100) NOT NULL,
    nickname VARCHAR(50),
    budget_min DECIMAL(10, 2),
    budget_max DECIMAL(10, 2),
    onboarding_done TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME,
    updated_at DATETIME
);
CREATE UNIQUE INDEX uk_sys_user_username ON sys_user (username);

CREATE TABLE food (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    description VARCHAR(300),
    canteen VARCHAR(80),
    category VARCHAR(40),
    price DECIMAL(10, 2) NOT NULL,
    rating DECIMAL(2, 1) NOT NULL DEFAULT 0,
    popularity INT NOT NULL DEFAULT 0,
    emoji VARCHAR(16),
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME,
    updated_at DATETIME
);

CREATE TABLE tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(30) NOT NULL,
    code VARCHAR(40) NOT NULL,
    sort_no INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1
);
CREATE UNIQUE INDEX uk_tag_code ON tag (code);

CREATE TABLE food_tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    food_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    created_at DATETIME
);
CREATE UNIQUE INDEX uk_food_tag ON food_tag (food_id, tag_id);

CREATE TABLE user_preference (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    created_at DATETIME
);
CREATE UNIQUE INDEX uk_user_preference ON user_preference (user_id, tag_id);

CREATE TABLE user_favorite (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    food_id BIGINT NOT NULL,
    created_at DATETIME
);
CREATE UNIQUE INDEX uk_user_favorite ON user_favorite (user_id, food_id);

CREATE TABLE agent_conversation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(80),
    created_at DATETIME,
    updated_at DATETIME
);
CREATE INDEX idx_agent_conversation_user ON agent_conversation (user_id);

CREATE TABLE agent_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT,
    tool_calls TEXT,
    tool_call_id VARCHAR(80),
    created_at DATETIME
);
CREATE INDEX idx_agent_message_conversation ON agent_message (conversation_id);

-- 用户 AI 推荐缓存表（若已建表可跳过）
CREATE TABLE IF NOT EXISTS `forum_user_recommendation` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `post_ids` JSON NOT NULL COMMENT '推荐帖子ID数组，按展示顺序',
    `summary` VARCHAR(500) DEFAULT NULL COMMENT 'AI推荐理由摘要',
    `action_count` INT DEFAULT 0 COMMENT '生成时参考的行为条数',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '首次生成时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近刷新时间',
    UNIQUE KEY `uk_user_id` (`user_id`),
    FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户AI推荐结果缓存表';

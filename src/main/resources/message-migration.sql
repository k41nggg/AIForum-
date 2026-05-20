-- 通知消息表升级（在 aiforum 库执行）
-- 若已有旧版 sys_message 且可清空，可直接 DROP 后重建；否则用下方 ALTER

-- 方案 A：重建（无历史数据时推荐）
DROP TABLE IF EXISTS `sys_message`;
CREATE TABLE `sys_message` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
    `receiver_id` BIGINT NOT NULL COMMENT '接收者用户ID',
    `sender_id` BIGINT DEFAULT NULL COMMENT '触发者ID，NULL表示系统',
    `type` INT NOT NULL COMMENT '消息类型，见 NotificationType',
    `title` VARCHAR(255) DEFAULT NULL COMMENT '标题',
    `content` TEXT NOT NULL COMMENT '正文',
    `target_type` VARCHAR(20) DEFAULT NULL COMMENT '关联类型: post, comment, user, category',
    `target_id` BIGINT DEFAULT NULL COMMENT '关联ID',
    `extra_id` BIGINT DEFAULT NULL COMMENT '扩展ID，如帖子ID（评论类通知跳转用）',
    `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '0未读 1已读',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_receiver_read` (`receiver_id`, `is_read`),
    INDEX `idx_receiver_time` (`receiver_id`, `create_time`),
    FOREIGN KEY (`receiver_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内通知消息表';

-- AIForum Database Design SQL

-- 1. User Management Tables
CREATE TABLE `sys_user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Primary Key',
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT 'Username',
    `password` VARCHAR(100) NOT NULL COMMENT 'Encrypted Password',
    `nickname` VARCHAR(50) COMMENT 'Nickname',
    `avatar` VARCHAR(255) COMMENT 'Avatar URL',
    `email` VARCHAR(100) UNIQUE COMMENT 'Email Address',
    `bio` TEXT COMMENT 'Biography/Introduction',
    `points` INT DEFAULT 0 COMMENT 'User Points',
    `level` INT DEFAULT 1 COMMENT 'User Level',
    `role` VARCHAR(20) DEFAULT 'USER' COMMENT 'Role: USER, ADMIN',
    `status` TINYINT DEFAULT 1 COMMENT 'Status: 1-Active, 0-Banned',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT DEFAULT 0 COMMENT 'Soft Delete: 0-No, 1-Yes'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User Table';

-- 2. Category Management
CREATE TABLE `sys_category` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(50) NOT NULL UNIQUE COMMENT 'Category Name',
    `description` VARCHAR(255) COMMENT 'Description',
    `parent_id` BIGINT DEFAULT 0 COMMENT 'Parent Category ID',
    `sort` INT DEFAULT 0 COMMENT 'Sort Order',
    `icon` VARCHAR(255) COMMENT 'Category Icon',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `is_deleted` TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Category Table';

-- 3. Post Management
CREATE TABLE `forum_post` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT 'Author ID',
    `category_id` BIGINT NOT NULL COMMENT 'Category ID',
    `title` VARCHAR(255) NOT NULL COMMENT 'Post Title',
    `content` LONGTEXT NOT NULL COMMENT 'Post Content',
    `view_count` INT DEFAULT 0 COMMENT 'View Count',
    `like_count` INT DEFAULT 0 COMMENT 'Like Count',
    `collect_count` INT DEFAULT 0 COMMENT 'Collect Count',
    `comment_count` INT DEFAULT 0 COMMENT 'Comment Count',
    `status` TINYINT DEFAULT 0 COMMENT 'Status: 0-Draft, 1-Pending, 2-Published, 3-Off-shelf',
    `is_top` TINYINT DEFAULT 0 COMMENT 'Is Sticky/Top: 0-No, 1-Yes',
    `is_essence` TINYINT DEFAULT 0 COMMENT 'Is Essence: 0-No, 1-Yes',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT DEFAULT 0,
    FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`),
    FOREIGN KEY (`category_id`) REFERENCES `sys_category` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Post Table';

-- 4. Comment Management (Multi-level/Nested)
CREATE TABLE `forum_comment` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `post_id` BIGINT NOT NULL COMMENT 'Post ID',
    `user_id` BIGINT NOT NULL COMMENT 'Commenter ID',
    `parent_id` BIGINT DEFAULT 0 COMMENT 'Parent Comment ID',
    `root_id` BIGINT DEFAULT 0 COMMENT 'Root Comment ID',
    `content` TEXT NOT NULL COMMENT 'Comment Content',
    `like_count` INT DEFAULT 0 COMMENT 'Like Count',
    `dislike_count` INT DEFAULT 0 COMMENT 'Dislike Count',
    `status` TINYINT DEFAULT 1 COMMENT 'Status: 1-Active, 0-Pending Archive/Audit',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `is_deleted` TINYINT DEFAULT 0,
    INDEX `idx_post_id` (`post_id`),
    INDEX `idx_root_id` (`root_id`),
    FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Comment Table';

-- 5. User Interaction (Likes/Collections)
CREATE TABLE `forum_user_action` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `target_id` BIGINT NOT NULL COMMENT 'Post or Comment ID',
    `type` TINYINT NOT NULL COMMENT 'Type: 1-Like Post, 2-Collect Post, 3-Like Comment',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_target_type` (`user_id`, `target_id`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User Behavior/Action Table';

-- 6. Subscription
CREATE TABLE `forum_subscription` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `category_id` BIGINT NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_category` (`user_id`, `category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Category Subscription';

-- 7. Reporting System
CREATE TABLE `sys_report` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `reporter_id` BIGINT NOT NULL,
    `target_id` BIGINT NOT NULL COMMENT 'Post/Comment ID',
    `type` TINYINT NOT NULL COMMENT 'Type: 1-Post, 2-Comment',
    `reason` VARCHAR(255) NOT NULL COMMENT 'Report Reason',
    `status` TINYINT DEFAULT 0 COMMENT 'Status: 0-Pending, 1-Handled',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Report Table';

-- 8. System Config & Logs
CREATE TABLE `sys_config` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `config_key` VARCHAR(50) NOT NULL UNIQUE,
    `config_value` TEXT,
    `remark` VARCHAR(255),
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='System Configuration';

CREATE TABLE `sys_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT,
    `username` VARCHAR(50),
    `operation` VARCHAR(100) COMMENT 'User Operation',
    `method` VARCHAR(200) COMMENT 'Request Method',
    `params` TEXT COMMENT 'Request Parameters',
    `time` BIGINT COMMENT 'Execution Time (ms)',
    `ip` VARCHAR(50) COMMENT 'Request IP',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='System Logs';

-- 9. Message System
CREATE TABLE `sys_message` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `sender_id` BIGINT COMMENT 'Sender (0 for system notice)',
    `receiver_id` BIGINT NOT NULL,
    `content` TEXT NOT NULL,
    `type` TINYINT COMMENT 'Type: 1-System, 2-Comment, 3-Like, 4-Private Msg',
    `is_read` TINYINT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Message Center';

-- 10. Data Dictionary
CREATE TABLE `sys_dict` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `dict_type` VARCHAR(50) NOT NULL COMMENT 'Dictionary Type',
    `dict_label` VARCHAR(50) NOT NULL COMMENT 'Display Name',
    `dict_value` VARCHAR(100) NOT NULL COMMENT 'Value',
    `is_default` TINYINT DEFAULT 0,
    `sort` INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Data Dictionary';

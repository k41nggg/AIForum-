-- AIForum 数据库设计 SQL

-- 1. 用户管理模块相关表
CREATE TABLE `sys_user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名（登录凭证，唯一）',
    `password` VARCHAR(100) NOT NULL COMMENT '加密存储的密码（使用BCrypt加密）',
    `nickname` VARCHAR(50) COMMENT '用户昵称（用于展示）',
    `avatar` VARCHAR(255) COMMENT '头像资源URL地址',
    `email` VARCHAR(100) UNIQUE COMMENT '电子邮箱地址（唯一，用于找回密码或通知）',
    `bio` TEXT COMMENT '个人简介/个性签名',
    `points` INT DEFAULT 0 COMMENT '用户账户积分，用于等级提升或兑换',
    `level` INT DEFAULT 1 COMMENT '用户当前等级',
    `role` VARCHAR(20) DEFAULT 'USER' COMMENT '用户角色：USER-普通用户, ADMIN-管理员',
    `status` TINYINT DEFAULT 1 COMMENT '账户状态：1-正常使用, 0-禁用/黑名单',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '账户注册/创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '资料最后一次更新时间',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记：0-未删除, 1-已逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户基础信息表';

-- 2. 话题分类管理
CREATE TABLE `sys_category` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '分类主键ID',
    `name` VARCHAR(50) NOT NULL UNIQUE COMMENT '分类名称（如：技术专区、生活闲谈）',
    `description` VARCHAR(255) COMMENT '分类详细介绍',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父分类ID（支持无限层级架构，0表示顶级分类）',
    `sort` INT DEFAULT 0 COMMENT '排序权重，数值越小越靠前',
    `icon` VARCHAR(255) COMMENT '分类图标或相关标识符',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '分类创建时间',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='话题板块分类表';

-- 3. 帖子/内容管理
CREATE TABLE `forum_post` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '帖子主键ID',
    `user_id` BIGINT NOT NULL COMMENT '发布者用户ID（关联sys_user）',
    `category_id` BIGINT NOT NULL COMMENT '所属分类ID（关联sys_category）',
    `title` VARCHAR(255) NOT NULL COMMENT '帖子的标题内容',
    `content` LONGTEXT NOT NULL COMMENT '帖子的详细正文内容',
    `view_count` INT DEFAULT 0 COMMENT '总浏览/点击量统计',
    `like_count` INT DEFAULT 0 COMMENT '获赞总数统计',
    `collect_count` INT DEFAULT 0 COMMENT '被收藏总数统计',
    `comment_count` INT DEFAULT 0 COMMENT '该帖子下的评论总数',
    `status` TINYINT DEFAULT 0 COMMENT '内容状态：0-草稿箱, 1-待审核, 2-已发布, 3-下架违规',
    `is_top` TINYINT DEFAULT 0 COMMENT '是否置顶展示：0-否, 1-是',
    `is_essence` TINYINT DEFAULT 0 COMMENT '是否设为精华帖：0-否, 1-是',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '帖子发布/创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后编辑/修改时间',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`),
    FOREIGN KEY (`category_id`) REFERENCES `sys_category` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社区帖子内容表';

-- 4. 评论/回复管理（支持多级嵌套盖楼）
CREATE TABLE `forum_comment` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '评论主键ID',
    `post_id` BIGINT NOT NULL COMMENT '关联的具体帖子ID',
    `user_id` BIGINT NOT NULL COMMENT '评论者的用户ID',
    `parent_id` BIGINT DEFAULT 0 COMMENT '直接上级评论的ID（实现回复功能，0表示针对帖子主体的评论）',
    `root_id` BIGINT DEFAULT 0 COMMENT '根评论ID（用于聚合整个对话楼层）',
    `content` TEXT NOT NULL COMMENT '评论的具体文字内容',
    `like_count` INT DEFAULT 0 COMMENT '评论点赞数',
    `dislike_count` INT DEFAULT 0 COMMENT '评论点踩数',
    `status` TINYINT DEFAULT 1 COMMENT '评论可见状态：1-正常显示, 0-审核中/屏蔽',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX `idx_post_id` (`post_id`) COMMENT '帖子ID普通索引',
    INDEX `idx_root_id` (`root_id`) COMMENT '根评论ID索引，用于快速查询楼层内容',
    FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子评论回复表';

-- 5. 用户行为记录（点赞、收藏关联表）
CREATE TABLE `forum_user_action` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    `user_id` BIGINT NOT NULL COMMENT '操作者用户ID',
    `target_id` BIGINT NOT NULL COMMENT '操作的目标ID（帖子ID或评论ID）',
    `type` TINYINT NOT NULL COMMENT '行为类型：1-点赞帖子, 2-收藏帖子, 3-点赞评论',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '行为发生时间',
    UNIQUE KEY `uk_user_target_type` (`user_id`, `target_id`, `type`) COMMENT '唯一索引，防止对同一目标重复点赞/收藏'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户行为动作记录表（去重）';

-- 6. 分类订阅/话题关注功能
CREATE TABLE `forum_subscription` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '订阅记录ID',
    `user_id` BIGINT NOT NULL COMMENT '关注者的用户ID',
    `category_id` BIGINT NOT NULL COMMENT '关注的话题分类ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '开始关注时间',
    UNIQUE KEY `uk_user_category` (`user_id`, `category_id`) COMMENT '确保每个用户对同一分类只订阅一次'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='话题分类订阅关联表';

-- 7. 举报反馈系统
CREATE TABLE `sys_report` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '举报单ID',
    `reporter_id` BIGINT NOT NULL COMMENT '发起举报的用户ID',
    `target_id` BIGINT NOT NULL COMMENT '被举报的内容ID（帖子或评论）',
    `type` TINYINT NOT NULL COMMENT '被举报目标类型：1-帖子内容, 2-评论回复',
    `reason` VARCHAR(255) NOT NULL COMMENT '举报的原因/具体理由',
    `status` TINYINT DEFAULT 0 COMMENT '处理状态：0-待处理, 1-已核实处理',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '举报提交时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内容违规举报记录表';

-- 8. 系统配置与操作日志
CREATE TABLE `sys_config` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '配置ID',
    `config_key` VARCHAR(50) NOT NULL UNIQUE COMMENT '配置项的唯一健名（如：SITE_NAME）',
    `config_value` TEXT COMMENT '配置项对应的数值信息',
    `remark` VARCHAR(255) COMMENT '该配置项的备注说明',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '配置更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='全局系统基础参数配置表';

CREATE TABLE `sys_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    `user_id` BIGINT COMMENT '操作人员的用户ID',
    `username` VARCHAR(50) COMMENT '操作人员的账户名称',
    `operation` VARCHAR(100) COMMENT '具体记录的操作描述',
    `method` VARCHAR(200) COMMENT '请求执行的后端方法名',
    `params` TEXT COMMENT '请求传入的原始参数内容',
    `time` BIGINT COMMENT '后端接口处理耗时（单位：毫秒）',
    `ip` VARCHAR(50) COMMENT '发起请求的IP地址信息',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '日志生成的记录时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统操作审计日志表';

-- 9. 消息中心与系统通知
CREATE TABLE `sys_message` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
    `sender_id` BIGINT COMMENT '发送者ID（0或NULL表示为系统自动发送的消息）',
    `receiver_id` BIGINT NOT NULL COMMENT '接收该消息的用户ID',
    `content` TEXT NOT NULL COMMENT '具体的消息通知内容',
    `type` TINYINT COMMENT '消息子类型：1-系统群发通知, 2-获得的评论回复, 3-获得的点赞反馈, 4-用户私信',
    `is_read` TINYINT DEFAULT 0 COMMENT '查看状态：0-未读, 1-已读',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '消息送达时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统消息通知中心表';

-- 10. 全局数据字典模块
CREATE TABLE `sys_dict` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '字典主键ID',
    `dict_type` VARCHAR(50) NOT NULL COMMENT '数据字典的分类代码（如：USER_ROLE_TYPE）',
    `dict_label` VARCHAR(50) NOT NULL COMMENT '前端展示给用户看的文字名称',
    `dict_value` VARCHAR(100) NOT NULL COMMENT '后端业务逻辑使用的实际数值',
    `is_default` TINYINT DEFAULT 0 COMMENT '是否设为该类型的默认选项：0-否, 1-是',
    `sort` INT DEFAULT 0 COMMENT '级联排序权重'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统万能数据字典表';

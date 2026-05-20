package com.zhidao.demo.constant;

public final class NotificationType {

    private NotificationType() {}

    public static final int SYSTEM = 10;

    public static final int POST_AUDIT_APPROVED = 20;
    public static final int POST_AUDIT_REJECTED = 21;
    public static final int POST_REMOVED = 22;

    public static final int POST_COMMENTED = 30;
    public static final int COMMENT_REPLIED = 31;

    public static final int POST_LIKED = 40;
    public static final int COMMENT_LIKED = 41;

    public static final int USER_FOLLOWED = 50;

    public static final int FOLLOWEE_NEW_POST = 60;
    public static final int CATEGORY_NEW_POST = 61;
}

package com.insurance.rag.context;

import com.insurance.rag.enums.UserRoleEnum;

/**
 * 当前登录用户上下文（ThreadLocal）。
 * 生产环境从 JWT / Session 解析后放入；demo 由 Controller 从请求参数注入。
 * 必须在请求结束后 clear()，防止线程复用串权限。
 */
public class SecurityContext {

    private static final ThreadLocal<UserRoleEnum> CURRENT_ROLE = new ThreadLocal<>();
    private static final ThreadLocal<Long> CURRENT_ORG_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_USER = new ThreadLocal<>();

    public static void set(UserRoleEnum role, Long orgId, String user) {
        CURRENT_ROLE.set(role);
        CURRENT_ORG_ID.set(orgId);
        CURRENT_USER.set(user);
    }

    public static UserRoleEnum getRole() {
        return CURRENT_ROLE.get() == null ? UserRoleEnum.ORG_MANAGER : CURRENT_ROLE.get();
    }

    public static Long getOrgId() {
        return CURRENT_ORG_ID.get();
    }

    public static String getUser() {
        return CURRENT_USER.get() == null ? "anonymous" : CURRENT_USER.get();
    }

    public static boolean isAdmin() {
        return UserRoleEnum.ADMIN == getRole();
    }

    public static void clear() {
        CURRENT_ROLE.remove();
        CURRENT_ORG_ID.remove();
        CURRENT_USER.remove();
    }
}

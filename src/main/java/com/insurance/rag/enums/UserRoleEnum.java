package com.insurance.rag.enums;

/**
 * 用户角色：
 * ADMIN       总管：全部工具、全机构数据
 * ORG_MANAGER 机构管理员：仅本机构数据，禁止底层表结构类工具
 */
public enum UserRoleEnum {
    ADMIN,
    ORG_MANAGER
}

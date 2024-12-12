package com.tianji.learning.utils;

/**
 * @author 鲁昊天
 * @date 2024/12/12
 */
public class TableInfoContext {
    private static final ThreadLocal<String> TABLE_NAME = new ThreadLocal<>();
    public static void setTableName(String tableName) {
        TABLE_NAME.set(tableName);
    }
    public static String getTableName() {
        return TABLE_NAME.get();
    }
    public static void remove() {
        TABLE_NAME.remove();
    }
}

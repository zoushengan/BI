package com.example.oj.utlis;

import org.apache.commons.lang3.StringUtils;

/**
 * SQL 工具
 */
public class SqlUtils {

    public static boolean validSortField(String sortFiled) {
        if (StringUtils.isBlank(sortFiled)) {
            return false;
        }
        return !StringUtils.containsAny(sortFiled, "=", "(",")","");
    }
}

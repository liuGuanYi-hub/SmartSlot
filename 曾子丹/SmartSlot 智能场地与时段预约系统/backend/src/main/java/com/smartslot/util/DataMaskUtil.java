package com.smartslot.util;

import org.springframework.util.StringUtils;

/**
 * 敏感数据安全脱敏工具类 (Enterprise Data Security & Compliance)
 */
public class DataMaskUtil {

    /**
     * 手机号脱敏：保留前3后4，中间4位为星号 (例: 139****5678)
     */
    public static String maskPhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 7) {
            return "******";
        }
        if (phone.length() == 11) {
            return phone.substring(0, 3) + "****" + phone.substring(7);
        }
        int len = phone.length();
        return phone.substring(0, 2) + "****" + phone.substring(Math.max(2, len - 2));
    }

    /**
     * 中文姓名脱敏：
     * 2个字：张三 -> 张*
     * 3个字：李小龙 -> 李*龙
     * 4个字及以上：欧阳六六 -> 欧**六
     */
    public static String maskName(String name) {
        if (!StringUtils.hasText(name)) {
            return "*";
        }
        int len = name.length();
        if (len <= 1) {
            return "*";
        }
        if (len == 2) {
            return name.charAt(0) + "*";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(name.charAt(0));
        for (int i = 1; i < len - 1; i++) {
            sb.append("*");
        }
        sb.append(name.charAt(len - 1));
        return sb.toString();
    }

    /**
     * 核销码脱敏：6位核销码隐藏中间2位 (例: 83**01)
     */
    public static String maskVerifyCode(String code) {
        if (!StringUtils.hasText(code) || code.length() < 4) {
            return "******";
        }
        int len = code.length();
        return code.substring(0, 2) + "**" + code.substring(len - 2);
    }
}

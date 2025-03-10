package com.criel.train.common.exception;

/**
 * 自定义异常枚举类
 */

public enum BusinessExceptionEnum {

    MOBILE_IS_EXIST("手机号已被注册");

    private String desc;

    BusinessExceptionEnum(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}

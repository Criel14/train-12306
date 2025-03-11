package com.criel.train.common.exception;

/**
 * 自定义异常枚举类
 */

public enum BusinessExceptionEnum {

    MOBILE_IS_EXIST("手机号已被注册"),
    MOBILE_IS_NOT_EXIST("手机号未注册"),
    CODE_IS_ERROR("验证码错误"),
    CODE_IS_EMPTY("验证码为空"),
    GET_CODE_FIRST("请先获取验证码");

    private String desc;

    BusinessExceptionEnum(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}

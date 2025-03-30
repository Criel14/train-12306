package com.criel.train.common.exception;

import lombok.Getter;

/**
 * 自定义异常枚举类
 */

@Getter
public enum BusinessExceptionEnum {

    MOBILE_IS_EXIST("手机号已被注册"),
    MOBILE_IS_NOT_EXIST("手机号未注册"),
    CODE_IS_ERROR("验证码错误"),
    CODE_IS_EMPTY("验证码为空"),
    CODE_IS_EXPIRED("验证码未获取或已过期，请重新获取"),
    GET_CODE_FIRST("请先获取验证码"),
    BUSINESS_STATION_NAME_UNIQUE_ERROR("车站已存在"),
    BUSINESS_TRAIN_CODE_UNIQUE_ERROR("车次编号已存在"),
    BUSINESS_TRAIN_STATION_INDEX_UNIQUE_ERROR("该车次中该站序已存在"),
    BUSINESS_TRAIN_STATION_NAME_UNIQUE_ERROR("该车次中该站名已存在"),
    BUSINESS_TRAIN_CARRIAGE_INDEX_UNIQUE_ERROR("该厢号已存在"),
    BUSINESS_TRAIN_STATION_IN_TIME_OUT_TIME_ERROR("出站时间不得早于入站时间"),
    CONFIRM_ORDER_TICKET_COUNT_ERROR("余票不足");

    private final String desc;

    BusinessExceptionEnum(String desc) {
        this.desc = desc;
    }

}

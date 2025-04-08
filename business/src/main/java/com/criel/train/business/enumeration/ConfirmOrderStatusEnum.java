package com.criel.train.business.enumeration;

/**
 * 订单状态
 */
public enum ConfirmOrderStatusEnum {

    INIT("I", "初始化"),
    PENDING("P", "处理中"),
    SUCCESS("S", "成功"),
    FAILURE("F", "失败"),
    EMPTY("E", "无票"),
    CANCEL("C", "取消");

    private String code;

    private String desc;

    ConfirmOrderStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public String toString() {
        return "ConfirmOrderStatusEnum{" +
                "code='" + code + '\'' +
                ", desc='" + desc + '\'' +
                "} " + super.toString();
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 根据code获取枚举值
     */
    public static ConfirmOrderStatusEnum getByCode(String code) {
        for (ConfirmOrderStatusEnum statusEnum : ConfirmOrderStatusEnum.values()) {
            if (statusEnum.getCode().equals(code)) {
                return statusEnum;
            }
        }
        return null;
    }

}

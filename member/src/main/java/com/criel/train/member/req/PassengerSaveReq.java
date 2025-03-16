package com.criel.train.member.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

/**
 * 保存乘车人请求参数
 */
@Data
public class PassengerSaveReq {
    private Long id;

    private Long memberId;

    @NotBlank(message = "姓名不能为空")
    private String name;

    @NotBlank(message = "身份证不能为空")
    private String idCard;

    @NotBlank(message = "乘车人类型不能为空")
    private String type;

    private Date createTime;

    private Date updateTime;
}

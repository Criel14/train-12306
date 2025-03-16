package com.criel.train.member.req;

import jakarta.validation.constraints.NotBlank;
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

    private String idCard;

    private String type;

    private Date createTime;

    private Date updateTime;
}

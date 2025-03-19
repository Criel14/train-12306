package com.criel.train.member.req;

import com.fasterxml.jackson.annotation.JsonFormat;
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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
}

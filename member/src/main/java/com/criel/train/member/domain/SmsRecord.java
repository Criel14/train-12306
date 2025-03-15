package com.criel.train.member.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class SmsRecord implements Serializable {
    private String mobile;
    private String code;
    // 有效期的毫秒数
    private long expireTime;
    // 业务类型
    private String businessType;
    // 发送时间
    private LocalDateTime sendTime;
}

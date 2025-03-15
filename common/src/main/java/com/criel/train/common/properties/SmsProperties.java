package com.criel.train.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 短信验证码配置类
 */
@Component
@Data
@ConfigurationProperties("train.sms")
public class SmsProperties {
    // 过期时间（毫秒）
    private long expireTime;
}

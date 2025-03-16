package com.criel.train.gateway.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * jwt配置类
 */
@Component
@Data
@ConfigurationProperties("train.jwt")
public class JwtProperties {
    // key
    private String memberSecretKey;
    // 过期时间（毫秒）
    private int memberTtl;
}

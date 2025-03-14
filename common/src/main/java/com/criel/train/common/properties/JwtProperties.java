package com.criel.train.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties("train.jwt")
public class JwtProperties {
    private String memberSecretKey;
    private String memberTtl;
}

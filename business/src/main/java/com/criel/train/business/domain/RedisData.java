package com.criel.train.business.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class RedisData {
    private LocalDateTime expireTime;
    private Object data;
}

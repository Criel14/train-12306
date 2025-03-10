package com.criel.train.common.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class BusinessException extends RuntimeException{

    private BusinessExceptionEnum anEnum;

    public BusinessException(BusinessExceptionEnum anEnum) {
        this.anEnum = anEnum;
    }
}

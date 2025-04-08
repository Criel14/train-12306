package com.criel.train.business.dto;

import lombok.Data;

import java.util.Date;

@Data
public class ConfirmOrderMQDto {

    /**
     * 日期
     *
     */
    private Date date;

    /**
     * 车次编号
     */
    private String trainCode;

    /**
     * 日志ID(流水号)
     */
    private String LogId;
}

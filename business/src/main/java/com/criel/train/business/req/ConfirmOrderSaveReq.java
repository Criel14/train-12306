package com.criel.train.business.req;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConfirmOrderSaveReq {

    /**
     * 用户ID(无需传入，传入后赋值)
     */
    private Long memberId;

    /**
     * 订单ID(无需传入，传入后赋值)
     *
     */
    private long confirmOrderId;

    /**
     * 日志ID(无需传入，传入后赋值)
     */
    private String LogId;

    /**
     * 日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @NotNull(message = "【日期】不能为空")
    private Date date;

    /**
     * 车次编号
     */
    @NotBlank(message = "【车次编号】不能为空")
    private String trainCode;

    /**
     * 出发站
     */
    @NotBlank(message = "【出发站】不能为空")
    private String start;

    /**
     * 到达站
     */
    @NotBlank(message = "【到达站】不能为空")
    private String end;

    /**
     * 余票ID
     */
    @NotNull(message = "【余票ID】不能为空")
    private Long dailyTrainTicketId;

    /**
     * 车票，前端传的是Json数据，自动映射
     */
    @NotNull(message = "【车票】不能为空")
    private List<ConfirmOrderTicketReq> tickets;

}

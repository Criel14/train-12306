package com.criel.train.business.req;

import com.criel.train.common.req.PageReq;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class DailyTrainTicketQueryReq extends PageReq {

    // 车次编号
    private String trainCode;

    // 日期
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date date;

    // 出发站（站名）
    private String start;

    // 到达站（站名）
    private String end;

}

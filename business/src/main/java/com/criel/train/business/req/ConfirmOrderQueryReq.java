package com.criel.train.business.req;

import com.criel.train.common.req.PageReq;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class ConfirmOrderQueryReq extends PageReq {

    private String trainCode;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date date;

}

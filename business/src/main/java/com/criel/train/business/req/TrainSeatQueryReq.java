package com.criel.train.business.req;

import com.criel.train.common.req.PageReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class TrainSeatQueryReq extends PageReq {
    private String trainCode;

}

package com.criel.train.member.req;

import com.criel.train.common.req.PageReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class TicketQueryReq extends PageReq {

    private Long memberId;

}

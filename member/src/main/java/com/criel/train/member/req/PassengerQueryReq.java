package com.criel.train.member.req;

import com.criel.train.common.req.PageReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 乘车人分页查询请求
 * 用于查询某个用户下的乘车人
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PassengerQueryReq extends PageReq {
    private Long memberId;
}

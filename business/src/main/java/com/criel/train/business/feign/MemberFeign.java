package com.criel.train.business.feign;

import com.criel.train.common.req.MemberTicketReq;
import com.criel.train.common.resp.CommonResp;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("member")
//@FeignClient(name = "member", url = "http://localhost:8001")
public interface MemberFeign {

    @GetMapping("/member/feign/ticket/save")
    CommonResp save(@RequestBody MemberTicketReq req);
}

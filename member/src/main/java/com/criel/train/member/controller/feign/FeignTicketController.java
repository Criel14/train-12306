package com.criel.train.member.controller.feign;

import com.criel.train.common.req.MemberTicketReq;
import com.criel.train.common.resp.CommonResp;
import com.criel.train.member.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提供给远程调用的接口
 */
@RestController
@RequestMapping("/feign/ticket")
public class FeignTicketController {

    @Autowired
    private TicketService ticketService;

    @PostMapping("/save")
    public CommonResp save(@Valid @RequestBody MemberTicketReq memberTicketReq) {
        ticketService.saveMemberTicket(memberTicketReq);
        return CommonResp.success();
    }
}

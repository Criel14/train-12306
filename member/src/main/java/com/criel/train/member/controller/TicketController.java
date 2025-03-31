package com.criel.train.member.controller;

import com.criel.train.common.context.LoginMemberContext;
import com.criel.train.common.resp.CommonResp;
import com.criel.train.common.resp.PageResp;
import com.criel.train.member.req.TicketQueryReq;
import com.criel.train.member.resp.TicketQueryResp;
import com.criel.train.member.service.TicketService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ticket")
public class TicketController {

    @Resource
    private TicketService ticketService;

    @GetMapping("/query-list")
    public CommonResp<PageResp<TicketQueryResp>> queryList(@Valid TicketQueryReq req) {
        req.setMemberId(LoginMemberContext.getId());
        PageResp<TicketQueryResp> list = ticketService.queryList(req);
        return CommonResp.success(list);
    }

}

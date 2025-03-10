package com.criel.train.member.controller;

import com.criel.train.common.resp.CommonResp;
import com.criel.train.member.req.MemberRegisterReq;
import com.criel.train.member.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/member")
public class MemberController {

    @Autowired
    private MemberService memberService;

    @GetMapping("/testConnect")
    public CommonResp<Long> testConnect() {
        Long count =memberService.count();
        return CommonResp.success(count);
    }

    @PostMapping("/register")
    public CommonResp<Long>  register(MemberRegisterReq req) {
        Long memberId = memberService.register(req);
        return CommonResp.success(memberId);
    }

}

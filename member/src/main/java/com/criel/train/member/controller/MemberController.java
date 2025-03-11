package com.criel.train.member.controller;

import com.criel.train.common.resp.CommonResp;
import com.criel.train.member.req.MemberGetCodeReq;
import com.criel.train.member.req.MemberLoginReq;
import com.criel.train.member.req.MemberRegisterReq;
import com.criel.train.member.resp.MemberLoginResp;
import com.criel.train.member.service.MemberService;
import jakarta.validation.Valid;
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

    @GetMapping("/test-connect")
    public CommonResp<Long> testConnect() {
        Long count = memberService.count();
        return CommonResp.success(count);
    }

    @PostMapping("/register")
    public CommonResp<Long> register(@Valid MemberRegisterReq req) {
        Long memberId = memberService.register(req);
        return CommonResp.success(memberId);
    }

    @PostMapping("/code")
    public CommonResp<String> getCode(@Valid MemberGetCodeReq req) {
        String code = memberService.getCode(req);
        return CommonResp.success(code);
    }

    @PostMapping("/login")
    public CommonResp<MemberLoginResp> login(@Valid MemberLoginReq req) {
        MemberLoginResp memberLoginResp = memberService.login(req);
        return CommonResp.success(memberLoginResp);
    }
}

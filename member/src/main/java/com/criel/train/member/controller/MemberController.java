package com.criel.train.member.controller;

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
    public long testConnect() {
        return memberService.count();
    }

    @PostMapping("/register")
    public long  register(String mobile) {
        return memberService.register(mobile);
    }

}

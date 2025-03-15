package com.criel.train.member.controller;

import cn.hutool.core.bean.BeanUtil;
import com.criel.train.common.properties.JwtProperties;
import com.criel.train.common.resp.CommonResp;
import com.criel.train.common.util.JwtUtil;
import com.criel.train.member.req.MemberGetCodeReq;
import com.criel.train.member.req.MemberLoginReq;
import com.criel.train.member.req.MemberRegisterReq;
import com.criel.train.member.resp.MemberLoginResp;
import com.criel.train.member.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/member")
public class MemberController {

    @Autowired
    private MemberService memberService;

    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 测试连接
     * @return
     */
    @GetMapping("/test-connect")
    public CommonResp<Long> testConnect() {
        Long count = memberService.count();
        return CommonResp.success(count);
    }

    /**
     * 获取用户数量
     * @return
     */
    @GetMapping("/count")
    public CommonResp<Long> count() {
        Long count = memberService.count();
        return CommonResp.success(count);
    }

    /**
     * 注册
     * 未使用
     * @param req
     * @return
     */
    @PostMapping("/register")
    public CommonResp<Long> register(@Valid @RequestBody MemberRegisterReq req) {
        Long memberId = memberService.register(req);
        return CommonResp.success(memberId);
    }

    /**
     * 获取验证码
     * @param req
     * @return
     */
    @PostMapping("/code")
    public CommonResp getCode(@Valid @RequestBody MemberGetCodeReq req) {
        memberService.getCode(req);
        return CommonResp.success();
    }

    /**
     * 登录
     * @param req
     * @return
     */
    @PostMapping("/login")
    public CommonResp<MemberLoginResp> login(@Valid @RequestBody MemberLoginReq req) {
        MemberLoginResp memberLoginResp = memberService.login(req);
        // 获取token
        String token = JwtUtil.createToken(
                BeanUtil.beanToMap(memberLoginResp),
                jwtProperties.getMemberSecretKey(),
                jwtProperties.getMemberTtl());
        // 为返回信息加上token
        memberLoginResp.setToken(token);
        return CommonResp.success(memberLoginResp);
    }

}

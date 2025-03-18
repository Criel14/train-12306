package com.criel.train.member.controller;

import com.criel.train.common.context.LoginMemberContext;
import com.criel.train.common.resp.CommonResp;
import com.criel.train.member.config.MemberApplication;
import com.criel.train.member.req.PassengerQueryReq;
import com.criel.train.member.req.PassengerSaveReq;
import com.criel.train.member.resp.PassengerQueryResp;
import com.criel.train.member.service.PassengerService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/passenger")
public class PassengerController {

    private static final Logger LOG = LoggerFactory.getLogger(MemberApplication.class);

    @Autowired
    PassengerService passengerService;

    /**
     * 新增乘车人
     * @param passengerSaveReq
     */
    @PostMapping("/save")
    public CommonResp save(@Valid @RequestBody PassengerSaveReq passengerSaveReq) {
        passengerService.save(passengerSaveReq);
        return CommonResp.success();
    }

    /**
     * 用户分页查询乘客
     * @param passengerQueryReq
     * @return
     */
    @GetMapping("/query-list")
    public CommonResp<List<PassengerQueryResp>> queryList(@Valid PassengerQueryReq passengerQueryReq) {
        // 赋值memberId
        passengerQueryReq.setMemberId(LoginMemberContext.getId());
        List<PassengerQueryResp> passengerQueryRespList = passengerService.queryList(passengerQueryReq);
        return CommonResp.success(passengerQueryRespList);
    }


}

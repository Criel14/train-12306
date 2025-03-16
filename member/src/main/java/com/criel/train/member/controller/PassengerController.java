package com.criel.train.member.controller;

import com.criel.train.common.resp.CommonResp;
import com.criel.train.member.req.PassengerSaveReq;
import com.criel.train.member.service.PassengerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/passenger")
public class PassengerController {

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


}

package com.criel.train.member.controller;

import com.criel.train.common.context.LoginMemberContext;
import com.criel.train.common.resp.CommonResp;
import com.criel.train.common.resp.PageResp;
import com.criel.train.member.req.PassengerQueryReq;
import com.criel.train.member.req.PassengerSaveReq;
import com.criel.train.member.resp.PassengerQueryResp;
import com.criel.train.member.service.PassengerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/passenger")
public class PassengerController {

    @Autowired
    PassengerService passengerService;

    /**
     * 新增/修改乘车人
     * 如果passengerSaveReq参数中有id，则为修改，没有则为新增
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
    public CommonResp<PageResp<PassengerQueryResp>> queryList(@Valid PassengerQueryReq passengerQueryReq) {
        // 赋值memberId
        passengerQueryReq.setMemberId(LoginMemberContext.getId());
        PageResp<PassengerQueryResp> passengerQueryRespList = passengerService.queryList(passengerQueryReq);
        return CommonResp.success(passengerQueryRespList);
    }

    /**
     * 删除乘车人
     * @param id
     * @return
     */
    @DeleteMapping("/delete/{id}")
    public CommonResp deleteById(@PathVariable Long id) {
        passengerService.deleteById(id);
        return CommonResp.success();
    }

    /**
     * 查询当前登录用户的乘车人列表
     * @return
     */
    @GetMapping("/query-mine")
    public CommonResp<List<PassengerQueryResp>> queryMine() {
        List<PassengerQueryResp> passengerQueryRespList = passengerService.queryMine();
        return CommonResp.success(passengerQueryRespList);
    }

}

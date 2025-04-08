package com.criel.train.business.controller;

import com.criel.train.business.req.ConfirmOrderQueryReq;
import com.criel.train.business.req.ConfirmOrderSaveReq;
import com.criel.train.business.resp.ConfirmOrderQueryResp;
import com.criel.train.business.service.BeforeConfirmOrderService;
import com.criel.train.business.service.ConfirmOrderService;
import com.criel.train.common.resp.CommonResp;
import com.criel.train.common.resp.PageResp;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/confirm-order")
public class ConfirmOrderController {

    @Resource
    private BeforeConfirmOrderService beforeConfirmOrderService;

    @PostMapping("/confirm")
    public CommonResp<Object> confirm(@Valid @RequestBody ConfirmOrderSaveReq req) {
        beforeConfirmOrderService.beforeConfirm(req);
        return CommonResp.success();
    }

}

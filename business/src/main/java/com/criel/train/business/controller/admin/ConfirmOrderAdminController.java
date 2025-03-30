package com.criel.train.business.controller.admin;

import com.criel.train.common.context.LoginMemberContext;
import com.criel.train.common.resp.CommonResp;
import com.criel.train.common.resp.PageResp;
import com.criel.train.business.req.ConfirmOrderQueryReq;
import com.criel.train.business.req.ConfirmOrderSaveReq;
import com.criel.train.business.resp.ConfirmOrderQueryResp;
import com.criel.train.business.service.ConfirmOrderService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/confirm-order")
public class ConfirmOrderAdminController {

    @Resource
    private ConfirmOrderService confirmOrderService;

    @PostMapping("/save")
    public CommonResp<Object> save(@Valid @RequestBody ConfirmOrderSaveReq req) {
        confirmOrderService.save(req);
        return CommonResp.success();
    }

    @GetMapping("/query-list")
    public CommonResp<PageResp<ConfirmOrderQueryResp>> queryList(@Valid ConfirmOrderQueryReq req) {
        PageResp<ConfirmOrderQueryResp> list = confirmOrderService.queryList(req);
        return CommonResp.success(list);
    }

    @DeleteMapping("/delete/{id}")
    public CommonResp<Object> delete(@PathVariable Long id) {
        confirmOrderService.delete(id);
        return CommonResp.success();
    }

}

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/confirm-order")
public class ConfirmOrderController {

    @Autowired
    private BeforeConfirmOrderService beforeConfirmOrderService;

    @Autowired
    private ConfirmOrderService confirmOrderService;

    @PostMapping("/confirm")
    public CommonResp<Object> confirm(@Valid @RequestBody ConfirmOrderSaveReq req) {
        long confirmOrderId = beforeConfirmOrderService.beforeConfirm(req);
        // 转为String防止前端js中精度丢失
        return CommonResp.success(String.valueOf(confirmOrderId));
    }

    /**
     * 前端查询订单
     * 如果为成功/失败等最终状态，则返回状态信息，用负数表示；如果是中间状态，则返回前面排队的人数
     *
     * @param id
     */
    @GetMapping("/query-line-up-count/{id}")
    public CommonResp<Integer> queryLineUpCount(@PathVariable long id) {
        Integer count = confirmOrderService.queryLineUpCount(id);
        return CommonResp.success(count);
    }

}

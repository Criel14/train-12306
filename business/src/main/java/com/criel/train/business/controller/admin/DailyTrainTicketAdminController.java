package com.criel.train.business.controller.admin;

import com.criel.train.common.context.LoginMemberContext;
import com.criel.train.common.resp.CommonResp;
import com.criel.train.common.resp.PageResp;
import com.criel.train.business.req.DailyTrainTicketQueryReq;
import com.criel.train.business.req.DailyTrainTicketSaveReq;
import com.criel.train.business.resp.DailyTrainTicketQueryResp;
import com.criel.train.business.service.DailyTrainTicketService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/daily-train-ticket")
public class DailyTrainTicketAdminController {

    @Resource
    private DailyTrainTicketService dailyTrainTicketService;

    @GetMapping("/query-list")
    public CommonResp<PageResp<DailyTrainTicketQueryResp>> queryList(@Valid DailyTrainTicketQueryReq req) {
        PageResp<DailyTrainTicketQueryResp> list = dailyTrainTicketService.queryListAdmin(req);
        return CommonResp.success(list);
    }
}

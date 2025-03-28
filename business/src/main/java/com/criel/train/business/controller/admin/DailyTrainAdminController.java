package com.criel.train.business.controller.admin;

import com.criel.train.common.resp.CommonResp;
import com.criel.train.common.resp.PageResp;
import com.criel.train.business.req.DailyTrainQueryReq;
import com.criel.train.business.req.DailyTrainSaveReq;
import com.criel.train.business.resp.DailyTrainQueryResp;
import com.criel.train.business.service.DailyTrainService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController
@RequestMapping("/admin/daily-train")
public class DailyTrainAdminController {

    @Resource
    private DailyTrainService dailyTrainService;

    @PostMapping("/save")
    public CommonResp save(@Valid @RequestBody DailyTrainSaveReq req) {
        dailyTrainService.save(req);
        return CommonResp.success();
    }

    @GetMapping("/query-list")
    public CommonResp<PageResp<DailyTrainQueryResp>> queryList(@Valid DailyTrainQueryReq req) {
        PageResp<DailyTrainQueryResp> list = dailyTrainService.queryList(req);
        return CommonResp.success(list);
    }

    @DeleteMapping("/delete/{id}")
    public CommonResp delete(@PathVariable Long id) {
        dailyTrainService.delete(id);
        return CommonResp.success();
    }

    /**
     * 生成每日所有车次数据
     * @param date 生成的日期
     * @return
     */
    @GetMapping("/gen-daily/{date}")
    public CommonResp genDaily(@PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") Date date) {
        dailyTrainService.genDaily(date);
        return CommonResp.success();
    }

}

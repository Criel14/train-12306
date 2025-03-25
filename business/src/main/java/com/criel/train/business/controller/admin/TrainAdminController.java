package com.criel.train.business.controller.admin;

import com.criel.train.business.service.TrainSeatService;
import com.criel.train.common.context.LoginMemberContext;
import com.criel.train.common.resp.CommonResp;
import com.criel.train.common.resp.PageResp;
import com.criel.train.business.req.TrainQueryReq;
import com.criel.train.business.req.TrainSaveReq;
import com.criel.train.business.resp.TrainQueryResp;
import com.criel.train.business.service.TrainService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/train")
public class TrainAdminController {

    @Autowired
    private TrainService trainService;

    @Autowired
    private TrainSeatService trainSeatService;

    @PostMapping("/save")
    public CommonResp save(@Valid @RequestBody TrainSaveReq req) {
        trainService.save(req);
        return CommonResp.success();
    }

    @GetMapping("/query-list")
    public CommonResp<PageResp<TrainQueryResp>> queryList(@Valid TrainQueryReq req) {
        PageResp<TrainQueryResp> list = trainService.queryList(req);
        return CommonResp.success(list);
    }

    @GetMapping("/query-all")
    public CommonResp<List<TrainQueryResp>> queryAll() {
        List<TrainQueryResp> list = trainService.queryAll();
        return CommonResp.success(list);
    }

    @DeleteMapping("/delete/{id}")
    public CommonResp delete(@PathVariable Long id) {
        trainService.delete(id);
        return CommonResp.success();
    }

    /**
     * 根据车次编号，生成所有座位信息
     * @param trainCode
     * @return
     */
    @GetMapping("/gen-seat/{trainCode}")
    public CommonResp genSeat(@PathVariable String trainCode) {
        trainSeatService.genTrainSeat(trainCode);
        return CommonResp.success();
    }

}

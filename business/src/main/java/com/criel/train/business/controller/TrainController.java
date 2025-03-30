package com.criel.train.business.controller;

import com.criel.train.business.req.TrainQueryReq;
import com.criel.train.business.req.TrainSaveReq;
import com.criel.train.business.resp.TrainQueryResp;
import com.criel.train.business.service.TrainSeatService;
import com.criel.train.business.service.TrainService;
import com.criel.train.common.resp.CommonResp;
import com.criel.train.common.resp.PageResp;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/train")
public class TrainController {

    @Autowired
    private TrainService trainService;

    @GetMapping("/query-all")
    public CommonResp<List<TrainQueryResp>> queryAll() {
        List<TrainQueryResp> list = trainService.queryAll();
        return CommonResp.success(list);
    }
}

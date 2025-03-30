package com.criel.train.business.controller;

import com.criel.train.business.req.StationQueryReq;
import com.criel.train.business.req.StationSaveReq;
import com.criel.train.business.resp.StationQueryResp;
import com.criel.train.business.service.StationService;
import com.criel.train.common.resp.CommonResp;
import com.criel.train.common.resp.PageResp;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/station")
public class StationController {

    @Resource
    private StationService stationService;

    @GetMapping("/query-all")
    public CommonResp<List<StationQueryResp>> queryAll() {
        List<StationQueryResp> list = stationService.queryAll();
        return CommonResp.success(list);
    }

}

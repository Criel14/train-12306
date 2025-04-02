package com.criel.train.business.controller;

import com.criel.train.business.service.TrainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @Autowired
    private TrainService trainService;

    @GetMapping("/test-connect")
    public String testConnect() {
        return "business test success";
    }

    @GetMapping("/test-query")
    public String testQuery() {
        trainService.queryAll();
        return "business testQuery success";
    }
}

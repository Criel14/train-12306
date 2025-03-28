package com.criel.train.batch.controller;

import com.criel.train.batch.config.BatchApplication;
import com.criel.train.batch.feign.BusinessFeign;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    private static final Logger LOG = LoggerFactory.getLogger(BatchApplication.class);

    @Autowired
    private BusinessFeign businessFeign;

    @GetMapping("/test-connect")
    public String testConnect() {
        String feignRes = businessFeign.testConnect();
        LOG.info("feignRes: {}", feignRes);
        return "batch test success ";
    }
}

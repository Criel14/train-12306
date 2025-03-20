package com.criel.train.business.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/business")
public class TestController {

    @GetMapping("/test-connect")
    public String test() {
        return "Hello Business!";
    }
}

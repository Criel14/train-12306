package com.criel.train.batch.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

//@FeignClient("business")
@FeignClient(name = "business", url = "http://localhost:8002/business")
public interface BusinessFeign {
    @GetMapping("/test-connect")
    String testConnect();

}

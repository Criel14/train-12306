package com.criel.train.batch.feign;

import com.criel.train.common.resp.CommonResp;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Date;

// @FeignClient("business") // 等完成了注册中心再用这个
@FeignClient(name = "business", url = "http://localhost:8002/business")
public interface BusinessFeign {
    @GetMapping("/test-connect")
    String testConnect();

    @GetMapping("/admin/daily-train/gen-daily/{date}")
    CommonResp genDaily(@PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") Date date);

}

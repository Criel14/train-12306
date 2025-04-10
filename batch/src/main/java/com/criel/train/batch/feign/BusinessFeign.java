package com.criel.train.batch.feign;

import com.criel.train.common.resp.CommonResp;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Date;

@FeignClient("business")
//@FeignClient(name = "business", url = "http://localhost:8002/business")
public interface BusinessFeign {
    @GetMapping("/business/admin/daily-train/gen-daily/{date}")
    CommonResp genDaily(@PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") Date date);

}

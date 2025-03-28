package com.criel.train.batch.job;

import cn.hutool.core.util.RandomUtil;
import com.criel.train.batch.feign.BusinessFeign;
import com.criel.train.common.resp.CommonResp;
import jakarta.annotation.Resource;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Date;

/**
 * 每日车次生成定时任务
 */
@DisallowConcurrentExecution // 禁用并发执行任务
public class DailyTrainJob implements Job {
    private static final Logger LOG = LoggerFactory.getLogger(DailyTrainJob.class);

    @Resource
    private BusinessFeign businessFeign;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        // 增加线程的日志流水号
        MDC.put("LOG_ID", System.currentTimeMillis() + RandomUtil.randomString(3));
        LOG.info("生成后15日车次信息开始...");

        // 获取15天后的日期
        int offset15DaysMillis = 15 * 24 * 60 * 60 * 1000;
        Date offsetDate = new Date(System.currentTimeMillis() + offset15DaysMillis);
        businessFeign.genDaily(offsetDate);

        LOG.info("生成后15日车次信息结束...");
    }
}
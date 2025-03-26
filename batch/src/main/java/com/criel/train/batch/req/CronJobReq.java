package com.criel.train.batch.req;

import lombok.Data;

@Data
public class CronJobReq {
    // 任务分组
    private String group;

    // 任务名称
    private String name;

    // 任务描述
    private String description;

    // cron表达式
    private String cronExpression;
}

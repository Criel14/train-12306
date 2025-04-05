package com.criel.train.business.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
@ComponentScan("com.criel")
@MapperScan("com.criel.train.business.mapper")
@EnableFeignClients("com.criel.train.business.feign")
public class BusinessApplication {

    private static final Logger LOG = LoggerFactory.getLogger(BusinessApplication.class);

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(BusinessApplication.class);
        Environment env = app.run(args).getEnvironment();
        LOG.info("business启动成功...");
        LOG.info("business地址:http://127.0.0.1:{}", env.getProperty("server.port"));
        initFlowRules();
    }

    /**
     * Sentinel用，初始化限流规则
     */
    private static void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();
        FlowRule rule = new FlowRule();
        rule.setResource("confirm"); // 名称要和注解里的对应上
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS); // 指定级别
        rule.setCount(20); // 限制QPS为每秒20
        rules.add(rule);
        FlowRuleManager.loadRules(rules);
    }
}

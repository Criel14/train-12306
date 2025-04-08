package com.criel.train.business.service;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.criel.train.business.enumeration.RedisKeyPreEnum;
import com.criel.train.business.req.ConfirmOrderSaveReq;
import com.criel.train.common.constant.RocketMQTopicConstant;
import com.criel.train.common.context.LoginMemberContext;
import com.criel.train.common.exception.BusinessException;
import com.criel.train.common.exception.BusinessExceptionEnum;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class BeforeConfirmOrderService {

    private static final Logger LOG = LoggerFactory.getLogger(BeforeConfirmOrderService.class);

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private SkTokenService skTokenService;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    /**
     * 处理订单前置：校验令牌，获取锁后发送消息队列，排队处理后续订单
     *
     * @param req
     */
    @SentinelResource(value = "beforeConfirm", blockHandler = "beforeConfirmBlockHandler")
    public void beforeConfirm(ConfirmOrderSaveReq req) {
        // 校验令牌
        boolean validSkToken = skTokenService.validSkToken(req.getDate(), req.getTrainCode(), LoginMemberContext.getId());
        if (!validSkToken) {
            throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_SK_TOKEN_FAIL);
        }

        // 这里需要传入memberId，因为在MQ的消费者那里不是同一个线程，拿不到ThreadLocal里的数据
        req.setMemberId(LoginMemberContext.getId());

        // 分布式锁
        String lockKey = RedisKeyPreEnum.CONFIRM_ORDER.getCode() + req.getTrainCode() + ":" + req.getDate();
        RLock rLock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            // 自带看门狗机制，参数是（最大等待时间，单位）
            locked = rLock.tryLock(10, TimeUnit.SECONDS);
            if (!locked) {
                throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_LOCK_FAIL);
            }

            // 抢到锁则发送MQ，发送购票的请求参数
            rocketMQTemplate.convertAndSend(RocketMQTopicConstant.CONFIRM_ORDER_TOPIC, JSON.toJSONString(req));
            LOG.info("排队购票，发送MQ");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // 确保锁被当前线程持有时才释放
            if (locked && rLock.isHeldByCurrentThread()) {
                rLock.unlock();
            }
        }
    }

    /**
     * 降级方法
     *
     * @param req 原方法参数
     * @param e
     */
    private void beforeConfirmBlockHandler(ConfirmOrderSaveReq req, BlockException e) {
        LOG.info("请求被限流:{}", req);
        throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_FLOW_EXCEPTION);
    }
}
